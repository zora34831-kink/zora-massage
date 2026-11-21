
package com.zora.dev

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ZoraMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class ZoraConversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = ""
)

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun currentUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Kamu belum login.")
    }

    fun conversationId(otherUid: String): String {
        val myUid = currentUid()

        require(otherUid.isNotBlank() && otherUid != myUid) {
            "Pengguna tidak valid."
        }

        return listOf(myUid, otherUid)
            .sorted()
            .joinToString("_")
    }

    suspend fun createUserProfile(name: String) {
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("Belum login.")

        val cleanName = name.trim().ifBlank {
            firebaseUser.email?.substringBefore("@") ?: "Pengguna"
        }

        val data = mapOf(
            "uid" to firebaseUser.uid,
            "name" to cleanName,
            "email" to (firebaseUser.email ?: ""),
            "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(firebaseUser.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun startConversation(otherUid: String): String {
        val myUid = currentUid()

        require(otherUid.isNotBlank() && otherUid != myUid) {
            "Pengguna tidak valid."
        }

        val id = conversationId(otherUid)

        val data = mapOf(
            "participants" to listOf(myUid, otherUid).sorted(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("conversations")
            .document(id)
            .set(data, SetOptions.merge())
            .await()

        return id
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String
    ) {
        val uid = currentUid()
        val cleanText = text.trim()

        require(cleanText.isNotEmpty()) {
            "Pesan tidak boleh kosong."
        }

        require(cleanText.length <= 5000) {
            "Pesan terlalu panjang."
        }

        val conversation = db.collection("conversations")
            .document(conversationId)

        val messageRef = conversation
            .collection("messages")
            .document()

        val messageData = mapOf(
            "senderId" to uid,
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.runBatch { batch ->
            batch.set(messageRef, messageData)

            batch.update(
                conversation,
                mapOf(
                    "lastMessage" to cleanText,
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    fun observeMessages(
        conversationId: String
    ): Flow<List<ZoraMessage>> = callbackFlow {

        val listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(100)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().map { doc ->
                    ZoraMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }

                trySend(messages)
            }

        awaitClose {
            listener.remove()
        }
    }

    fun observeConversations(): Flow<List<ZoraConversation>> =
        callbackFlow {

            val uid = currentUid()

            val listener = db.collection("conversations")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val conversations =
                        snapshot?.documents.orEmpty()
                            .map { doc ->
                                ZoraConversation(
                                    id = doc.id,
                                    participants = doc.get("participants")
                                        as? List<String> ?: emptyList(),
                                    lastMessage = doc.getString(
                                        "lastMessage"
                                    ) ?: ""
                                )
                            }
                            .sortedByDescending { conversation ->
                                snapshot?.documents
                                    ?.firstOrNull {
                                        it.id == conversation.id
                                    }
                                    ?.getTimestamp("updatedAt")
                            }

                    trySend(conversations)
                }

            awaitClose {
                listener.remove()
            }
        }
}
