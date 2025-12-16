package com.nidoham.socialsphere.database.cloud.helper

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.nidoham.socialsphere.database.cloud.path.FirebasePath
import kotlin.random.Random

object CheckAvailableUsername {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = database.collection(FirebasePath.USERS)

    // Returns unique username OR null if failed
    fun check(originalUsername: String, callback: (String?) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            callback(null)
            return
        }
        val uid = currentUser.uid

        val cleanUsername = originalUsername.lowercase().trim()
            .replace(Regex("[^a-z0-9_]"), "")
            .take(20)

        if (cleanUsername.length < 3) {
            callback(generateRandomUsername(cleanUsername))
            return
        }

        findUniqueUsername(cleanUsername, uid, callback)
    }

    private fun findUniqueUsername(username: String, uid: String, callback: (String?) -> Unit) {
        val userDoc = usersCollection.document(uid)

        // Check exact match first
        userDoc.get().addOnSuccessListener { snapshot: DocumentSnapshot ->
            if (isAvailable(snapshot, username)) {
                userDoc.update("username", username)
                    .addOnSuccessListener { callback(username) }
                return@addOnSuccessListener
            }

            // Try numbered versions
            tryNumbered(username, uid, 1, callback)
        }
    }

    private fun tryNumbered(username: String, uid: String, number: Int, callback: (String?) -> Unit) {
        if (number > 100) {
            callback(generateRandomUsername(username))
            return
        }

        val candidate = "${username}${number}"
        val userDoc = usersCollection.document(uid)

        userDoc.get().addOnSuccessListener { snapshot: DocumentSnapshot ->
            if (isAvailable(snapshot, candidate)) {
                userDoc.update("username", candidate)
                    .addOnSuccessListener { callback(candidate) }
            } else {
                tryNumbered(username, uid, number + 1, callback)
            }
        }
    }

    private fun isAvailable(snapshot: DocumentSnapshot, username: String): Boolean {
        val existing = snapshot.getString("username")
        return existing.isNullOrBlank() || existing != username
    }

    private fun generateRandomUsername(base: String = ""): String {
        val randomNum = Random.nextInt(1000, 99999)
        val prefix = if (base.isNotEmpty()) base.take(8) else "user"
        return "${prefix}${randomNum}"
    }
}