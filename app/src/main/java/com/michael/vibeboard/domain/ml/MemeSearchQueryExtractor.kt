package com.michael.vibeboard.domain.ml

/**
 * Turns the user’s draft text into a Giphy search query (or empty for trending).
 * Replace [HeuristicMemeSearchQueryExtractor] with a TFLite / MediaPipe–backed implementation
 * that maps text to intent labels or embedding-based keywords without changing the repository.
 */
fun interface MemeSearchQueryExtractor {

    /**
     * @return Search string for Giphy `q`, trimmed and length-limited. **Empty** means callers
     * should use the trending feed instead of search.
     */
    fun toGiphySearchQuery(userDraftText: String): String
}
