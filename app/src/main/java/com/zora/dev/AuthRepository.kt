
package com.zora.dev

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun login(email: String, password: String) {
        require(email.isNotBlank()) { "Email wajib diisi." }
        require(password.isNotBlank()) { "Password wajib diisi." }

        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun register(
        name: String,
        email: String,
        password: String
    ) {
        require(name.isNotBlank()) { "Nama wajib diisi." }
        require(email.isNotBlank()) { "Email wajib diisi." }
        require(password.length >= 6) {
            "Password minimal 6 karakter."
        }

        val result = auth.createUserWithEmailAndPassword(
            email.trim(),
            password
        ).await()

        val profile = userProfileChangeRequest {
            displayName = name.trim()
        }

        result.user?.updateProfile(profile)?.await()

        val uid = result.user?.uid
            ?: throw IllegalStateException("Akun gagal dibuat.")

        com.google.firebase.firestore.FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "verified" to false,
                    "createdAt" to
                        com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun resetPassword(email: String) {
        require(email.isNotBlank()) { "Masukkan email terlebih dahulu." }
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUser() = auth.currentUser
}
