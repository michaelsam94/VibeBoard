package com.michael.vibeboard.domain.model

import androidx.annotation.DrawableRes

/**
 * Something the keyboard could insert: a GIF, static meme, or sticker.
 * [remoteMediaUrl] is loaded when online; [previewDrawableRes] is used as Coil placeholder/error
 * and offline fallback.
 */
data class MediaSuggestion(
    val id: String,
    val title: String,
    val tags: List<String>,
    @param:DrawableRes val previewDrawableRes: Int,
    val remoteMediaUrl: String?,
    /** When true, show Giphy attribution in UI (required for production API use). */
    val requireGiphyAttribution: Boolean = false,
)
