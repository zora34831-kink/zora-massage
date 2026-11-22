
package com.zora.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.google.firebase.functions.FirebaseFunctions

private val Dark = Color(0xFF101827)
private val Accent = Color(0xFF4385F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AdminApp() } }
    }
}

@Composable
fun AdminApp() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(Dark).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "ZORA MASSAGE",
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
        Text("ADMIN CONSOLE", color = Accent)
        Spacer(Modifier.height(30.dp))

        if (!owner) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Admin email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))

            Button(
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                onClick = {
                    loading = true
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                loading = false
                                message = "Login gagal."
                            } else {
                                FirebaseAuth.getInstance().currentUser
                                    ?.getIdToken(true)
                                    ?.addOnCompleteListener { tokenTask ->
                                        loading = false
                                        if (tokenTask.isSuccessful &&
                                            tokenTask.result?.claims?.get("owner") == true
                                        ) {
                                            owner = true
                                        } else {
                                            FirebaseAuth.getInstance().signOut()
                                            message = "Akun tidak memiliki akses Admin."
                                        }
                                    }
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Memeriksa..." else "LOGIN ADMIN")
            }
        } else {
            Text("ADMIN DASHBOARD", color = Color.White, fontSize = 23.sp)
            Spacer(Modifier.height(12.dp))
            Text("Akses Owner aktif.", color = Color.LightGray)
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    owner = false
                }
            ) {
                Text("Keluar")
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(message, color = Color.Red)
    }
}
