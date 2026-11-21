
package com.zora.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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
        onDispose { auth.removeAuthStateListener(listener) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ZoraCream
    ) {
        if (currentUser == null) {
            AuthScreen(
                onSuccess = {
                    currentUser = auth.currentUser
                }
            )
        } else {
            MainDashboard(
                onLogout = {
                    auth.signOut()
                    currentUser = null
                }
            )
        }
    }
}

@Composable
fun MainDashboard(onLogout: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    var selectedTab by remember { mutableIntStateOf(0) }
    var isOwner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ownerRepository = remember { OwnerRepository() }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            try {
                ownerRepository.bootstrapOwner()
                user.getIdToken(true).awaitToken()
                isOwner = true
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
                    IconButton(onClick = { selectedTab = tabs.indexOf("Profil") }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZoraCream
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = ZoraWhite) {
                val icons = listOf(
                    Icons.Default.Chat,
                    Icons.Default.Circle,
                    Icons.Default.Groups,
                    Icons.Default.Call,
                    Icons.Default.Person
                ) + if (isOwner) listOf(Icons.Default.AdminPanelSettings) else emptyList()

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
                    if (isOwner) {
                        OwnerScreen()
                    } else {
                        Text("Akses ditolak.")
                    }
                }
            }
        }
    }
}

private suspend fun com.google.android.gms.tasks.Task<com.google.firebase.auth.GetTokenResult>.awaitToken() {
    kotlinx.coroutines.tasks.await(this)
}
