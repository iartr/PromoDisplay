package ru.offerfactory.promodisplay.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import ru.offerfactory.promodisplay.network.domain.util.NetworkConfig
import java.security.KeyStore
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXBuilderParameters
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(null as KeyStore?) // системные CA

        // Находим X509TrustManager из фабрики
        val x509TrustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()

        // Настраиваем PKIX с отключённой проверкой отзыва
        val pkixParams = PKIXBuilderParameters(
            KeyStore.getInstance(KeyStore.getDefaultType()).also { ks ->
                ks.load(null)
                // Добавляем системные доверенные CA
                trustManagerFactory.trustManagers
                    .filterIsInstance<X509TrustManager>()
                    .first()
                    .acceptedIssuers
                    .forEachIndexed { i, cert -> ks.setCertificateEntry("ca_$i", cert) }
            },
            X509CertSelector()
        ).apply {
            isRevocationEnabled = false // <-- отключаем OCSP/CRL
        }

        val certPathValidator = CertPathValidator.getInstance("PKIX")

        val customTrustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> =
                x509TrustManager.acceptedIssuers

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                x509TrustManager.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                val certPath = CertificateFactory.getInstance("X.509")
                    .generateCertPath(chain.toList())
                certPathValidator.validate(certPath, pkixParams)
            }
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(customTrustManager), null)
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .sslSocketFactory(sslContext.socketFactory, customTrustManager)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(client)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
}