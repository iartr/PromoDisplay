package ru.offerfactory.promodisplay.network.data.remote

import retrofit2.http.GET

interface ConfigApi {
    @GET("/api-config")
    suspend fun getConfig(): ConfigDto
}