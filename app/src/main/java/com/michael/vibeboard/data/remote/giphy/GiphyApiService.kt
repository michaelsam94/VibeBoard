package com.michael.vibeboard.data.remote.giphy

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * https://developers.giphy.com/docs/api/endpoint#search
 */
interface GiphyApiService {

    @GET("v1/gifs/search")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("rating") rating: String = "pg-13",
        @Query("lang") lang: String = "en",
    ): GiphySearchResponseDto

    @GET("v1/gifs/trending")
    suspend fun trending(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("rating") rating: String = "pg-13",
    ): GiphySearchResponseDto
}
