
package com.zora.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZoraTheme {
                ZoraApp()
            }
        }
    }
}

@Composable
fun ZoraApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            currentUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ZoraCream
    ) {
        if (currentUser == null) {
            AuthScreen()
        } else {
            MainDashboard {
                auth.signOut()
                currentUser = null
            }
        }
    }
}

@Composable
fun AuthScreen() {
    val repository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(26.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ZORA",
            style = MaterialTheme.typography.displayMedium,
            color = ZoraInk
        )

        Text(
            text = "MASSAGE",
            style = MaterialTheme.typography.headlineMedium,
            color = ZoraInk
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isRegister)
                "Buat akun baru kamu."
            else
                "Selamat datang kembali.",
            color = ZoraMuted
        )

        Spacer(Modifier.height(28.dp))

        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama lengkap") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
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

        Spacer(Modifier.height(18.dp))

        if (message.isNotBlank()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(10.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    message = ""

                    try {
                        if (isRegister) {
                            repository.register(
                                name,
                                email,
                                password
                            )
                        } else {
                            repository.login(
                                email,
                                password
                            )
                        }
                    } catch (e: Exception) {
                        message = e.localizedMessage
                            ?: "Terjadi kesalahan."
                    }

                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = ZoraInk
                )
            } else {
                Text(
                    if (isRegister) "DAFTAR AKUN"
                    else "MASUK"
                )
            }
        }

        TextButton(
            onClick = {
                isRegister = !isRegister
                message = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isRegister)
                    "Sudah punya akun? Masuk"
                else
                    "Belum punya akun? Daftar"
            )
        }

        if (!isRegister) {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            repository.resetPassword(email)
                            message = "Email reset password telah dikirim."
                        } catch (e: Exception) {
                            message = e.localizedMessage
                                ?: "Gagal mengirim email."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lupa password?")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(onLogout: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser

    var selectedTab by remember { mutableIntStateOf(0) }
    var isOwner by remember { mutableStateOf(false) }

    val ownerRepository = remember { OwnerRepository() }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            try {
                ownerRepository.bootstrapOwner()
                val token = user.getIdToken(true).await()
                isOwner = token.claims["owner"] == true
            } catch (_: Exception) {
                isOwner = false
            }
        }
    }

    val tabs = buildList {
        add("Chat")
        add("Status")
        add("Grup")
        add("Panggilan")
        add("Profil")
        if (isOwner) add("Owner")
    }

    Scaffold(
        containerColor = ZoraCream,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ZORA MASSAGE",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "PRIVATE MESSAGING",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZoraMuted
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedTab = tabs.indexOf("Profil")
                        }
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profil"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZoraCream
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ZoraWhite
            ) {
                val icons = listOf(
                    Icons.Default.Chat,
                    Icons.Default.Circle,
                    Icons.Default.Groups,
                    Icons.Default.Call,
                    Icons.Default.Person
                ) + if (isOwner) {
                    listOf(Icons.Default.AdminPanelSettings)
                } else {
                    emptyList()
                }

                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                icons[index],
                                contentDescription = tab
                            )
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tabs.getOrNull(selectedTab)) {
                "Chat" -> ChatScreen()
                "Status" -> StatusScreen()
                "Grup" -> GroupScreen()
                "Panggilan" -> CallScreen()
                "Profil" -> ProfileScreen(onLogout)
                "Owner" -> {
                    if (isOwner) OwnerScreen()
                    else Text("Akses ditolak.")
                }
            }
        }
    }
}
