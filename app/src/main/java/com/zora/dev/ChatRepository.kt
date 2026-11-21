
package com.zora.dev

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ZoraUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val verified: Boolean = false
)

data class ZoraMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)

data class ZoraConversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val updatedAt: Long = 0L
)

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun uid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Silakan login terlebih dahulu.")
    }

    fun observeUsers(): Flow<List<ZoraUser>> = callbackFlow {
        val myUid = try {
            uid()
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val registration = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val result = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        val userUid = doc.getString("uid") ?: doc.id

                        if (userUid == myUid) return@mapNotNull null

                        ZoraUser(
                            uid = userUid,
                            name = doc.getString("name") ?: "Pengguna ZORA",
                            email = doc.getString("email") ?: "",
                            verified = doc.getBoolean("verified") ?: false
                        )
                    }

                trySend(result)
            }

        awaitClose { registration.remove() }
    }

    suspend fun startConversation(otherUid: String): String {
        val myUid = uid()

        require(otherUid.isNotBlank() && otherUid != myUid) {
            "Pengguna tidak valid."
        }

        val participants = listOf(myUid, otherUid).sorted()

        val existing = db.collection("conversations")
            .whereArrayContains("participants", myUid)
            .get()
            .await()
            .documents
            .firstOrNull { doc ->
                val members = (doc.get("participants") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.sorted()

                members == participants
            }

        if (existing != null) return existing.id

        val ref = db.collection("conversations").document()

        ref.set(
            mapOf(
                "participants" to participants,
                "lastMessage" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        return ref.id
    }

    fun observeConversations(): Flow<List<ZoraConversation>> =
        callbackFlow {
            val myUid = try {
                uid()
            } catch (e: Exception) {
                close(e)
                return@callbackFlow
            }

            val registration = db.collection("conversations")
                .whereArrayContains("participants", myUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val result = snapshot?.documents.orEmpty()
                        .map { doc ->
                            ZoraConversation(
                                id = doc.id,
                                participants = (
                                    doc.get("participants") as? List<*>
                                )?.filterIsInstance<String>() ?: emptyList(),
                                lastMessage = doc.getString("lastMessage") ?: "",
                                updatedAt = doc.getTimestamp("updatedAt")
                                    ?.toDate()?.time ?: 0L
                            )
                        }
                        .sortedByDescending { it.updatedAt }

                    trySend(result)
                }

            awaitClose { registration.remove() }
        }

    fun observeMessages(
        conversationId: String
    ): Flow<List<ZoraMessage>> = callbackFlow {

        val registration = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val result = snapshot?.documents.orEmpty()
                    .map { doc ->
                        ZoraMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            text = doc.getString("text") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                                ?.toDate()?.time ?: 0L
                        )
                    }

                trySend(result)
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String
    ) {
        val myUid = uid()
        val cleanText = text.trim()

        require(conversationId.isNotBlank()) {
            "Percakapan tidak valid."
        }

        require(cleanText.isNotBlank()) {
            "Pesan tidak boleh kosong."
        }

        require(cleanText.length <= 5000) {
            "Pesan terlalu panjang."
        }

        val conversation = db.collection("conversations")
            .document(conversationId)

        val message = conversation.collection("messages")
            .document()

        db.runBatch { batch ->
            batch.set(
                message,
                mapOf(
                    "senderId" to myUid,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.update(
                conversation,
                mapOf(
                    "lastMessage" to cleanText,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }
}
