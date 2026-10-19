package com.github.leemoon228.messanger_lab3

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.kodein.di.instance
import retrofit2.HttpException
import kotlin.time.Duration.Companion.milliseconds

data class ChatUiState(
    val username: String? = null,
    val token: String? = null,
    val checkingSession: Boolean = true,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val channels: List<String> = emptyList(),
    val selectedChat: String? = null,
    val scrollAnchorMessageId: String? = null,
    val scrollAnchorOffset: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val canLoadMore: Boolean = false,
    val selectedImage: String? = null,
    val imageCache: Map<String, Bitmap> = emptyMap()
)

class ChatAPIVM(application: Application) : AndroidViewModel(application) {
    private val chatApi: ChatApi by di.instance()

    private val preferences = application.getSharedPreferences("chat_session", 0)
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<Int>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private val loadingImages = MutableStateFlow<Set<String>>(emptySet())

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val username = preferences.getString("username", null)
        val token = preferences.getString("token", null)
        if (username == null || token == null) {
            _state.value = ChatUiState(checkingSession = false)
            return
        }

        _state.value = ChatUiState(username = username, token = token)
        viewModelScope.launch {
            try {
                chatApi.inbox(username, token, limit = 1)
                loadChannels()
            } catch (error: HttpException) {
                if (error.code() == 401 || error.code() == 403) {
                    clearSession()
                    _events.send(R.string.token_expired)
                } else {
                    _events.send(R.string.network_error)
                }
            } catch (_: Exception) {
                _events.send(R.string.network_error)
            } finally {
                _state.value = _state.value.copy(checkingSession = false)
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val response = chatApi.login(LoginRequest(username, password))
                if (!response.isSuccessful) {
                    response.errorBody()?.close()
                    throw HttpException(response)
                }
                val token = response.body()?.use { it.string() }?.trim()?.trim('"').orEmpty()
                if (token.isBlank()) {
                    throw IllegalStateException("Missing auth token")
                }
                preferences.edit {
                    putString("username", username)
                    putString("token", token)
                }
                _state.value = _state.value.copy(
                    username = username,
                    token = token,
                    loading = false
                )
                loadChannels()
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
                _events.send(R.string.login_failed)
            }
        }
    }

    fun logout() {
        val token = _state.value.token
        viewModelScope.launch {
            try {
                if (token != null) {
                    val response = chatApi.logout(token)
                    if (response.isSuccessful) response.body()?.close()
                    else response.errorBody()?.close()
                }
            } catch (_: Exception) { }
            finally {
                preferences.edit {
                    remove("username")
                    remove("token")
                }
                _state.value = ChatUiState(checkingSession = false)
            }
        }
    }

    private suspend fun loadChannels() {
        try {
            val channels = chatApi.channels().sortedBy { it.lowercase() }
            _state.value = _state.value.copy(channels = channels)
        } catch (_: Exception) {
            _events.send(R.string.network_error)
        }
    }

    fun selectChat(channel: String) {
        val current = _state.value
        if (current.selectedChat == channel && current.messages.isNotEmpty()) {
            current.messages.mapNotNull(ChatMessage::imagePath)
                .filter { current.imageCache[imageKey(it, fullResolution = false)] == null }
                .forEach { loadImage(it) }
            return
        }
        _state.value = _state.value.copy(
            selectedChat = channel,
            scrollAnchorMessageId = null,
            scrollAnchorOffset = 0,
            selectedImage = null,
            messages = emptyList(),
            canLoadMore = false,
            loading = true
        )
        viewModelScope.launch {
            try {
                val newestFirst = chatApi.channelMessages(
                    channel = channel,
                    limit = 20,
                    lastKnownId = Long.MAX_VALUE.toString(),
                    reverse = true
                )
                val messages = newestFirst.asReversed()
                val imagePaths = messages.mapNotNull(ChatMessage::imagePath)
                _state.value = _state.value.copy(
                    messages = messages,
                    canLoadMore = messages.size == 20,
                    loading = false
                )
                imagePaths.forEach { loadImage(it) }
            } catch (_: Exception) {
                _state.value = _state.value.copy(loading = false)
                _events.send(R.string.network_error)
            }
        }
    }

    fun loadOlderMessages() {
        val current = _state.value
        val channel = current.selectedChat ?: return
        val oldestId = current.messages.firstOrNull()?.idValue ?: return
        if (!current.canLoadMore || current.loadingMore) return
        _state.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            try {
                delay(400.milliseconds)
                val older = chatApi.channelMessages(
                    channel = channel,
                    limit = 20,
                    lastKnownId = oldestId,
                    reverse = true
                )
                val currentMessages = _state.value.messages
                val knownIds = currentMessages.map(ChatMessage::idValue).toSet()
                val uniqueOlder = older.filterNot { it.idValue in knownIds }
                _state.value = _state.value.copy(
                    messages = uniqueOlder.asReversed() + currentMessages,
                    canLoadMore = older.size == 20,
                    loadingMore = false
                )
                uniqueOlder.mapNotNull(ChatMessage::imagePath).forEach { loadImage(it) }
            } catch (_: Exception) {
                _state.value = _state.value.copy(loadingMore = false)
                _events.send(R.string.network_error)
            }
        }
    }

    fun sendMessage(text: String) {
        val current = _state.value
        val username = current.username ?: return
        val token = current.token ?: return
        val channel = current.selectedChat ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val response = chatApi.sendMessage(
                    token,
                    NewMessageRequest(username, channel, MessageData(text = TextContent(text)))
                )
                if (!response.isSuccessful) {
                    response.errorBody()?.close()
                    throw HttpException(response)
                }
                val id = response.body()?.use { it.string() }?.trim()?.trim('"')
                    ?: throw IllegalStateException("Missing server message id")

                val message = ChatMessage(
                    id = JsonPrimitive(id),
                    from = username,
                    to = channel,
                    data = buildJsonObject {
                        put("Text", buildJsonObject { put("text", JsonPrimitive(text)) })
                    }
                )
                if (_state.value.selectedChat == channel) {
                    _state.value = _state.value.copy(messages = _state.value.messages + message)
                }
            } catch (_: Exception) {
                _events.send(R.string.network_error)
            }
        }
    }

    fun showImage(path: String) {
        _state.value = _state.value.copy(selectedImage = path)
        loadImage(path, fullResolution = true)
    }

    fun closeImage() {
        _state.value = _state.value.copy(selectedImage = null)
    }

    fun saveScrollPosition(messageId: String, offset: Int) {
        val current = _state.value
        if (current.selectedChat != null &&
            current.messages.any { it.idValue == messageId } &&
            (current.scrollAnchorMessageId != messageId || current.scrollAnchorOffset != offset)
        ) {
            _state.value = current.copy(scrollAnchorMessageId = messageId, scrollAnchorOffset = offset)
        }
    }

    fun closeChat() {
        _state.value = _state.value.copy(
            selectedChat = null,
            scrollAnchorMessageId = null,
            scrollAnchorOffset = 0,
            selectedImage = null,
            messages = emptyList()
        )
    }

    fun loadImage(path: String, fullResolution: Boolean = false) {
        val key = imageKey(path, fullResolution)
        if (_state.value.imageCache.containsKey(key)) {
            return
        }
        while (true) {
            val current = loadingImages.value
            if (key in current) return
            if (loadingImages.compareAndSet(current, current + key)) break
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val route = if (fullResolution) "img/$path" else "thumb/$path"
                val response = chatApi.image(route)
                val responseBody = response.body()
                if (!response.isSuccessful) {
                    response.errorBody()?.close()
                    throw HttpException(response)
                }
                val bytes = responseBody?.use { it.bytes() } ?: error("Image response was empty")
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    withContext(Dispatchers.Main.immediate) {
                        _state.update { state ->
                            state.copy(imageCache = state.imageCache + (key to bitmap))
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                loadingImages.update { it - key }
            }
        }
    }

    private fun imageKey(path: String, fullResolution: Boolean): String =
        "${if (fullResolution) "img" else "thumb"}/$path"

    private fun clearSession() {
        preferences.edit {
            remove("username")
            remove("token")
        }
        _state.value = ChatUiState(checkingSession = true)
    }
}