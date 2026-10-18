package com.github.leemoon228.messanger_lab3

import kotlinx.serialization.Serializable

@Serializable
data class FakeAPIPost (
    val id: Long,
    val userId: Long,
    val title: String,
    val body: String,

)