package com.michael.vibeboard.presentation.di

import com.michael.vibeboard.BuildConfig
import com.michael.vibeboard.data.datasource.LocalMemeCatalog
import com.michael.vibeboard.data.ml.HeuristicMemeSearchQueryExtractor
import com.michael.vibeboard.data.remote.giphy.GiphyRetrofit
import com.michael.vibeboard.data.repository.MemeRepositoryImpl
import com.michael.vibeboard.domain.repository.MemeRepository
import com.michael.vibeboard.domain.usecase.GetMediaSuggestionsUseCase
import com.michael.vibeboard.domain.usecase.LoadMemeGalleryPageUseCase

/**
 * Manual composition root. Replace with Hilt/Koin when the graph grows.
 * Set `GIPHY_API_KEY` in `local.properties` (see Giphy developer dashboard).
 */
class AppContainer {

    private val memeCatalog = LocalMemeCatalog()
    private val queryExtractor = HeuristicMemeSearchQueryExtractor()
    private val giphyApi = if (BuildConfig.GIPHY_API_KEY.isNotBlank()) {
        GiphyRetrofit.createService()
    } else {
        null
    }

    private val memeRepository: MemeRepository = MemeRepositoryImpl(
        localCatalog = memeCatalog,
        giphyApi = giphyApi,
        apiKey = BuildConfig.GIPHY_API_KEY,
        queryExtractor = queryExtractor,
    )

    val getMediaSuggestionsUseCase: GetMediaSuggestionsUseCase =
        GetMediaSuggestionsUseCase(memeRepository)

    val loadMemeGalleryPageUseCase: LoadMemeGalleryPageUseCase =
        LoadMemeGalleryPageUseCase(memeRepository)
}
