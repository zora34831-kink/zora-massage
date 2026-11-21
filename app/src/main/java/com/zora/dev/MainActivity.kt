
package com.zora.dev

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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE_URL = "https://GANTI-DENGAN-URL-RENDER"

private val Cream = Color(0xFFFFF9E9)
private val Yellow = Color(0xFFFFD84D)
private val Ink = Color(0xFF171717)
private val Gray = Color(0xFF777777)
private val White = Color.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            MaterialTheme {
                ZoraRoot()
            }
        }
    }
}

private suspend fun postApi(
    path: String,
    data: JSONObject
): JSONObject = withContext(Dispatchers.IO) {
    require(!API_BASE_URL.contains("GANTI-DENGAN")) {
        "URL backend belum diatur."
    }

    val connection = URL(
        API_BASE_URL.trimEnd('/') + path
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        connection.outputStream.use {
            it.write(data.toString().toByteArray(Charsets.UTF_8))
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = stream?.bufferedReader()?.use {
            it.readText()
        } ?: "{}"

        val json = JSONObject(response)

        if (status !in 200..299) {
            throw Exception(
                json.optString("error", "Permintaan gagal.")
            )
        }

        json
    } finally {
        connection.disconnect()
    }
}

@Composable
fun ZoraRoot() {
    val auth = remember { FirebaseAuth.getInstance() }
    var loggedIn by remember {
        mutableStateOf(auth.currentUser != null)
    }

    if (loggedIn) {
        HomeScreen {
            auth.signOut()
            loggedIn = false
        }
    } else {
        AuthScreen(auth) {
            loggedIn = true
        }
    }
}

@Composable
fun AuthScreen(
    auth: FirebaseAuth,
    onLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var register by remember { mutableStateOf(false) }
    var otpStep by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(Yellow, RoundedCornerShape(18.dp))
                .border(2.dp, Ink, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "ZORA MASSAGE",
            fontSize = 29.sp,
            fontWeight = FontWeight.Black,
            color = Ink
        )

        Text(
            "Your conversations, all in one place.",
            color = Gray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(30.dp))

        Text(
            when {
                otpStep -> "Verifikasi email"
                register -> "Buat akun baru"
                else -> "Selamat datang kembali"
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            when {
                otpStep -> "Masukkan kode OTP 6 digit dari email."
                register -> "Isi data untuk membuat akun."
                else -> "Login dengan email dan password."
            },
            fontSize = 13.sp,
            color = Gray
        )

        Spacer(Modifier.height(20.dp))

        if (register && !otpStep) {
            Field(name, { name = it }, "Nama")
            Spacer(Modifier.height(10.dp))
        }

        Field(
            email,
            { email = it },
            "Email",
            KeyboardType.Email,
            enabled = !otpStep
        )

        Spacer(Modifier.height(10.dp))

        if (otpStep) {
            Field(
                otp,
                { otp = it.filter(Char::isDigit).take(6) },
                "Kode OTP",
                KeyboardType.Number
            )
        } else {
            Field(
                password,
                { password = it },
                "Password",
                password = true
            )

            if (register) {
                Spacer(Modifier.height(10.dp))
                Field(
                    confirm,
                    { confirm = it },
                    "Konfirmasi password",
                    password = true
                )
            }
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFF287A42), fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                error = ""
                message = ""

                when {
                    otpStep -> {
                        if (!otp.matches(Regex("\\d{6}"))) {
                            error = "Masukkan OTP 6 digit."
                            return@Button
                        }

                        loading = true

                        scope.launch {
                            try {
                                postApi(
                                    "/api/register/verify-otp",
                                    JSONObject().apply {
                                        put("email", email.trim())
                                        put("otp", otp)
                                    }
                                )

                                auth.signInWithEmailAndPassword(
                                    email.trim(),
                                    password
                                ).addOnCompleteListener { task ->
                                    loading = false
                                    if (task.isSuccessful) {
                                        onLogin()
                                    } else {
                                        error =
                                            "Akun berhasil dibuat. Silakan login kembali."
                                        register = false
                                        otpStep = false
                                    }
                                }
                            } catch (e: Exception) {
                                loading = false
                                error = e.message ?: "Verifikasi gagal."
                            }
                        }
                    }

                    register -> {
                        if (name.trim().length < 2) {
                            error = "Nama minimal 2 karakter."
                            return@Button
                        }

                        if (!android.util.Patterns.EMAIL_ADDRESS
                                .matcher(email.trim()).matches()) {
                            error = "Email tidak valid."
                            return@Button
                        }

                        if (password.length < 8 || password.length > 72) {
                            error = "Password harus 8–72 karakter."
                            return@Button
                        }

                        if (password != confirm) {
                            error = "Konfirmasi password tidak sama."
                            return@Button
                        }

                        loading = true

                        scope.launch {
                            try {
                                val result = postApi(
                                    "/api/register/request-otp",
                                    JSONObject().apply {
                                        put("name", name.trim())
                                        put("email", email.trim())
                                        put("password", password)
                                    }
                                )

                                loading = false
                                otpStep = true
                                message = result.optString(
                                    "message",
                                    "OTP terkirim."
                                )
                            } catch (e: Exception) {
                                loading = false
                                error = e.message ?: "Gagal mengirim OTP."
                            }
                        }
                    }

                    else -> {
                        if (email.isBlank() || password.isBlank()) {
                            error = "Email dan password wajib diisi."
                            return@Button
                        }

                        loading = true

                        auth.signInWithEmailAndPassword(
                            email.trim(),
                            password
                        ).addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                onLogin()
                            } else {
                                error = "Email atau password salah."
                            }
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor = Ink
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Ink,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    when {
                        otpStep -> "VERIFIKASI OTP"
                        register -> "KIRIM OTP KE EMAIL"
                        else -> "MASUK"
                    },
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (otpStep) {
            Text(
                "Kembali",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        otpStep = false
                        error = ""
                        message = ""
                    }
                    .padding(12.dp),
                color = Ink,
                fontWeight = FontWeight.Bold
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    if (register) "Sudah punya akun? " else "Belum punya akun? ",
                    color = Gray,
                    fontSize = 13.sp
                )
                Text(
                    if (register) "Login" else "Daftar",
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable {
                        register = !register
                        error = ""
                        message = ""
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "ZORA STORE",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = Color(0xFFBDB7A8),
            cursorColor = Ink
        )
    )
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }

    val labels = listOf("Chats", "Updates", "Groups", "Calls", "Settings")
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
                Modifier
                    .fillMaxWidth()
                    .background(Yellow)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "ZORA MASSAGE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    Text("Stay connected.", color = Ink, fontSize = 12.sp)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = White) {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = {
                            Icon(icons[index], contentDescription = label)
                        },
                        label = { Text(label, fontSize = 10.sp) },
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                labels[selected],
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Ink
            )

            Spacer(Modifier.height(18.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .border(2.dp, Ink, RoundedCornerShape(16.dp))
                    .background(White, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Text(
                    "Belum tersedia",
                    fontWeight = FontWeight.Black,
                    color = Ink
                )
                Text(
                    "Fitur ${labels[selected]} akan ditambahkan pada tahap berikutnya.",
                    color = Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}
