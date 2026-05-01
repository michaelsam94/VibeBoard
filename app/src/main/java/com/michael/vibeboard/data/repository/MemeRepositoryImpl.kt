package com.michael.vibeboard.data.repository

import android.os.SystemClock
import android.util.Log
import com.michael.vibeboard.BuildConfig
import com.michael.vibeboard.R
import com.michael.vibeboard.data.datasource.LocalMemeCatalog
import com.michael.vibeboard.data.remote.giphy.GiphyApiException
import com.michael.vibeboard.data.remote.giphy.GiphyApiService
import com.michael.vibeboard.data.remote.giphy.toPage
import com.michael.vibeboard.domain.ml.MemeSearchQueryExtractor
import com.michael.vibeboard.domain.model.MediaSuggestion
import com.michael.vibeboard.domain.model.MemeSearchPage
import com.michael.vibeboard.domain.repository.MemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MemeRepositoryImpl(
    private val localCatalog: LocalMemeCatalog,
    private val giphyApi: GiphyApiService?,
    apiKey: String,
    private val queryExtractor: MemeSearchQueryExtractor,
) : MemeRepository {

    private val apiKey: String = apiKey.trim()

    private val placeholder = R.drawable.ic_meme_placeholder

    private val useGiphy: Boolean
        get() = giphyApi != null && apiKey.isNotBlank()

    private val cacheLock = Any()

    /** After HTTP 429, skip Giphy until this elapsedRealtime (ms). */
    @Volatile
    private var giphyBackoffUntilElapsed: Long = 0L

    /** Avoid duplicate network calls for the same suggestion query within [SUGGESTIONS_CACHE_TTL_MS]. */
    private var suggestionsCacheKey: String? = null
    private var suggestionsCacheAt: Long = 0L
    private var suggestionsCacheValue: List<MediaSuggestion> = emptyList()

    init {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Giphy enabled=${useGiphy}, keyLength=${apiKey.length}")
        }
    }

    override suspend fun suggestionsForContext(userText: String): List<MediaSuggestion> =
        withContext(Dispatchers.IO) {
            if (!useGiphy) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Giphy disabled (missing API key in BuildConfig — sync after setting GIPHY_API_KEY)")
                }
                return@withContext localCatalog.match(userText)
            }
            val cacheKey = userText.trim()
            val now = SystemClock.elapsedRealtime()
            synchronized(cacheLock) {
                if (suggestionsCacheKey == cacheKey && now - suggestionsCacheAt < SUGGESTIONS_CACHE_TTL_MS) {
                    return@withContext suggestionsCacheValue
                }
            }
            if (isInGiphyBackoff(now)) {
                Log.w(TAG, "suggestionsForContext: in Giphy cooldown after rate limit; using bundled memes")
                return@withContext localCatalog.match(userText)
            }
            try {
                val q = queryExtractor.toGiphySearchQuery(userText)
                val items = if (q.isBlank()) {
                    giphyApi!!.trending(apiKey, limit = 24, offset = 0).toPage(placeholder, 24, 0).items
                } else {
                    giphyApi!!.search(apiKey, q, limit = 24, offset = 0).toPage(placeholder, 24, 0).items
                }
                clearGiphyBackoff()
                synchronized(cacheLock) {
                    suggestionsCacheKey = cacheKey
                    suggestionsCacheAt = SystemClock.elapsedRealtime()
                    suggestionsCacheValue = items
                }
                items
            } catch (e: GiphyApiException) {
                logGiphyFailure("suggestionsForContext", e)
                localCatalog.match(userText)
            } catch (e: HttpException) {
                handleHttpException(e, "suggestionsForContext")
                localCatalog.match(userText)
            } catch (e: Exception) {
                logGiphyFailure("suggestionsForContext", e)
                localCatalog.match(userText)
            }
        }

    override suspend fun trendingPage(offset: Int, limit: Int): MemeSearchPage =
        withContext(Dispatchers.IO) {
            if (!useGiphy) return@withContext localTrendingFallback(offset, limit)
            val now = SystemClock.elapsedRealtime()
            if (isInGiphyBackoff(now)) {
                Log.w(TAG, "trendingPage: in Giphy cooldown; using bundled memes")
                return@withContext localTrendingFallback(offset, limit)
            }
            try {
                val page = giphyApi!!.trending(apiKey, limit = limit, offset = offset)
                    .toPage(placeholder, limit, offset)
                clearGiphyBackoff()
                page
            } catch (e: GiphyApiException) {
                logGiphyFailure("trendingPage", e)
                localTrendingFallback(offset, limit)
            } catch (e: HttpException) {
                handleHttpException(e, "trendingPage")
                localTrendingFallback(offset, limit)
            } catch (e: Exception) {
                logGiphyFailure("trendingPage", e)
                localTrendingFallback(offset, limit)
            }
        }

    override suspend fun searchPage(query: String, offset: Int, limit: Int): MemeSearchPage =
        withContext(Dispatchers.IO) {
            if (!useGiphy) return@withContext localSearchFallback(query, offset, limit)
            val q = query.trim()
            if (q.isEmpty()) {
                return@withContext trendingPage(offset, limit)
            }
            val now = SystemClock.elapsedRealtime()
            if (isInGiphyBackoff(now)) {
                Log.w(TAG, "searchPage: in Giphy cooldown; using bundled memes")
                return@withContext localSearchFallback(query, offset, limit)
            }
            try {
                val page = giphyApi!!.search(apiKey, q, limit = limit, offset = offset)
                    .toPage(placeholder, limit, offset)
                clearGiphyBackoff()
                page
            } catch (e: GiphyApiException) {
                logGiphyFailure("searchPage", e)
                localSearchFallback(query, offset, limit)
            } catch (e: HttpException) {
                handleHttpException(e, "searchPage")
                localSearchFallback(query, offset, limit)
            } catch (e: Exception) {
                logGiphyFailure("searchPage", e)
                localSearchFallback(query, offset, limit)
            }
        }

    override suspend fun galleryPage(userDraftText: String, offset: Int, limit: Int): MemeSearchPage =
        withContext(Dispatchers.IO) {
            val q = queryExtractor.toGiphySearchQuery(userDraftText)
            if (q.isBlank()) trendingPage(offset, limit) else searchPage(q, offset, limit)
        }

    private fun isInGiphyBackoff(now: Long): Boolean =
        giphyBackoffUntilElapsed > 0L && now < giphyBackoffUntilElapsed

    private fun clearGiphyBackoff() {
        giphyBackoffUntilElapsed = 0L
    }

    private fun scheduleBackoffFrom429(e: HttpException) {
        val now = SystemClock.elapsedRealtime()
        val retryAfterSec = e.response()?.headers()?.get("Retry-After")?.toLongOrNull()
        val extraMs = when {
            retryAfterSec != null -> (retryAfterSec * 1000L).coerceIn(MIN_BACKOFF_MS, MAX_BACKOFF_MS)
            else -> DEFAULT_BACKOFF_MS
        }
        giphyBackoffUntilElapsed = now + extraMs
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "429 cooldown: no Giphy calls for ~${extraMs / 1000}s (Retry-After=$retryAfterSec)")
        }
    }

    private fun handleHttpException(e: HttpException, where: String) {
        val code = e.code()
        if (code == 429) {
            scheduleBackoffFrom429(e)
        }
        val msg = when (code) {
            429 -> "HTTP 429 Too Many Requests — Giphy rate limit; using bundled memes until cooldown ends."
            in 500..599 -> "HTTP $code server error; using bundled memes."
            else -> "HTTP $code"
        }
        Log.w(TAG, "Giphy $where: $msg", e)
    }

    private fun logGiphyFailure(where: String, e: Throwable) {
        Log.e(TAG, "Giphy failed in $where", e)
    }

    private fun localTrendingFallback(offset: Int, limit: Int): MemeSearchPage {
        val all = localCatalog.all()
        val end = (offset + limit).coerceAtMost(all.size)
        val slice = if (offset >= all.size) emptyList() else all.subList(offset, end)
        return MemeSearchPage(
            items = slice,
            nextOffset = end,
            totalCount = all.size,
            hasMore = end < all.size,
        )
    }

    private fun localSearchFallback(query: String, offset: Int, limit: Int): MemeSearchPage {
        val matched = localCatalog.match(query)
        val end = (offset + limit).coerceAtMost(matched.size)
        val slice = if (offset >= matched.size) emptyList() else matched.subList(offset, end)
        return MemeSearchPage(
            items = slice,
            nextOffset = end,
            totalCount = matched.size,
            hasMore = end < matched.size,
        )
    }

    companion object {
        private const val TAG = "VibeBoard/Giphy"
        private const val SUGGESTIONS_CACHE_TTL_MS = 45_000L
        private const val DEFAULT_BACKOFF_MS = 90_000L
        private const val MIN_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 300_000L
    }
}
