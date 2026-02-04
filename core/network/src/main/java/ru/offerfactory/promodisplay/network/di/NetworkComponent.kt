package ru.offerfactory.promodisplay.network.di

import com.squareup.moshi.Moshi
import dagger.Component
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        NetworkModule::class
    ]
)
interface NetworkComponent {
    fun okHttpClient(): OkHttpClient
    fun moshi(): Moshi
    fun retrofit(): Retrofit
}
