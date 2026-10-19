package com.github.leemoon228.messanger_lab3

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ChatApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    @GET("channels")
    suspend fun channels(): List<String>

    @GET("channel/{channel}")
    suspend fun channelMessages(
        @Path("channel") channel: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String = "0",
        @Query("reverse") reverse: Boolean = false
    ): List<ChatMessage>

    @GET("inbox/{username}")
    suspend fun inbox(
        @Path("username") username: String,
        @Header("X-Auth-Token") token: String,
        @Query("limit") limit: Int = 20
    ): List<ChatMessage>

    @POST("messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: NewMessageRequest
    ): Response<Long>

    @POST("logout")
    suspend fun logout(@Header("X-Auth-Token") token: String): Response<Unit>

    @Streaming
    @GET("{path}")
    suspend fun image(@Path(value = "path", encoded = true) path: String): Response<ResponseBody>
}
