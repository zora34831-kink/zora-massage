
package com.zora.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ChatScreen() {
    val repository = remember { ChatRepository() }
    val scope = rememberCoroutineScope()

    var users by remember {
        mutableStateOf<List<ZoraUser>>(emptyList())
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }

    var selectedConversation by remember {
        mutableStateOf<String?>(null)
    }

    var selectedUser by remember {
        mutableStateOf<ZoraUser?>(null)
    }

    LaunchedEffect(reloadKey) {
        loading = true
        error = ""

        try {
            repository.observeUsers().collect { result ->
                users = result
                loading = false
                error = ""
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loading = false
            error = e.localizedMessage ?: "Gagal memuat pengguna."
        }
    }

    if (selectedConversation != null) {
        ConversationScreen(
            conversationId = selectedConversation!!,
            title = selectedUser?.name ?: "Percakapan",
            verified = selectedUser?.verified == true,
            onBack = {
                selectedConversation = null
                selectedUser = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZoraCream)
            .padding(18.dp)
    ) {
        Text(
            text = "Pesan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ZoraInk
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = "Terhubung dengan pengguna ZORA",
            color = ZoraMuted
        )

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PENGGUNA",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ZoraMuted
            )

            IconButton(
                onClick = {
                    users = emptyList()
                    reloadKey++
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Muat ulang"
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            loading && users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZoraInk)
                }
            }

            error.isNotBlank() && users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyChatState(
                        title = "Tidak dapat memuat pengguna",
                        description = error,
                        onRetry = {
                            reloadKey++
                        }
                    )
                }
            }

            users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyChatState(
                        title = "Belum ada pengguna",
                        description = "Pengguna ZORA lainnya akan muncul di sini.",
                        onRetry = {
                            reloadKey++
                        }
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = users,
                        key = { it.uid }
                    ) { person ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        try {
                                            error = ""

                                            val id = repository
                                                .startConversation(person.uid)

                                            selectedUser = person
                                            selectedConversation = id
                                        } catch (e: Exception) {
                                            error = e.localizedMessage
                                                ?: "Gagal membuka percakapan."
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ZoraWhite
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            ZoraYellow,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.name
                                            .firstOrNull()
                                            ?.uppercase() ?: "Z",
                                        fontWeight = FontWeight.Bold,
                                        color = ZoraInk
                                    )
                                }

                                Spacer(Modifier.width(13.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = person.name,
                                            fontWeight = FontWeight.Bold,
                                            color = ZoraInk
                                        )

                                        if (person.verified) {
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "✓",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = person.email,
                                        color = ZoraMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text(
                                    text = "›",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = ZoraMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        if (error.isNotBlank() && users.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun EmptyChatState(
    title: String,
    description: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ZoraInk
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = description,
            color = ZoraMuted
        )

        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = onRetry) {
            Text("Coba lagi")
        }
    }
}

@Composable
private fun ConversationScreen(
    conversationId: String,
    title: String,
    verified: Boolean,
    onBack: () -> Unit
) {
    val repository = remember { ChatRepository() }

    val currentUid = FirebaseAuth.getInstance()
        .currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember {
        mutableStateOf<List<ZoraMessage>>(emptyList())
    }

    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        loading = true
        error = ""

        try {
            repository.observeMessages(conversationId).collect {
                messages = it
                loading = false
                error = ""
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loading = false
            error = e.localizedMessage ?: "Gagal memuat pesan."
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZoraCream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZoraWhite)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Kembali"
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = ZoraInk
                    )

                    if (verified) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Percakapan pribadi",
                    color = ZoraMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        HorizontalDivider()

        if (loading && messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZoraInk)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->

                    val mine = message.senderId == currentUid

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        Surface(
                            color = if (mine) ZoraYellow else ZoraWhite,
                            shape = RoundedCornerShape(
                                topStart = 17.dp,
                                topEnd = 17.dp,
                                bottomStart = if (mine) 17.dp else 4.dp,
                                bottomEnd = if (mine) 4.dp else 17.dp
                            ),
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Text(
                                text = message.text,
                                color = ZoraInk,
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 11.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZoraWhite)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Tulis pesan...")
                },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                enabled = !sending
            )

            Spacer(Modifier.width(7.dp))

            IconButton(
                enabled = input.isNotBlank() && !sending,
                onClick = {
                    val messageText = input.trim()

                    scope.launch {
                        sending = true
                        error = ""

                        try {
                            repository.sendMessage(
                                conversationId,
                                messageText
                            )
                            input = ""
                        } catch (e: Exception) {
                            error = e.localizedMessage
                                ?: "Pesan gagal dikirim."
                        } finally {
                            sending = false
                        }
                    }
                }
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Kirim",
                        tint = ZoraInk
                    )
                }
            }
        }
    }
}
