package com.example.proyfronted.backend.Music.Client

import android.content.Context
import com.example.proyfronted.backend.Auth.SessionManager
import com.example.proyfronted.backend.Movie.TokenInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.jvm.java

object RetrofitClient {
    lateinit var musicApi: MusicApi
    private lateinit var retrofit: Retrofit

    fun init(context: Context) {
        val sessionManager = SessionManager(context)

        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC

        val client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor { sessionManager.fetchAccessToken() }) // <- token actual
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl("http://100.27.253.120:8080/music/")
        //    .baseUrl("http://192.168.1.38:8080/music/")
        //  .baseUrl("http://10.0.2.2:8080/music/") // ← asegúrate de que esta sea la URL correcta
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        musicApi = retrofit.create(MusicApi::class.java)
    }
}
