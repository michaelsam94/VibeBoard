package com.michael.vibeboard.domain.usecase

import com.michael.vibeboard.domain.model.MediaSuggestion
import com.michael.vibeboard.domain.repository.MemeRepository

/**
 * Single entry point for "what should we suggest for this draft message?".
 * Later you can swap [MemeRepository] for TFLite/MediaPipe + Room without changing the ViewModel.
 */
class GetMediaSuggestionsUseCase(
    private val repository: MemeRepository,
) {
    suspend operator fun invoke(userText: String): List<MediaSuggestion> =
        repository.suggestionsForContext(userText)
}
