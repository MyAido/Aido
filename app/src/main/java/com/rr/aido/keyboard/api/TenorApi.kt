package com.rr.aido.keyboard.api

import retrofit2.http.GET
import retrofit2.http.Query

interface TenorApi {
    @GET("trending")
    suspend fun getTrending(
        @Query("key") apiKey: String = "LIVDSRZULELA",
        @Query("limit") limit: Int = 20,
        @Query("media_filter") mediaFilter: String = "minimal",
        @Query("contentfilter") contentFilter: String = "off"
    ): TenorResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("key") apiKey: String = "LIVDSRZULELA",
        @Query("limit") limit: Int = 20,
        @Query("media_filter") mediaFilter: String = "minimal",
        @Query("contentfilter") contentFilter: String = "off"
    ): TenorResponse
}

data class TenorResponse(
    val results: List<TenorResult>
)

data class TenorResult(
    val id: String,
    val media: List<TenorMedia>
)

data class TenorMedia(
    val tinygif: TenorFormat,
    val gif: TenorFormat
)

data class TenorFormat(
    val url: String,
    val dims: List<Int>,
    val size: Int
)
