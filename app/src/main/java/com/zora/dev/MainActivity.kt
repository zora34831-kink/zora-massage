
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
            "ZORA",
            style = MaterialTheme.typography.displayMedium,
            color = ZoraInk
        )

        Text(
            "MASSAGE",
            style = MaterialTheme.typography.headlineMedium,
            color = ZoraInk
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (isRegister) "Buat akun baru kamu."
            else "Selamat datang kembali.",
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
                message,
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
                        if (email.isBlank() || password.isBlank()) {
                            throw IllegalArgumentException(
                                "Email dan password wajib diisi."
                            )
                        }

                        if (isRegister) {
                            if (name.isBlank()) {
                                throw IllegalArgumentException(
                                    "Nama wajib diisi."
                                )
                            }
                            repository.register(name, email, password)
                        } else {
                            repository.login(email, password)
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
                Text(if (isRegister) "DAFTAR AKUN" else "MASUK")
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
                if (isRegister) "Sudah punya akun? Masuk"
                else "Belum punya akun? Daftar"
            )
        }

        if (!isRegister) {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            if (email.isBlank()) {
                                throw IllegalArgumentException(
                                    "Masukkan email terlebih dahulu."
                                )
                            }
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
    val ownerRepository = remember { OwnerRepository() }

    var selectedTab by remember { mutableStateOf("Chat") }
    var isOwner by remember { mutableStateOf(false) }
    var ownerChecked by remember { mutableStateOf(false) }
    var ownerError by remember { mutableStateOf("") }

    LaunchedEffect(user?.uid) {
        isOwner = false
        ownerChecked = false
        ownerError = ""

        if (user != null) {
            try {
                // Memastikan klaim owner sudah dipasang oleh backend.
                ownerRepository.bootstrapOwner()

                // Memaksa Firebase mengambil token terbaru.
                val token = user.getIdToken(true).await()
                isOwner = token.claims["owner"] == true
            } catch (e: Exception) {
                isOwner = false
                ownerError = e.localizedMessage
                    ?: "Pengecekan Owner gagal."
            } finally {
                ownerChecked = true
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

    if (selectedTab !in tabs) {
        selectedTab = "Chat"
    }

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
                            selectedTab = "Profil"
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
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                icons[index],
                                contentDescription = tab
                            )
                        },
                        label = {
                            Text(tab)
                        }
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
            when (selectedTab) {
                "Chat" -> ChatScreen()
                "Status" -> StatusScreen()
                "Grup" -> GroupScreen()
                "Panggilan" -> CallScreen()
                "Profil" -> ProfileScreen(onLogout)
                "Owner" -> {
                    if (isOwner) {
                        OwnerScreen(
                            onBack = {
                                selectedTab = "Chat"
                            }
                        )
                    } else {
                        Text("Akses ditolak.")
                    }
                }
            }

            if (!ownerChecked && user != null) {
                // Menu Owner ditampilkan setelah pemeriksaan selesai.
            }
        }
    }
}
