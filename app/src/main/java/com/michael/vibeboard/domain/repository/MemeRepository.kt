package com.michael.vibeboard.domain.repository

import com.michael.vibeboard.domain.model.MediaSuggestion
import com.michael.vibeboard.domain.model.MemeSearchPage

/** Remote-first (Giphy) with a small bundled catalog fallback when offline or no API key. */
interface MemeRepository {
    /** First page of context-aware suggestions (uses [com.michael.vibeboard.domain.ml.MemeSearchQueryExtractor]). */
    suspend fun suggestionsForContext(userText: String): List<MediaSuggestion>

    suspend fun trendingPage(offset: Int, limit: Int): MemeSearchPage

    suspend fun searchPage(query: String, offset: Int, limit: Int): MemeSearchPage

    /**
     * Gallery / infinite scroll: trending if [userDraftText] yields an empty query from the extractor,
     * otherwise search with the extracted query.
     */
    suspend fun galleryPage(userDraftText: String, offset: Int, limit: Int): MemeSearchPage
}
