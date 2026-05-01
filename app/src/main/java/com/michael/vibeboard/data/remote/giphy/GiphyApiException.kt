package com.michael.vibeboard.data.remote.giphy

import java.io.IOException

/** Giphy JSON returns HTTP 200 with [meta][GiphyMetaDto.status] != 200 for auth and other API errors. */
class GiphyApiException(
    val statusCode: Int,
    message: String?,
) : IOException(message ?: "Giphy API error ($statusCode)")
