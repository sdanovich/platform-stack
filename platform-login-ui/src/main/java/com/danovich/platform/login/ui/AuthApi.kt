package com.danovich.platform.login.ui

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

// Wire contract for the platform login endpoints (served by backend-login). Login,
// register, and both social flows return a JWT whose subject is the user id; the
// app rides it as a Bearer token. The consuming app builds the Retrofit AuthApi
// (with its own OkHttp/interceptors) and hands it to the login screen via LoginConfig.

@JsonClass(generateAdapter = true)
data class AuthRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequest(val idToken: String)

@JsonClass(generateAdapter = true)
data class GitHubAuthRequest(val code: String)

@JsonClass(generateAdapter = true)
data class RefreshRequest(val refreshToken: String)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val expiresInSeconds: Long,
    val userId: String,
    val email: String,
    val refreshToken: String
)

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body req: AuthRequest): AuthResponse

    @POST("/api/auth/register")
    suspend fun register(@Body req: AuthRequest): AuthResponse

    @POST("/api/auth/google")
    suspend fun google(@Body req: GoogleAuthRequest): AuthResponse

    @POST("/api/auth/github")
    suspend fun github(@Body req: GitHubAuthRequest): AuthResponse

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body req: RefreshRequest): AuthResponse
}
