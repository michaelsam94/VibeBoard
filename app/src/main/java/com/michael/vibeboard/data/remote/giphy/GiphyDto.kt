package com.michael.vibeboard.data.remote.giphy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiphySearchResponseDto(
    val data: List<GiphyGifDto> = emptyList(),
    val pagination: GiphyPaginationDto? = null,
    val meta: GiphyMetaDto? = null,
)

@Serializable
data class GiphyMetaDto(
    val status: Int = 200,
    val msg: String? = null,
)

@Serializable
data class GiphyPaginationDto(
    @SerialName("total_count") val totalCount: Int = 0,
    val count: Int = 0,
    val offset: Int = 0,
)

@Serializable
data class GiphyGifDto(
    val id: String,
    val title: String = "",
    val slug: String = "",
    val images: GiphyImagesDto,
)

@Serializable
data class GiphyImagesDto(
    val downsized: GiphyRenditionDto? = null,
    @SerialName("downsized_medium") val downsizedMedium: GiphyRenditionDto? = null,
    @SerialName("fixed_width") val fixedWidth: GiphyRenditionDto? = null,
    @SerialName("fixed_height") val fixedHeight: GiphyRenditionDto? = null,
    @SerialName("fixed_height_small") val fixedHeightSmall: GiphyRenditionDto? = null,
    @SerialName("preview_gif") val previewGif: GiphyRenditionDto? = null,
    val original: GiphyRenditionDto? = null,
)

@Serializable
data class GiphyRenditionDto(
    val url: String? = null,
)
