package com.github.leemoon228.messanger_lab3

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val di = DI {
    bindSingleton {
        Json {
            ignoreUnknownKeys = true
        }
    }

    bindSingleton {
        OkHttpClient()
    }

    bindSingleton {
        Retrofit.Builder()
            .client(instance())
            .addConverterFactory(instance<Json>().asConverterFactory("application/json".toMediaType()))
            .baseUrl("https://faerytea.name/")
            .build()
    }

    bindSingleton<ChatApi> {
        ChatApi(instance())
    }
}