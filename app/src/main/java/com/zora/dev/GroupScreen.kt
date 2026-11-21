
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun GroupScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var groups by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var groupName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("groups").addSnapshotListener { snapshot, error ->
            if (error != null) {
                message = error.message ?: "Gagal memuat grup."
                return@addSnapshotListener
            }

            groups = snapshot?.documents.orEmpty().map {
                it.id to (it.getString("name") ?: "Grup ZORA")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Grup", style = MaterialTheme.typography.headlineMedium)
        Text("Buat ruang percakapan bersama.", color = ZoraMuted)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama grup") },
            singleLine = true
        )

        Button(
            enabled = groupName.isNotBlank(),
            onClick = {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: return@Button

                scope.launch {
                    try {
                        db.collection("groups").add(
                            mapOf(
                                "name" to groupName.trim(),
                                "ownerId" to user.uid,
                                "members" to listOf(user.uid),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).await()

                        groupName = ""
                        message = "Grup berhasil dibuat."
                    } catch (e: Exception) {
                        message = e.message ?: "Gagal membuat grup."
                    }
                }
            }
        ) {
            Text("Buat grup")
        }

        if (message.isNotBlank()) {
            Text(message, color = ZoraMuted)
        }

        Spacer(Modifier.height(16.dp))
        Text("Daftar grup", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(groups.size) { index ->
                val group = groups[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            group.second,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("ID: ${group.first}", color = ZoraMuted)
                    }
                }
            }
        }
    }
}
