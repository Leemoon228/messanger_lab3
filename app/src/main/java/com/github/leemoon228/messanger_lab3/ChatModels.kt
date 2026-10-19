package com.github.leemoon228.messanger_lab3

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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
    @SerialName("Text") val text: TextContent? = null
)

@Serializable
data class TextContent(val text: String)

@Serializable
data class ChatMessage(
    val id: JsonPrimitive,
    val from: String,
    val to: String? = null,
    val data: JsonObject,
    val time: JsonPrimitive? = null
) {
    val idValue: String get() = id.content
    val text: String? get() = (data["Text"] as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull
    val imagePath: String? get() = (data["Image"] as? JsonObject)?.get("link")?.jsonPrimitive?.contentOrNull
}
