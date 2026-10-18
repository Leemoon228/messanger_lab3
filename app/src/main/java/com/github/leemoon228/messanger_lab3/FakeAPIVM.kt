package com.github.leemoon228.messanger_lab3

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.flow
import org.kodein.di.instance

class FakeAPIVM : ViewModel() {
    private val fakeAPI : FakeAPI by di.instance()

    val posts = flow {
        emit(fakeAPI.posts())
    }
}