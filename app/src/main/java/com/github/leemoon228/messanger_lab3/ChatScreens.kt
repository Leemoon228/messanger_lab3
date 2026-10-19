package com.github.leemoon228.messanger_lab3

import android.graphics.Bitmap
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow

@Composable
fun ChatScreen(viewModel: ChatAPIVM, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            Toast.makeText(context, resources.getString(message), Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = state.selectedImage != null || state.selectedChat != null) {
        if (state.selectedImage != null) viewModel.closeImage() else viewModel.closeChat()
    }

    when {
        state.checkingSession && state.token == null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        state.token == null -> LoginScreen(state.loading, viewModel::login, modifier)
        state.selectedImage != null -> ImageScreen(
            path = state.selectedImage,
            viewModel = viewModel,
            bitmap = state.imageCache["img/${state.selectedImage}"],
            modifier = modifier
        )
        landscape -> Row(modifier.fillMaxSize()) {
            ChatList(state, viewModel::selectChat, viewModel::logout, Modifier.weight(0.38f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            if (state.selectedChat == null) {
                Box(Modifier.weight(0.62f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.select_chat), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                MessagePane(state, viewModel, showChatsButton = false, modifier = Modifier.weight(0.62f))
            }
        }
        state.selectedChat != null -> MessagePane(state, viewModel, showChatsButton = true, modifier = modifier)
        else -> ChatList(state, viewModel::selectChat, viewModel::logout, modifier)
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onLogin(username.trim(), password) }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onLogin(username.trim(), password) },
            enabled = username.isNotBlank() && password.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator() else Text(stringResource(R.string.sign_in))
        }
    }
}

@Composable
private fun ChatList(
    state: ChatUiState,
    onSelectChat: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.chats), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null
                )
            }
        }
        HorizontalDivider()
        if (state.channels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_chats))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.channels, key = { it }) { channel ->
                    val selected = channel == state.selectedChat
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onSelectChat(channel) }
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MessagePane(
    state: ChatUiState,
    viewModel: ChatAPIVM,
    showChatsButton: Boolean,
    modifier: Modifier = Modifier
) {
    val channel = state.selectedChat ?: return
    var draft by remember(channel) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val latestState by rememberUpdatedState(state)
    var initialPositionSet by remember(channel) { mutableStateOf(false) }
    var previousLastMessageId by remember(channel) { mutableStateOf<String?>(null) }

    LaunchedEffect(channel, state.messages.lastOrNull()?.idValue) {
        val lastMessage = state.messages.lastOrNull() ?: return@LaunchedEffect
        if (!initialPositionSet) {
            val savedIndex = state.scrollAnchorMessageId?.let { id ->
                state.messages.indexOfFirst { it.idValue == id }
            } ?: -1
            if (savedIndex >= 0) {
                listState.scrollToItem(savedIndex, state.scrollAnchorOffset)
            } else {
                listState.scrollToItem(state.messages.lastIndex)
            }
            initialPositionSet = true
        } else if (
            lastMessage.idValue != previousLastMessageId &&
            lastMessage.from == state.username
        ) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
        previousLastMessageId = lastMessage.idValue
    }

    LaunchedEffect(channel) {
        snapshotFlow {
            listState.isScrollInProgress &&
                listState.firstVisibleItemIndex == 0 &&
                latestState.canLoadMore &&
                !latestState.loadingMore &&
                latestState.messages.isNotEmpty()
        }.distinctUntilChanged().collect { atTop ->
            if (atTop) {
                viewModel.loadOlderMessages()
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::closeChat) {
                if (showChatsButton) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null
                    )
                }
            }
            Text(channel, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
        }
        HorizontalDivider()
        if (state.loading && state.messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                ) {
                    items(state.messages, key = { it.idValue }) { message ->
                        val bitmap = message.imagePath?.let { state.imageCache["thumb/$it"] }
                        MessageRow(message, bitmap) { path ->
                            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key?.toString()?.let { messageId ->
                                viewModel.saveScrollPosition(messageId, listState.firstVisibleItemScrollOffset)
                            }
                            viewModel.showImage(path)
                        }
                    }
                }
                if (state.loadingMore) {
                    Row(
                        modifier = Modifier.align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.loading_older_messages),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(stringResource(R.string.message_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    viewModel.sendMessage(draft)
                    draft = ""
                }),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.sendMessage(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(stringResource(R.string.send))
            }
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage, bitmap: Bitmap?, onImageClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(message.from, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        message.text?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        message.imagePath?.let { path ->
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp)
                    .clickable(enabled = bitmap != null) { onImageClick(path) },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap == null) {
                    CircularProgressIndicator()
                } else {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageScreen(path: String?, viewModel: ChatAPIVM, bitmap: Bitmap?, modifier: Modifier = Modifier) {
    if (path == null) return
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        IconButton(onClick = viewModel::closeImage, modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null
            )
        }
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().clickable { viewModel.closeImage() },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap == null) CircularProgressIndicator()
            else {
                val aspectRatio = bitmap.width.toFloat() / bitmap.height
                val imageWidth = if (maxWidth / maxHeight > aspectRatio) {
                    maxHeight * aspectRatio
                } else {
                    maxWidth
                }
                val imageHeight = imageWidth / aspectRatio
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(imageWidth, imageHeight).clickable { }
                )
            }
        }
    }
}