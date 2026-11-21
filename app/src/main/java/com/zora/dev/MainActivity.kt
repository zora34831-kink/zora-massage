
package com.zora.dev

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import java.util.concurrent.TimeUnit

private val Cream = Color(0xFFFFF9E9)
private val Yellow = Color(0xFFFFD84D)
private val Ink = Color(0xFF171717)
private val White = Color(0xFFFFFFFF)
private val Gray = Color(0xFF777777)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Cream
                ) {
                    ZoraApp()
                }
            }
        }
    }
}

@Composable
fun ZoraApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val activity = LocalContext.current as Activity

    var user by remember {
        mutableStateOf(auth.currentUser)
    }

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    if (user != null) {
        HomeScreen(
            onLogout = {
                auth.signOut()
                user = null
                phone = ""
                otp = ""
                verificationId = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Yellow, RoundedCornerShape(18.dp))
                .border(2.dp, Ink, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "ZORA MASSAGE",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = Ink
        )

        Text(
            "Your conversations, all in one place.",
            fontSize = 14.sp,
            color = Gray
        )

        Spacer(Modifier.height(36.dp))

        Text(
            if (verificationId == null) "Masuk dengan nomor HP"
            else "Verifikasi nomor HP",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (verificationId == null)
                "Masukkan nomor HP aktif dengan kode negara."
            else
                "Masukkan kode OTP yang dikirim melalui SMS.",
            color = Gray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        if (verificationId == null) {
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it.filter { char -> char.isDigit() || char == '+' }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nomor HP") },
                placeholder = { Text("+628123456789") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = inputColors()
            )
        } else {
            OutlinedTextField(
                value = otp,
                onValueChange = {
                    otp = it.filter(Char::isDigit).take(6)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kode OTP") },
                placeholder = { Text("6 digit kode") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = inputColors()
            )
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = Color(0xFFB3261E), fontSize = 13.sp)
        }

        if (info.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(info, color = Color(0xFF287A42), fontSize = 13.sp)
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = {
                error = ""
                info = ""

                if (verificationId == null) {
                    if (phone.length < 9 || !phone.startsWith("+")) {
                        error = "Gunakan format nomor internasional, contoh +628..."
                        return@Button
                    }

                    loading = true

                    val callbacks =
                        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                            override fun onVerificationCompleted(
                                credential: PhoneAuthCredential
                            ) {
                                activity.runOnUiThread {
                                    auth.signInWithCredential(credential)
                                        .addOnCompleteListener { task ->
                                            loading = false
                                            if (task.isSuccessful) {
                                                user = auth.currentUser
                                            } else {
                                                error = task.exception?.localizedMessage
                                                    ?: "Verifikasi gagal."
                                            }
                                        }
                                }
                            }

                            override fun onVerificationFailed(
                                e: com.google.firebase.FirebaseException
                            ) {
                                activity.runOnUiThread {
                                    loading = false
                                    error = e.localizedMessage
                                        ?: "Gagal mengirim OTP."
                                }
                            }

                            override fun onCodeSent(
                                id: String,
                                token: PhoneAuthProvider.ForceResendingToken
                            ) {
                                activity.runOnUiThread {
                                    loading = false
                                    verificationId = id
                                    info = "Kode OTP berhasil dikirim."
                                }
                            }
                        }

                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(callbacks)
                        .build()

                    PhoneAuthProvider.verifyPhoneNumber(options)
                } else {
                    val id = verificationId

                    if (otp.length != 6 || id == null) {
                        error = "Masukkan kode OTP 6 digit."
                        return@Button
                    }

                    loading = true

                    val credential =
                        PhoneAuthProvider.getCredential(id, otp)

                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                user = auth.currentUser
                            } else {
                                error = task.exception?.localizedMessage
                                    ?: "Kode OTP salah atau kedaluwarsa."
                            }
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor = Ink
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Ink,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (verificationId == null) "KIRIM KODE OTP"
                    else "VERIFIKASI & MASUK",
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (verificationId != null) {
            Text(
                "Ganti nomor HP",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        verificationId = null
                        otp = ""
                        error = ""
                        info = ""
                    }
                    .padding(16.dp),
                color = Ink,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "ZORA STORE",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Gray
        )
    }
}

@Composable
fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Ink,
    unfocusedBorderColor = Color(0xFFBDB7A8),
    focusedLabelColor = Ink,
    cursorColor = Ink
)

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        "Chats",
        "Updates",
        "Groups",
        "Calls",
        "Settings"
    )

    val icons = listOf(
        Icons.Default.Chat,
        Icons.Default.Circle,
        Icons.Default.Groups,
        Icons.Default.Call,
        Icons.Default.Settings
    )

    Scaffold(
        containerColor = Cream,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Yellow)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "ZORA MASSAGE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    Text(
                        "Stay connected.",
                        fontSize = 12.sp,
                        color = Ink
                    )
                }

                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = "Keluar",
                        tint = Ink
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = White,
                contentColor = Ink
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(icons[index], contentDescription = title)
                        },
                        label = {
                            Text(title, fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = Ink,
                            indicatorColor = Yellow,
                            unselectedIconColor = Gray
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
                .padding(20.dp)
        ) {
            Text(
                tabs[selectedTab],
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Ink
            )

            Spacer(Modifier.height(18.dp))

            when (selectedTab) {
                0 -> {
                    Text(
                        "Percakapan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    EmptyCard(
                        "Belum ada percakapan",
                        "Percakapan baru akan muncul di sini."
                    )
                }

                1 -> EmptyCard(
                    "Updates",
                    "Status teman akan tampil di sini."
                )

                2 -> EmptyCard(
                    "Communities",
                    "Grup dan komunitas akan tampil di sini."
                )

                3 -> EmptyCard(
                    "Calls",
                    "Riwayat panggilan akan tampil di sini."
                )

                4 -> {
                    EmptyCard(
                        "Pengaturan akun",
                        "Pengaturan profil dan privasi akan tersedia di sini."
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Developer: ZORA STORE",
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCard(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Ink, RoundedCornerShape(16.dp))
            .background(White, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = Ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            color = Gray,
            fontSize = 13.sp
        )
    }
}
