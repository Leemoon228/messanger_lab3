package com.github.leemoon228.messanger_lab3

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class LoginRequest(val name: String, val pwd: String)

@Serializable
data class NewMessageRequest(
    val from: String,
    val to: String,
    val data: MessageData
)

@Serializable
data class MessageData(
    @SerialName("Text") val text: TextContent? = null,
    @SerialName("Image") val image: ImageContent? = null
)

@Serializable
data class TextContent(val text: String)

@Serializable
data class ImageContent(val link: String)

@Serializable
data class ChatMessage(
    val id: JsonPrimitive,
    val from: String,
    val to: String? = null,
    val data: MessageData,
    val time: JsonPrimitive? = null
) {
    val idValue: String get() = id.content
    val text: String? get() = data.text?.text
    val imagePath: String? get() = data.image?.link
}

fun NewMessageRequest.asChatMessage(id: Long): ChatMessage =
    ChatMessage(JsonPrimitive(id), from, to, data)
