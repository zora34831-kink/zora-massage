
package com.zora.dev

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun ChatScreen() {
    val repository = remember { ChatRepository() }
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<ZoraUser>>(emptyList()) }
    var conversations by remember {
        mutableStateOf<List<ZoraConversation>>(emptyList())
    }
    var selectedConversation by remember {
        mutableStateOf<String?>(null)
    }
    var selectedUser by remember { mutableStateOf<ZoraUser?>(null) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        launch {
            repository.observeUsers().collect {
                users = it
            }
        }

        launch {
            repository.observeConversations().collect {
                conversations = it
            }
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
            .padding(16.dp)
    ) {
        Text(
            "Pesan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Mulai percakapan dengan pengguna ZORA",
            color = ZoraMuted
        )

        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Pengguna",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.uid }) { person ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                try {
                                    val id = repository.startConversation(person.uid)
                                    selectedUser = person
                                    selectedConversation = id
                                } catch (e: Exception) {
                                    error = e.message ?: "Gagal membuka chat."
                                }
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ZoraYellow,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    person.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    person.name,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (person.verified) {
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                person.email,
                                color = ZoraMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
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
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val scope = rememberCoroutineScope()

    var messages by remember {
        mutableStateOf<List<ZoraMessage>>(emptyList())
    }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        repository.observeMessages(conversationId).collect {
            messages = it
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold)
                    if (verified) {
                        Spacer(Modifier.width(5.dp))
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("Percakapan pribadi", color = ZoraMuted)
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val mine = message.senderId == currentUid

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine)
                        Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (mine) ZoraYellow else ZoraWhite,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Text(
                            message.text,
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 10.dp
                            )
                        )
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tulis pesan...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                enabled = input.isNotBlank() && !sending,
                onClick = {
                    val text = input
                    sending = true
                    error = ""

                    scope.launch {
                        try {
                            repository.sendMessage(conversationId, text)
                            input = ""
                        } catch (e: Exception) {
                            error = e.message ?: "Pesan gagal dikirim."
                        } finally {
                            sending = false
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Kirim"
                )
            }
        }
    }
}
