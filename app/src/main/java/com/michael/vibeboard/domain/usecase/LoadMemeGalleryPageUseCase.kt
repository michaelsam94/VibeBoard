package com.michael.vibeboard.domain.usecase

import com.michael.vibeboard.domain.model.MemeSearchPage
import com.michael.vibeboard.domain.repository.MemeRepository

/** Loads one page for the meme gallery (trending vs search follows [MemeRepository.galleryPage]). */
class LoadMemeGalleryPageUseCase(
    private val repository: MemeRepository,
) {
    suspend operator fun invoke(
        userDraftText: String,
        offset: Int,
        limit: Int = 25,
    ): MemeSearchPage = repository.galleryPage(userDraftText, offset, limit)
}
