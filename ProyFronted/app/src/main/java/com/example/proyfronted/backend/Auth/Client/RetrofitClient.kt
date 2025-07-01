package com.example.proyfronted.backend.Auth.Client
import com.example.proyfronted.backend.Auth.Interface.KeycloakApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val retrofit by lazy {
        Retrofit.Builder()
           .baseUrl("http://100.27.253.120:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: KeycloakApiService by lazy {
        retrofit.create(KeycloakApiService::class.java)
    }
}