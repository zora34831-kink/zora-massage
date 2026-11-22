
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ZoraStatus(
    val uid: String = "",
    val name: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)

@Composable
fun StatusScreen() {
    val database = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("") }
    var statuses by remember {
        mutableStateOf<List<ZoraStatus>>(emptyList())
    }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    suspend fun loadStatuses() {
        val snapshot = database.collection("statuses")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        statuses = snapshot.documents.map { document ->
            ZoraStatus(
                uid = document.getString("uid") ?: "",
                name = document.getString("name") ?: "Pengguna ZORA",
                text = document.getString("text") ?: "",
                createdAt = document.getLong("createdAt") ?: 0L
            )
        }
    }

    LaunchedEffect(Unit) {
        try {
            loadStatuses()
        } catch (e: Exception) {
            message = e.message ?: "Gagal memuat status."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Bagikan kabar singkat.",
            color = ZoraMuted
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = statusText,
            onValueChange = { statusText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Apa yang sedang kamu pikirkan?")
            },
            maxLines = 4
        )

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = statusText.isNotBlank() && !loading,
            onClick = {
                val user = auth.currentUser

                if (user == null) {
                    message = "Silakan login terlebih dahulu."
                    return@Button
                }

                loading = true

                scope.launch {
                    try {
                        val data = hashMapOf(
                            "uid" to user.uid,
                            "name" to (
                                user.displayName
                                    ?: user.email
                                    ?: "Pengguna ZORA"
                            ),
                            "text" to statusText.trim(),
                            "createdAt" to System.currentTimeMillis()
                        )

                        database.collection("statuses")
                            .add(data)
                            .await()

                        statusText = ""
                        message = "Status berhasil dibagikan."

                        loadStatuses()
                    } catch (e: Exception) {
                        message = e.message
                            ?: "Gagal membagikan status."
                    } finally {
                        loading = false
                    }
                }
            }
        ) {
            Text(
                if (loading) "Mengirim..."
                else "Bagikan status"
            )
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = ZoraMuted)
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text(
            text = "Status terbaru",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(statuses) { status ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = status.name,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(status.text)
                    }
                }
            }
        }
    }
}
