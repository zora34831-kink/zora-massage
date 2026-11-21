
package com.zora.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val Cream = Color(0xFFFFF9E9)
private val Yellow = Color(0xFFFFD94D)
private val Ink = Color(0xFF171717)
private val Muted = Color(0xFF77736B)
private val Line = Color(0xFF242424)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val colors = lightColorScheme(
                primary = Yellow,
                onPrimary = Ink,
                background = Cream,
                surface = Cream,
                onSurface = Ink,
                secondary = Ink
            )

            MaterialTheme(colorScheme = colors) {
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
        color = Cream
    ) {
        if (user != null) {
            ZoraDashboard(
                userName = user?.displayName ?: user?.email ?: "Pengguna",
                email = user?.email ?: "",
                onLogout = {
                    auth.signOut()
                    user = null
                    password = ""
                    message = ""
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 25.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Yellow, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Z",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "ZORA MASSAGE",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink
                )

                Text(
                    "Your conversations, all in one place.",
                    fontSize = 14.sp,
                    color = Muted
                )

                Spacer(Modifier.height(38.dp))

                Text(
                    if (isRegister) "Buat akun baru" else "Selamat datang kembali",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    if (isRegister) "Daftar untuk mulai menggunakan ZORA."
                    else "Masuk menggunakan akun ZORA kamu.",
                    color = Muted
                )

                Spacer(Modifier.height(25.dp))

                if (isRegister) {
                    ZoraField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nama lengkap"
                    )

                    Spacer(Modifier.height(13.dp))
                }

                ZoraField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Alamat email"
                )

                Spacer(Modifier.height(13.dp))

                ZoraField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    secret = true
                )

                if (!isRegister) {
                    TextButton(
                        onClick = {
                            if (email.isBlank()) {
                                message = "Masukkan email terlebih dahulu."
                            } else {
                                scope.launch {
                                    loading = true
                                    try {
                                        auth.sendPasswordResetEmail(
                                            email.trim()
                                        ).await()

                                        message = "Link reset password dikirim ke email kamu."
                                    } catch (e: Exception) {
                                        message = e.localizedMessage
                                            ?: "Gagal mengirim link reset."
                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Lupa password? Reset",
                            color = Ink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = if (message.contains("dikirim")) Color(0xFF247A42)
                        else Color(0xFFB3261E),
                        fontSize = 13.sp
                    )
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
                                        userProfileChangeRequest {
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
                                message = e.localizedMessage
                                    ?: "Terjadi kesalahan."
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Yellow,
                        contentColor = Ink
                    )
                ) {
                    Text(
                        when {
                            loading -> "MEMPROSES..."
                            isRegister -> "BUAT AKUN"
                            else -> "MASUK"
                        },
                        fontWeight = FontWeight.Black
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
                        if (isRegister)
                            "Sudah punya akun? Login"
                        else
                            "Belum punya akun? Daftar",
                        color = Ink,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "ZORA STORE",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ZoraField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    secret: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (secret)
            PasswordVisualTransformation()
        else
            androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = Line,
            focusedLabelColor = Ink,
            cursorColor = Ink
        )
    )
}

@Composable
fun ZoraDashboard(
    userName: String,
    email: String,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "Chat",
        "Status",
        "Grup",
        "Panggilan",
        "Setelan"
    )

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(
                containerColor = Cream,
                contentColor = Ink
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Text(
                                when (index) {
                                    0 -> "C"
                                    1 -> "S"
                                    2 -> "G"
                                    3 -> "P"
                                    else -> "A"
                                },
                                fontWeight = FontWeight.Black
                            )
                        },
                        label = {
                            Text(title, fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = Ink,
                            indicatorColor = Yellow,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ZORA MASSAGE",
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp,
                        color = Ink
                    )
                    Text(
                        "MESSAGING PLATFORM",
                        fontSize = 9.sp,
                        color = Muted,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(43.dp)
                        .background(Yellow, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            when (selectedTab) {
                0 -> {
                    Text(
                        "Pesan",
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Percakapan kamu akan muncul di sini.",
                        color = Muted
                    )
                    Spacer(Modifier.height(25.dp))
                    DashboardCard(
                        title = "Belum ada percakapan",
                        description = "Mulai percakapan setelah fitur kontak dan chat dihubungkan."
                    )
                }

                1 -> {
                    PageTitle("Status")
                    DashboardCard(
                        "Pembaruan status",
                        "Status foto, video, dan teks akan tersedia setelah fitur status dibuat."
                    )
                }

                2 -> {
                    PageTitle("Grup")
                    DashboardCard(
                        "Grup kamu",
                        "Buat grup dan mulai berkomunikasi bersama anggota."
                    )
                }

                3 -> {
                    PageTitle("Panggilan")
                    DashboardCard(
                        "Riwayat panggilan",
                        "Riwayat telepon suara dan video akan muncul di sini."
                    )
                }

                4 -> {
                    PageTitle("Setelan")

                    DashboardCard(
                        "Profil",
                        "$userName\n$email"
                    )

                    Spacer(Modifier.height(15.dp))

                    DashboardCard(
                        "Akun",
                        "Kelola informasi dan pengaturan akun kamu."
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow,
                            contentColor = Ink
                        )
                    ) {
                        Text(
                            "KELUAR AKUN",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageTitle(title: String) {
    Text(
        title,
        fontSize = 29.sp,
        fontWeight = FontWeight.Black,
        color = Ink
    )
    Spacer(Modifier.height(25.dp))
}

@Composable
fun DashboardCard(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFDF7),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Ink
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Ink
            )

            Spacer(Modifier.height(7.dp))

            Text(
                description,
                color = Muted,
                fontSize = 13.sp
            )
        }
    }
}
