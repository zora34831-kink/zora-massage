
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
        FirebaseDatabase.getInstance().reference
    }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("") }
    var statuses by remember { mutableStateOf<List<ZoraStatus>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        database.child("status").get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.children.mapNotNull { item ->
                    val text = item.child("text").getValue(String::class.java)
                        ?: return@mapNotNull null

                    ZoraStatus(
                        uid = item.key ?: "",
                        name = item.child("name").getValue(String::class.java)
                            ?: "Pengguna ZORA",
                        text = text,
                        createdAt = item.child("createdAt")
                            .getValue(Long::class.java) ?: 0L
                    )
                }
                statuses = result.sortedByDescending { it.createdAt }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Status", style = MaterialTheme.typography.headlineMedium)
        Text("Bagikan kabar singkat.", color = ZoraMuted)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = statusText,
            onValueChange = { statusText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Apa yang sedang kamu pikirkan?") },
            maxLines = 4
        )

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = statusText.isNotBlank() && !loading,
            onClick = {
                val user = auth.currentUser ?: return@Button
                loading = true
                scope.launch {
                    try {
                        database.child("status")
                            .child(user.uid)
                            .setValue(
                                mapOf(
                                    "name" to (user.displayName ?: "Pengguna ZORA"),
                                    "text" to statusText.trim(),
                                    "createdAt" to System.currentTimeMillis()
                                )
                            ).await()

                        statusText = ""
                        message = "Status berhasil dibagikan."
                        val snapshot = database.child("status").get().await()
                        statuses = snapshot.children.mapNotNull { item ->
                            val text = item.child("text")
                                .getValue(String::class.java)
                                ?: return@mapNotNull null

                            ZoraStatus(
                                uid = item.key ?: "",
                                name = item.child("name")
                                    .getValue(String::class.java) ?: "Pengguna ZORA",
                                text = text,
                                createdAt = item.child("createdAt")
                                    .getValue(Long::class.java) ?: 0L
                            )
                        }.sortedByDescending { it.createdAt }
                    } catch (e: Exception) {
                        message = e.message ?: "Gagal membagikan status."
                    } finally {
                        loading = false
                    }
                }
            }
        ) {
            Text(if (loading) "Mengirim..." else "Bagikan status")
        }

        if (message.isNotBlank()) {
            Text(message, color = ZoraMuted)
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text("Status terbaru", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(statuses) { status ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            status.name,
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
