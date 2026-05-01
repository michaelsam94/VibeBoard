package com.michael.vibeboard.data.datasource

import com.michael.vibeboard.R
import com.michael.vibeboard.domain.model.MediaSuggestion

/**
 * Stand-in for "largest database": a small keyword → suggestion map you ship in the APK.
 * [remoteMediaUrl] points to images/GIFs (needs network); swap for bundled assets or Room later.
 */
class LocalMemeCatalog {

    private val catalog: List<MediaSuggestion> = listOf(
        MediaSuggestion(
            id = "1",
            title = "Celebration dance",
            tags = listOf("yay", "won", "celebrate", "party", "birthday"),
            previewDrawableRes = R.drawable.ic_meme_placeholder,
            remoteMediaUrl = "https://picsum.photos/seed/vibeboard1/400/300",
        ),
        MediaSuggestion(
            id = "2",
            title = "Facepalm",
            tags = listOf("ugh", "fail", "wrong", "mistake"),
            previewDrawableRes = R.drawable.ic_meme_placeholder,
            remoteMediaUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Chess_Queen_%28white%29_on_b1.gif/240px-Chess_Queen_%28white%29_on_b1.gif",
        ),
        MediaSuggestion(
            id = "3",
            title = "This is fine",
            tags = listOf("fine", "stress", "deadline", "panic"),
            previewDrawableRes = R.drawable.ic_meme_placeholder,
            remoteMediaUrl = "https://picsum.photos/seed/vibeboard3/400/300",
        ),
        MediaSuggestion(
            id = "4",
            title = "Slow clap",
            tags = listOf("sarcasm", "wow", "impressed", "clap"),
            previewDrawableRes = R.drawable.ic_meme_placeholder,
            remoteMediaUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/200px-Rotating_earth_%28large%29.gif",
        ),
        MediaSuggestion(
            id = "5",
            title = "Happy cat",
            tags = listOf("cute", "love", "happy", "cat"),
            previewDrawableRes = R.drawable.ic_meme_placeholder,
            remoteMediaUrl = "https://picsum.photos/seed/vibeboard5/400/300",
        ),
        MediaSuggestion(
            id = "6",
            title = "Sleepy panda",
            tags = listOf("tired", "sleep", "late", "bed"),
            previewDrawableRes = R.drawable.ic_meme_placeholder,
            remoteMediaUrl = "https://picsum.photos/seed/vibeboard6/400/300",
        ),
    )

    fun all(): List<MediaSuggestion> = catalog

    fun match(userText: String): List<MediaSuggestion> {
        val tokens = userText.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length >= 2 }
            .toSet()
        if (tokens.isEmpty()) return catalog.take(6)

        val scored = catalog.map { item ->
            val score = item.tags.count { tag -> tag in tokens || tokens.any { it.contains(tag) } }
            item to score
        }
        val hits = scored.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
        return if (hits.isNotEmpty()) hits else catalog.take(4)
    }
}
