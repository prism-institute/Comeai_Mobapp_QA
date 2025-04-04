package com.example.comeai_new.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://28gg9c7085.execute-api.ca-central-1.amazonaws.com/prod/"



    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Logs full request and response body
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()


    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client) // Attach logging client
            .build()
            .create(ApiService::class.java)
    }
}
