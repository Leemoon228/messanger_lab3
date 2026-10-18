package com.github.leemoon228.messanger_lab3

import retrofit2.http.GET

interface FakeAPI {
    @GET("/posts")
    suspend fun posts () : List<FakeAPIPost>{

    }
}