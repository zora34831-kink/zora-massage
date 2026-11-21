
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OwnerScreen() {
    val repository = remember { OwnerRepository() }
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<OwnerUser>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<OwnerUser?>(null) }
    var showSuspendDialog by remember { mutableStateOf(false) }

    suspend fun refresh() {
        loading = true
        try {
            users = repository.getUsers()
            message = ""
        } catch (e: Exception) {
            message = e.message ?: "Gagal mengambil data."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "OWNER CONTROL",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Pengelolaan akun ZORA MASSAGE",
            color = ZoraMuted
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch { refresh() }
            },
            enabled = !loading
        ) {
            Text(if (loading) "Memuat..." else "Refresh akun")
        }

        if (message.isNotBlank()) {
            Text(
                message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users, key = { it.uid }) { account ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            account.name.ifBlank { "Pengguna ZORA" },
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(account.email)

                        Spacer(Modifier.height(6.dp))

                        Text(
                            when {
                                account.suspended -> "Status: Ditangguhkan"
                                account.disabled -> "Status: Nonaktif"
                                else -> "Status: Aktif"
                            },
                            color = if (account.suspended || account.disabled)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )

                        Text(
                            if (account.verified)
                                "Terverifikasi"
                            else
                                "Belum terverifikasi",
                            color = ZoraMuted
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            repository.setVerified(
                                                account.uid,
                                                !account.verified
                                            )
                                            refresh()
                                        } catch (e: Exception) {
                                            message = e.message ?: "Gagal mengubah verifikasi."
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (account.verified)
                                        "Cabut centang"
                                    else
                                        "Centang biru"
                                )
                            }

                            if (account.suspended || account.disabled) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                repository.unsuspendUser(account.uid)
                                                refresh()
                                            } catch (e: Exception) {
                                                message = e.message ?: "Gagal memulihkan akun."
                                            }
                                        }
                                    }
                                ) {
                                    Text("Pulihkan")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        selectedUser = account
                                        showSuspendDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Suspend")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSuspendDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = {
                showSuspendDialog = false
            },
            title = {
                Text("Suspend akun")
            },
            text = {
                Text("Pilih jenis penangguhan akun ini.")
            },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            val target = selectedUser ?: return@TextButton
                            showSuspendDialog = false
                            scope.launch {
                                try {
                                    repository.suspendUser(target.uid, 24)
                                    refresh()
                                } catch (e: Exception) {
                                    message = e.message ?: "Gagal suspend."
                                }
                            }
                        }
                    ) {
                        Text("24 jam")
                    }

                    TextButton(
                        onClick = {
                            val target = selectedUser ?: return@TextButton
                            showSuspendDialog = false
                            scope.launch {
                                try {
                                    repository.suspendUser(target.uid, 168)
                                    refresh()
                                } catch (e: Exception) {
                                    message = e.message ?: "Gagal suspend."
                                }
                            }
                        }
                    ) {
                        Text("7 hari")
                    }

                    TextButton(
                        onClick = {
                            val target = selectedUser ?: return@TextButton
                            showSuspendDialog = false
                            scope.launch {
                                try {
                                    repository.suspendUser(target.uid, null)
                                    refresh()
                                } catch (e: Exception) {
                                    message = e.message ?: "Gagal suspend."
                                }
                            }
                        }
                    ) {
                        Text("Permanen")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuspendDialog = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
