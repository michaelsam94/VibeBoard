package com.michael.vibeboard.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.michael.vibeboard.domain.usecase.GetMediaSuggestionsUseCase
import com.michael.vibeboard.domain.usecase.LoadMemeGalleryPageUseCase

class HomeViewModelFactory(
    private val getMediaSuggestions: GetMediaSuggestionsUseCase,
    private val loadMemeGalleryPage: LoadMemeGalleryPageUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(getMediaSuggestions, loadMemeGalleryPage) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
