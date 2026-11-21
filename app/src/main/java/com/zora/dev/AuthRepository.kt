
package com.zora.dev

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String) {
        require(email.isNotBlank()) { "Email wajib diisi." }
        require(password.isNotBlank()) { "Password wajib diisi." }

        auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).await()
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

        val firebaseUser = result.user
            ?: throw IllegalStateException("Akun gagal dibuat.")

        val profile = userProfileChangeRequest {
            displayName = name.trim()
        }

        firebaseUser.updateProfile(profile).await()

        db.collection("users")
            .document(firebaseUser.uid)
            .set(
                mapOf(
                    "uid" to firebaseUser.uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun resetPassword(email: String) {
        require(email.isNotBlank()) {
            "Masukkan email terlebih dahulu."
        }

        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUser() = auth.currentUser
}
