package com.github.leemoon228.messanger_lab3

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit

class ChatApi(retrofit: Retrofit) {
    private val service = retrofit.create(ChatApiService::class.java)

    suspend fun login(request: LoginRequest): Response<ResponseBody> = service.login(request)

    suspend fun channels(): List<String> = service.channels()

    suspend fun channelMessages(
        channel: String,
        limit: Int = 20,
        lastKnownId: String = "0",
        reverse: Boolean = false
    ): List<ChatMessage> = service.channelMessages(channel, limit, lastKnownId, reverse)

    suspend fun inbox(username: String, token: String, limit: Int = 20): List<ChatMessage> =
        service.inbox(username, token, limit)

    suspend fun sendMessage(token: String, message: NewMessageRequest): Response<ResponseBody> =
        service.sendMessage(token, message)

    suspend fun logout(token: String): Response<ResponseBody> = service.logout(token)

    suspend fun image(path: String): Response<ResponseBody> = service.image(path)
}
