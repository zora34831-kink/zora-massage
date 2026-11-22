
package com.zora.dev

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class OwnerUser(
    val uid: String,
    val email: String,
    val name: String,
    val disabled: Boolean,
    val verified: Boolean,
    val suspended: Boolean,
    val suspensionType: String
)

class OwnerRepository(
    private val functions: FirebaseFunctions =
        FirebaseFunctions.getInstance()
) {

    suspend fun getUsers(): List<OwnerUser> {
        val result = functions
            .getHttpsCallable("ownerListUsers")
            .call()
            .await()

        val data = result.getData() as? Map<*, *>
            ?: return emptyList()

        val users = data["users"] as? List<*>
            ?: return emptyList()

        return users.mapNotNull { item ->
            val user = item as? Map<*, *>
                ?: return@mapNotNull null

            OwnerUser(
                uid = user["uid"] as? String ?: "",
                email = user["email"] as? String ?: "",
                name = user["name"] as? String ?: "",
                disabled = user["disabled"] as? Boolean ?: false,
                verified = user["verified"] as? Boolean ?: false,
                suspended = user["suspended"] as? Boolean ?: false,
                suspensionType =
                    user["suspensionType"] as? String ?: ""
            )
        }
    }

    suspend fun setVerified(
        uid: String,
        verified: Boolean
    ) {
        functions
            .getHttpsCallable("ownerSetVerified")
            .call(
                mapOf(
                    "uid" to uid,
                    "verified" to verified
                )
            )
            .await()
    }

    suspend fun suspendUser(
        uid: String,
        durationHours: Int?
    ) {
        functions
            .getHttpsCallable("ownerSuspendUser")
            .call(
                mapOf(
                    "uid" to uid,
                    "durationHours" to durationHours
                )
            )
            .await()
    }

    suspend fun unsuspendUser(uid: String) {
        functions
            .getHttpsCallable("ownerUnsuspendUser")
            .call(
                mapOf("uid" to uid)
            )
            .await()
    }
}
