package com.michael.vibeboard.presentation.home

import com.michael.vibeboard.domain.model.MediaSuggestion

data class HomeUiState(
    val draftText: String = "",
    val suggestions: List<MediaSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val memeGalleryOpen: Boolean = false,
    val memeGalleryItems: List<MediaSuggestion> = emptyList(),
    val memeGalleryLoading: Boolean = false,
    val memeGalleryNextOffset: Int = 0,
    val memeGalleryHasMore: Boolean = false,
    val memeGalleryLoadingMore: Boolean = false,
)
