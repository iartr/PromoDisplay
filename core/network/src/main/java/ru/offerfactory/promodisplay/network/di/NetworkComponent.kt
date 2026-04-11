package ru.offerfactory.promodisplay.network.di

import dagger.Component
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Singleton
@Component(
    modules = [NetworkModule::class]
)
interface NetworkComponent {

    fun okHttpClient(): OkHttpClient

    fun retrofit(): Retrofit
}