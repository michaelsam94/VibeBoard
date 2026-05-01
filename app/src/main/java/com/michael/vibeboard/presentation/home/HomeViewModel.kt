@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.michael.vibeboard.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.vibeboard.domain.usecase.GetMediaSuggestionsUseCase
import com.michael.vibeboard.domain.usecase.LoadMemeGalleryPageUseCase
import com.michael.vibeboard.presentation.SUGGESTION_DEBOUNCE_MS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getMediaSuggestions: GetMediaSuggestionsUseCase,
    private val loadMemeGalleryPage: LoadMemeGalleryPageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val draftForSuggestions = MutableStateFlow("")

    init {
        viewModelScope.launch {
            draftForSuggestions
                .debounce(SUGGESTION_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { text ->
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    try {
                        val next = getMediaSuggestions(text)
                        _uiState.update {
                            it.copy(suggestions = next, isLoading = false)
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Something went wrong",
                            )
                        }
                    }
                }
        }
        onDraftTextChanged("")
    }

    fun onDraftTextChanged(text: String) {
        _uiState.update { it.copy(draftText = text) }
        draftForSuggestions.value = text
    }

    fun onOpenMemeGallery() {
        _uiState.update { it.copy(memeGalleryLoading = true) }
        viewModelScope.launch {
            try {
                val draft = _uiState.value.draftText
                val page = loadMemeGalleryPage(draft, offset = 0, limit = 25)
                _uiState.update {
                    it.copy(
                        memeGalleryItems = page.items,
                        memeGalleryNextOffset = page.nextOffset,
                        memeGalleryHasMore = page.hasMore,
                        memeGalleryOpen = true,
                        memeGalleryLoading = false,
                        memeGalleryLoadingMore = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        memeGalleryLoading = false,
                        errorMessage = e.message ?: "Could not load meme gallery",
                    )
                }
            }
        }
    }

    fun onLoadMoreMemeGallery() {
        val s = _uiState.value
        if (!s.memeGalleryOpen || !s.memeGalleryHasMore || s.memeGalleryLoadingMore || s.memeGalleryLoading) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(memeGalleryLoadingMore = true) }
            try {
                val page = loadMemeGalleryPage(s.draftText, offset = s.memeGalleryNextOffset, limit = 25)
                val existingIds = s.memeGalleryItems.map { it.id }.toSet()
                val merged = s.memeGalleryItems + page.items.filter { it.id !in existingIds }
                _uiState.update {
                    it.copy(
                        memeGalleryItems = merged,
                        memeGalleryNextOffset = page.nextOffset,
                        memeGalleryHasMore = page.hasMore,
                        memeGalleryLoadingMore = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        memeGalleryLoadingMore = false,
                        errorMessage = e.message ?: "Could not load more memes",
                    )
                }
            }
        }
    }

    fun onCloseMemeGallery() {
        _uiState.update { it.copy(memeGalleryOpen = false) }
    }

    fun onMemePickedFromGallery(title: String) {
        val next = _uiState.value.draftText + "[$title] "
        onDraftTextChanged(next)
    }
}
