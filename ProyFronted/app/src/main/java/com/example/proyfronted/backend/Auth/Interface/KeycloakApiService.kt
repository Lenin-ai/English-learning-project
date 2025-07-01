package com.example.proyfronted.backend.Auth.Interface
import com.example.proyfronted.backend.Auth.Model.LoginRequest
import com.example.proyfronted.backend.Auth.Model.RegisterRequest
import com.example.proyfronted.backend.Auth.Model.TokenResponse
import com.example.proyfronted.backend.Auth.Model.UserDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface KeycloakApiService {
    @POST("/keycloak/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<TokenResponse>
    @POST("keycloak/create")
    suspend fun register(@Body request: RegisterRequest): Response<Void>
    @GET("/keycloak/me")
    suspend fun getUserInfo(@Header("Authorization") token: String): Response<UserDTO>

    @DELETE("/keycloak/delete")
    suspend fun deleteUser(@Header("Authorization") token: String): Response<Unit>

    @PUT("/keycloak/enabled")
    suspend fun setUserEnabled(
        @Header("Authorization") token: String,
        @Body body: Map<String, Boolean>
    ): Response<Unit>

    @PUT("/keycloak/update")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Body body: UserDTO
    ): Response<Unit>

    @POST("/keycloak/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<TokenResponse>


}