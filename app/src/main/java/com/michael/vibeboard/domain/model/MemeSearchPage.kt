package com.michael.vibeboard.domain.model

/**
 * One page of results from a remote meme provider (e.g. Giphy search or trending).
 */
data class MemeSearchPage(
    val items: List<MediaSuggestion>,
    val nextOffset: Int,
    val totalCount: Int?,
    val hasMore: Boolean,
)
