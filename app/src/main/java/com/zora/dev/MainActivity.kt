
package com.zora.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ZoraMassageApp()
            }
        }
    }
}

@Composable
fun ZoraMassageApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var isRegister by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (user != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ZORA MASSAGE", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text("Selamat datang, ${user?.displayName ?: user?.email}")

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        auth.signOut()
                        user = null
                        email = ""
                        password = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("LOGOUT")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "ZORA MASSAGE",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    if (isRegister) "Buat akun baru" else "Login ke akun kamu",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(28.dp))

                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.length < 6 ||
                            (isRegister && name.isBlank())
                        ) {
                            message = "Lengkapi data. Password minimal 6 karakter."
                            return@Button
                        }

                        scope.launch {
                            loading = true
                            message = ""

                            try {
                                if (isRegister) {
                                    val result = auth
                                        .createUserWithEmailAndPassword(
                                            email.trim(),
                                            password
                                        ).await()

                                    result.user?.updateProfile(
                                        com.google.firebase.auth.userProfileChangeRequest {
                                            displayName = name.trim()
                                        }
                                    )?.await()

                                    user = auth.currentUser
                                } else {
                                    auth.signInWithEmailAndPassword(
                                        email.trim(),
                                        password
                                    ).await()

                                    user = auth.currentUser
                                }
                            } catch (e: Exception) {
                                message = e.localizedMessage ?: "Terjadi kesalahan."
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            loading -> "MEMPROSES..."
                            isRegister -> "DAFTAR"
                            else -> "LOGIN"
                        }
                    )
                }

                TextButton(
                    onClick = {
                        isRegister = !isRegister
                        message = ""
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (isRegister) "Sudah punya akun? Login"
                        else "Belum punya akun? Daftar"
                    )
                }
            }
        }
    }
}
