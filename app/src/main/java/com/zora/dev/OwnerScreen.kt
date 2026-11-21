package com.zora.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val OwnerBg = Color(0xFFFFF9E9)
private val OwnerYellow = Color(0xFFFFD94D)
private val OwnerInk = Color(0xFF171717)
private val OwnerMuted = Color(0xFF77736B)

private data class OwnerUser(
    val uid: String,
    val name: String,
    val email: String,
    val disabled: Boolean,
    val verified: Boolean,
    val suspended: Boolean,
    val suspensionType: String
)

@Composable
fun OwnerScreen(onBack: () -> Unit = {}) {
    val functions = remember { FirebaseFunctions.getInstance() }
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<OwnerUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyUid by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    suspend fun loadUsers() {
        loading = true
        error = ""
        try {
            val result = functions
                .getHttpsCallable("ownerListUsers")
                .call()
                .await()

            val data = result.data as? Map<*, *>
            val list = data?.get("users") as? List<*> ?: emptyList<Any>()

            users = list.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                OwnerUser(
                    uid = map["uid"] as? String ?: return@mapNotNull null,
                    name = map["name"] as? String ?: "",
                    email = map["email"] as? String ?: "",
                    disabled = map["disabled"] as? Boolean ?: false,
                    verified = map["verified"] as? Boolean ?: false,
                    suspended = map["suspended"] as? Boolean ?: false,
                    suspensionType = map["suspensionType"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            error = e.message ?: "Gagal memuat pengguna."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(refreshKey) {
        loadUsers()
    }

    fun runAction(uid: String, action: String, verified: Boolean = false) {
        scope.launch {
            busyUid = uid
            error = ""
            message = ""
            try {
                when (action) {
                    "verify" -> {
                        functions.getHttpsCallable("ownerSetVerified")
                            .call(mapOf("uid" to uid, "verified" to verified))
                            .await()
                        message = if (verified) "Akun berhasil diverifikasi." else "Verifikasi akun dicabut."
                    }

                    "suspend" -> {
                        functions.getHttpsCallable("ownerSuspendUser")
                            .call(mapOf("uid" to uid))
                            .await()
                        message = "Akun berhasil di-suspend."
                    }

                    "temporary" -> {
                        functions.getHttpsCallable("ownerSuspendUser")
                            .call(mapOf("uid" to uid, "durationHours" to 24))
                            .await()
                        message = "Akun di-suspend selama 24 jam."
                    }

                    "unsuspend" -> {
                        functions.getHttpsCallable("ownerUnsuspendUser")
                            .call(mapOf("uid" to uid))
                            .await()
                        message = "Akun berhasil dipulihkan."
                    }
                }
                refreshKey++
            } catch (e: Exception) {
                error = e.message ?: "Aksi gagal dijalankan."
            } finally {
                busyUid = null
            }
        }
    }

    val filteredUsers = users.filter {
        it.name.contains(search, ignoreCase = true) ||
            it.email.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OwnerBg)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("KEMBALI", color = OwnerInk)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { refreshKey++ }) {
                Text("REFRESH", color = OwnerInk, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "OWNER DASHBOARD",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = OwnerInk
        )
        Text(
            "ZORA MASSAGE / ADMINISTRATION",
            color = OwnerMuted,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OwnerStat("TOTAL USER", users.size.toString(), Modifier.weight(1f))
            OwnerStat("VERIFIED", users.count { it.verified }.toString(), Modifier.weight(1f))
            OwnerStat("SUSPENDED", users.count { it.suspended || it.disabled }.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Cari nama atau email") },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OwnerInk,
                focusedLabelColor = OwnerInk
            )
        )

        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFF287A43))
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OwnerInk)
                }
            }

            filteredUsers.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada pengguna ditemukan.", color = OwnerMuted)
                }
            }

            else -> {
                Text(
                    "DAFTAR PENGGUNA (${filteredUsers.size})",
                    color = OwnerInk,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it.uid }) { user ->
                        OwnerUserCard(
                            user = user,
                            busy = busyUid == user.uid,
                            onVerify = {
                                runAction(user.uid, "verify", true)
                            },
                            onUnverify = {
                                runAction(user.uid, "verify", false)
                            },
                            onSuspend = {
                                runAction(user.uid, "suspend")
                            },
                            onTemporarySuspend = {
                                runAction(user.uid, "temporary")
                            },
                            onUnsuspend = {
                                runAction(user.uid, "unsuspend")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = OwnerMuted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(5.dp))
            Text(
                value,
                color = OwnerInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun OwnerUserCard(
    user: OwnerUser,
    busy: Boolean,
    onVerify: () -> Unit,
    onUnverify: () -> Unit,
    onSuspend: () -> Unit,
    onTemporarySuspend: () -> Unit,
    onUnsuspend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                user.name.ifBlank { "Tanpa nama" },
                color = OwnerInk,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(user.email, color = OwnerMuted, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OwnerTag(
                    if (user.verified) "TERVERIFIKASI" else "BELUM VERIFIED",
                    if (user.verified) Color(0xFFE0F2E5) else Color(0xFFF0EEE8)
                )
                OwnerTag(
                    if (user.suspended || user.disabled) "SUSPENDED" else "AKTIF",
                    if (user.suspended || user.disabled) Color(0xFFFFE2DF) else Color(0xFFE0F2E5)
                )
            }

            if (user.suspensionType.isNotBlank()) {
                Text(
                    "Jenis suspend: ${user.suspensionType}",
                    color = OwnerMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(12.dp))

            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = OwnerInk
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user.verified) {
                        OutlinedButton(
                            onClick = onUnverify,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cabut centang")
                        }
                    } else {
                        Button(
                            onClick = onVerify,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OwnerYellow,
                                contentColor = OwnerInk
                            )
                        ) {
                            Text("Verifikasi")
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                if (user.suspended || user.disabled) {
                    Button(
                        onClick = onUnsuspend,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OwnerInk
                        )
                    ) {
                        Text("Pulihkan akun")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onTemporarySuspend,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Suspend 24 jam")
                        }
                        Button(
                            onClick = onSuspend,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB3261E)
                            )
                        ) {
                            Text("Suspend permanen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerTag(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = OwnerInk,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
