
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ZoraGroup(
    val id: String,
    val name: String,
    val ownerId: String
)

@Composable
fun GroupScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()

    val user = auth.currentUser

    var groups by remember {
        mutableStateOf<List<ZoraGroup>>(emptyList())
    }

    var groupName by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var creating by remember {
        mutableStateOf(false)
    }

    // Hanya mengambil grup yang diikuti pengguna.
    DisposableEffect(user?.uid) {
        var listener: ListenerRegistration? = null

        if (user != null) {
            loading = true

            listener = db.collection("groups")
                .whereArrayContains("members", user.uid)
                .addSnapshotListener { snapshot, error ->

                    loading = false

                    if (error != null) {
                        message = error.message
                            ?: "Gagal memuat grup."
                        return@addSnapshotListener
                    }

                    groups = snapshot?.documents.orEmpty().map {
                        ZoraGroup(
                            id = it.id,
                            name = it.getString("name")
                                ?: "Grup ZORA",
                            ownerId = it.getString("ownerId")
                                ?: ""
                        )
                    }
                }
        } else {
            loading = false
            groups = emptyList()
            message = "Silakan login terlebih dahulu."
        }

        onDispose {
            listener?.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Grup",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Buat ruang percakapan bersama.",
            color = ZoraMuted
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = groupName,
            onValueChange = {
                groupName = it
                message = ""
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama grup") },
            singleLine = true,
            enabled = !creating
        )

        Spacer(Modifier.height(10.dp))

        Button(
            enabled = groupName.isNotBlank() &&
                !creating &&
                user != null,
            onClick = {
                val currentUser = auth.currentUser
                    ?: return@Button

                val name = groupName.trim()

                if (name.isBlank()) {
                    message = "Nama grup wajib diisi."
                    return@Button
                }

                if (name.length > 60) {
                    message = "Nama grup maksimal 60 karakter."
                    return@Button
                }

                scope.launch {
                    creating = true
                    message = ""

                    try {
                        db.collection("groups").add(
                            mapOf(
                                "name" to name,
                                "ownerId" to currentUser.uid,
                                "members" to listOf(currentUser.uid),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).await()

                        groupName = ""
                        message = "Grup berhasil dibuat."

                    } catch (e: Exception) {
                        message = e.localizedMessage
                            ?: "Gagal membuat grup."
                    } finally {
                        creating = false
                    }
                }
            },
            modifier = Modifier.height(48.dp)
        ) {
            if (creating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Buat grup")
            }
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))

            Text(
                message,
                color = if (message.contains("berhasil")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Daftar grup",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        when {
            loading -> {
                CircularProgressIndicator()
            }

            groups.isEmpty() -> {
                Text(
                    "Belum ada grup. Buat grup pertamamu.",
                    color = ZoraMuted
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = groups,
                        key = { it.id }
                    ) { group ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    group.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    "ID: ${group.id}",
                                    color = ZoraMuted
                                )

                                Text(
                                    if (group.ownerId == user?.uid) {
                                        "Pemilik grup"
                                    } else {
                                        "Anggota grup"
                                    },
                                    color = ZoraMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
