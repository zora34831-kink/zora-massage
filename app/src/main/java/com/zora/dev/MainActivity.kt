
package com.zora.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val Ink = Color(0xFF101827)
private val Blue = Color(0xFF3978E8)
private val SurfaceDark = Color(0xFF192437)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                UserApp()
            }
        }
    }
}

@Composable
fun UserApp() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loggedIn by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    if (!loggedIn) {
        Column(
            Modifier.fillMaxSize()
                .background(Ink)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("ZORA", color = Color.White, fontSize = 36.sp,
                fontWeight = FontWeight.Bold)
            Text("MASSAGE", color = Blue, fontSize = 16.sp)
            Spacer(Modifier.height(36.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    loading = true
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener {
                            loading = false
                            if (it.isSuccessful) loggedIn = true
                            else error = it.exception?.localizedMessage ?: "Login gagal"
                        }
                },
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Memproses..." else "MASUK")
            }

            Text(error, color = Color.Red)
        }
    } else {
        UserHome(
            onLogout = {
                FirebaseAuth.getInstance().signOut()
                loggedIn = false
            }
        )
    }
}

@Composable
fun UserHome(onLogout: () -> Unit) {
    var selected by remember { mutableStateOf("Chat") }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                listOf("Chat", "Grup", "Status", "Profil").forEach { item ->
                    NavigationBarItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = {
                            Icon(
                                when (item) {
                                    "Chat" -> Icons.Default.Chat
                                    "Grup" -> Icons.Default.Groups
                                    "Status" -> Icons.Default.Circle
                                    else -> Icons.Default.Person
                                },
                                contentDescription = item
                            )
                        },
                        label = { Text(item) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .background(Ink)
                .padding(padding)
                .padding(22.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ZORA MASSAGE", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("Private communication", color = Color.Gray)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, "Keluar", tint = Color.White)
                }
            }

            Spacer(Modifier.height(30.dp))

            when (selected) {
                "Chat" -> {
                    Text("Pesan", color = Color.White, fontSize = 25.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada percakapan.", color = Color.Gray)
                }
                "Grup" -> Text("Grup", color = Color.White, fontSize = 25.sp)
                "Status" -> Text("Status", color = Color.White, fontSize = 25.sp)
                "Profil" -> {
                    Text("Profil akun", color = Color.White, fontSize = 25.sp)
                    Text(
                        FirebaseAuth.getInstance().currentUser?.email ?: "",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
