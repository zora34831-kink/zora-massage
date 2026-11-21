
package com.zora.dev

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    var name by remember {
        mutableStateOf(user?.displayName ?: "")
    }
    var email by remember {
        mutableStateOf(user?.email ?: "")
    }
    var verified by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    name = document.getString("name") ?: name
                    email = document.getString("email") ?: email
                    verified = document.getBoolean("verified") ?: false
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(22.dp))

        Text("Nama", color = ZoraMuted)
        Text(name, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        Text("Email", color = ZoraMuted)
        Text(email, style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(12.dp))

        Text(
            if (verified) "✓ Akun terverifikasi" else "Akun belum terverifikasi",
            color = if (verified) MaterialTheme.colorScheme.primary else ZoraMuted
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                message = "Pengaturan profil lanjutan belum tersedia."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit profil")
        }

        if (message.isNotBlank()) {
            Text(message, color = ZoraMuted)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Keluar akun")
        }
    }
}
