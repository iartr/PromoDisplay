package ru.offerfactory.promodisplay.syncer.impl.data.remote

import retrofit2.http.GET

interface ConfigApi {
    @GET("api-config")
    suspend fun getConfig(): ConfigDto
}