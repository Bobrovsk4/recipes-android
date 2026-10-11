package com.example.recipes.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    var version: Int = 0
        private set

    @Volatile var cachedTypes: List<Type> = emptyList()
    @Volatile var cachedDaytimes: List<DaytimeType> = emptyList()

    var baseUrl: String = "http://2.58.12.122:8080/"
        set(value) {
            val fixed = if (value.endsWith("/")) value else "$value/"
            if (fixed != field) {
                field = fixed
                cached = null
                version++
            }
        }

    private var cached: RecipeApi? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val api: RecipeApi
        get() = cached ?: Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecipeApi::class.java)
            .also { cached = it }

    fun imageUrl(img: String?): String? = when {
        img.isNullOrBlank() -> null
        img.startsWith("http") -> img
        else -> baseUrl + "images/" + img
    }
}