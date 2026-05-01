package com.michael.vibeboard.data.remote.giphy

import com.michael.vibeboard.domain.model.MediaSuggestion
import com.michael.vibeboard.domain.model.MemeSearchPage

internal fun GiphySearchResponseDto.toPage(
    placeholderDrawable: Int,
    limit: Int,
    requestedOffset: Int,
): MemeSearchPage {
    meta?.takeIf { it.status != 200 }?.let {
        throw GiphyApiException(it.status, it.msg)
    }
    val mapped = data.mapNotNull { it.toMediaSuggestion(placeholderDrawable) }
    val p = pagination
    val total = p?.totalCount
    val returned = p?.count ?: mapped.size
    val nextOffset = p?.let { it.offset + it.count } ?: (requestedOffset + returned)
    val hasMore = when {
        total != null && total > 0 -> nextOffset < total
        else -> mapped.size >= limit
    }
    return MemeSearchPage(
        items = mapped,
        nextOffset = nextOffset,
        totalCount = total,
        hasMore = hasMore && mapped.isNotEmpty(),
    )
}

private fun GiphyGifDto.toMediaSuggestion(placeholderDrawable: Int): MediaSuggestion? {
    val url = images.downsized?.url
        ?: images.downsizedMedium?.url
        ?: images.fixedWidth?.url
        ?: images.fixedHeight?.url
        ?: images.fixedHeightSmall?.url
        ?: images.previewGif?.url
        ?: images.original?.url
        ?: return null
    val label = title.ifBlank {
        slug.replace('-', ' ').ifBlank { id }
    }
    return MediaSuggestion(
        id = id,
        title = label,
        tags = emptyList(),
        previewDrawableRes = placeholderDrawable,
        remoteMediaUrl = url,
        requireGiphyAttribution = true,
    )
}
