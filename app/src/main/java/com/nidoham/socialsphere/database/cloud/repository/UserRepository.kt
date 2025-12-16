package com.nidoham.socialsphere.database.cloud.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nidoham.socialsphere.database.cloud.model.User
import com.nidoham.socialsphere.database.cloud.path.FirebasePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class UserRepository private constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "UserRepository"

        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(
            firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
            auth: FirebaseAuth = FirebaseAuth.getInstance()
        ): UserRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserRepository(firestore, auth).also { INSTANCE = it }
            }
        }
    }

    private val usersCollection = firestore.collection(FirebasePath.USERS)

    // ==================== CREATE ====================

    /**
     * Creates a new user profile in Firestore
     * @param displayName User's display name
     * @param email User's email (optional)
     * @return Result containing the created User or an error
     */
    suspend fun createUser(displayName: String, email: String? = null): Result<User> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not authenticated"))

        val username = generateUniqueUsername(displayName)
        val timestamp = System.currentTimeMillis()

        val newUser = User(
            id = userId,
            username = username,
            displayName = displayName,
            email = email,
            createdAt = timestamp,
            updatedAt = timestamp,
            lastActiveAt = timestamp
        )

        return try {
            usersCollection.document(userId)
                .set(newUser, SetOptions.merge())
                .await()
            Result.success(newUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            Result.failure(Exception("Failed to create user: ${e.message}", e))
        }
    }

    /**
     * Saves or updates a complete user object
     * @param user User object to save
     * @return True if successful, false otherwise
     */
    suspend fun saveUser(user: User): Boolean {
        return try {
            usersCollection.document(user.id)
                .set(user, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user", e)
            false
        }
    }

    // ==================== READ ====================

    /**
     * Retrieves a user profile by ID
     * @param userId User ID to fetch
     * @return User object or null if not found
     */
    suspend fun getUserById(userId: String): User? {
        if (userId.isBlank()) return null

        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID", e)
            null
        }
    }

    /**
     * Retrieves the current authenticated user's profile
     * @return User object or null if not found
     */
    suspend fun getCurrentUser(): User? {
        val userId = auth.currentUser?.uid ?: return null
        return getUserById(userId)
    }

    /**
     * Checks if a user exists in the database
     * @param userId User ID to check
     * @return True if user exists, false otherwise
     */
    suspend fun userExists(userId: String): Boolean {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence", e)
            false
        }
    }

    // ==================== UPDATE ====================

    /**
     * Updates specific user profile fields
     * @param userId User ID to update
     * @param updates Map of field names to new values
     * @return Result indicating success or failure
     */
    suspend fun updateUser(userId: String, updates: Map<String, Any?>): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }

        val fieldsToUpdate = updates.toMutableMap().apply {
            this["updatedAt"] = FieldValue.serverTimestamp()
            this["lastActiveAt"] = FieldValue.serverTimestamp()
        }

        return try {
            usersCollection.document(userId)
                .update(fieldsToUpdate)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            Result.failure(Exception("Failed to update user: ${e.message}", e))
        }
    }

    /**
     * Updates user profile with selective field updates
     * @param userId User ID to update
     * @param user User object with fields to update
     * @return True if successful, false otherwise
     */
    suspend fun updateUserProfile(userId: String, user: User): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>()

            // Update username
            user.username.takeIf { it.isNotBlank() }?.let {
                updates["username"] = it
            }

            // Update display name
            user.displayName?.takeIf { it.isNotBlank() }?.let {
                updates["displayName"] = it
            }

            // Update bio
            user.bio?.takeIf { it.isNotBlank() }?.let {
                updates["bio"] = it
            }

            // Update avatar
            user.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                updates["avatarUrl"] = it
            }

            // Update cover photo
            user.coverPhotoUrl?.takeIf { it.isNotBlank() }?.let {
                updates["coverPhotoUrl"] = it
            }

            // Update privacy setting
            if (user.isPrivateAccount != getUserById(userId)?.isPrivateAccount) {
                updates["isPrivateAccount"] = user.isPrivateAccount
            }

            // Add server timestamp
            updates["updatedAt"] = FieldValue.serverTimestamp()

            if (updates.isNotEmpty()) {
                usersCollection.document(userId).update(updates).await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            false
        }
    }

    /**
     * Updates the last active timestamp for a user
     * @param userId User ID (defaults to current user)
     * @return Result indicating success or failure
     */
    suspend fun updateLastActive(userId: String? = null): Result<Unit> {
        val targetUserId = userId ?: auth.currentUser?.uid
        ?: return Result.failure(Exception("User not authenticated"))

        return try {
            usersCollection.document(targetUserId)
                .update("lastActiveAt", FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last active", e)
            Result.failure(Exception("Failed to update activity: ${e.message}", e))
        }
    }

    // ==================== DELETE ====================

    /**
     * Soft deletes a user (sets account status to deleted)
     * @param userId User ID to delete
     * @return True if successful, false otherwise
     */
    suspend fun deleteUser(userId: String): Boolean {
        if (userId.isBlank()) return false

        return try {
            val updates = mapOf(
                "accountStatus" to "deleted",
                "isBanned" to true,
                "email" to FieldValue.delete(),
                "phone" to FieldValue.delete(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            usersCollection.document(userId).update(updates).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            false
        }
    }

    // ==================== SEARCH ====================

    /**
     * Searches for users by username (case-insensitive)
     * @param query Search query
     * @param limit Maximum number of results (default 20)
     * @return List of matching users
     */
    suspend fun searchUsers(query: String, limit: Int = 20): List<User> {
        if (query.length < 2) return emptyList()
        val lowerQuery = query.lowercase(Locale.ROOT)

        return try {
            usersCollection
                .whereGreaterThanOrEqualTo("username", lowerQuery)
                .whereLessThanOrEqualTo("username", lowerQuery + "\uf8ff")
                .orderBy("username")
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            emptyList()
        }
    }

    // ==================== FOLLOW SYSTEM ====================

    /**
     * Follows a user
     * @param currentUserId Current user's ID
     * @param targetUserId Target user's ID to follow
     * @return True if successful, false otherwise
     */
    suspend fun followUser(currentUserId: String, targetUserId: String): Boolean {
        if (currentUserId == targetUserId) return false

        return try {
            val targetFollowersRef = firestore.collection(FirebasePath.USERS)
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)

            val myFollowingRef = firestore.collection(FirebasePath.USERS)
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)

            // Check if already following
            val alreadyFollowing = myFollowingRef.get().await().exists()
            if (alreadyFollowing) return true

            firestore.runTransaction { transaction ->
                // Add to target's followers
                transaction.set(
                    targetFollowersRef,
                    mapOf("timestamp" to FieldValue.serverTimestamp())
                )

                // Add to my following
                transaction.set(
                    myFollowingRef,
                    mapOf("timestamp" to FieldValue.serverTimestamp())
                )

                // Increment target's follower count
                transaction.update(
                    usersCollection.document(targetUserId),
                    "followerCount",
                    FieldValue.increment(1)
                )

                // Increment my following count
                transaction.update(
                    usersCollection.document(currentUserId),
                    "followingCount",
                    FieldValue.increment(1)
                )
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error following user", e)
            false
        }
    }

    /**
     * Unfollows a user
     * @param currentUserId Current user's ID
     * @param targetUserId Target user's ID to unfollow
     * @return True if successful, false otherwise
     */
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Boolean {
        if (currentUserId == targetUserId) return false

        return try {
            val targetFollowersRef = firestore.collection(FirebasePath.USERS)
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)

            val myFollowingRef = firestore.collection(FirebasePath.USERS)
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)

            firestore.runTransaction { transaction ->
                transaction.delete(targetFollowersRef)
                transaction.delete(myFollowingRef)

                // Decrement counts
                transaction.update(
                    usersCollection.document(targetUserId),
                    "followerCount",
                    FieldValue.increment(-1)
                )

                transaction.update(
                    usersCollection.document(currentUserId),
                    "followingCount",
                    FieldValue.increment(-1)
                )
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unfollowing user", e)
            false
        }
    }

    /**
     * Checks if current user is following target user
     * @param currentUserId Current user's ID
     * @param targetUserId Target user's ID
     * @return True if following, false otherwise
     */
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val snapshot = firestore.collection(FirebasePath.USERS)
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .get()
                .await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking follow status", e)
            false
        }
    }

    /**
     * Gets the list of users who follow the specified user
     * @param userId User ID to get followers for
     * @param limit Maximum number of results (default 50)
     * @return List of follower users
     */
    suspend fun getFollowers(userId: String, limit: Int = 50): List<User> =
        withContext(Dispatchers.IO) {
            try {
                val followersSnapshot = firestore.collection(FirebasePath.USERS)
                    .document(userId)
                    .collection("followers")
                    .limit(limit.toLong())
                    .get()
                    .await()

                val followerIds = followersSnapshot.documents.map { it.id }
                if (followerIds.isEmpty()) return@withContext emptyList()

                // Split into chunks of 10 (Firestore 'whereIn' limit)
                val chunks = followerIds.chunked(10)

                // Fetch all chunks in parallel
                val deferredUsers = chunks.map { chunk ->
                    async {
                        usersCollection
                            .whereIn("id", chunk)
                            .get()
                            .await()
                            .toObjects(User::class.java)
                    }
                }

                deferredUsers.awaitAll().flatten()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting followers", e)
                emptyList()
            }
        }

    /**
     * Gets the list of users that the specified user follows
     * @param userId User ID to get following for
     * @param limit Maximum number of results (default 50)
     * @return List of users being followed
     */
    suspend fun getFollowing(userId: String, limit: Int = 50): List<User> =
        withContext(Dispatchers.IO) {
            try {
                val followingSnapshot = firestore.collection(FirebasePath.USERS)
                    .document(userId)
                    .collection("following")
                    .limit(limit.toLong())
                    .get()
                    .await()

                val followingIds = followingSnapshot.documents.map { it.id }
                if (followingIds.isEmpty()) return@withContext emptyList()

                // Split into chunks of 10
                val chunks = followingIds.chunked(10)

                val deferredUsers = chunks.map { chunk ->
                    async {
                        usersCollection
                            .whereIn("id", chunk)
                            .get()
                            .await()
                            .toObjects(User::class.java)
                    }
                }

                deferredUsers.awaitAll().flatten()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting following", e)
                emptyList()
            }
        }

    // ==================== HELPER ====================

    /**
     * Generates a unique username from display name
     * Format: cleanedname1234 (12 chars max + 4 digit random)
     */
    private fun generateUniqueUsername(displayName: String): String {
        val sanitized = displayName
            .lowercase()
            .trim()
            .replace(Regex("[^a-z0-9_]"), "")
            .take(12)
        val randomSuffix = (1000..9999).random()
        return "$sanitized$randomSuffix"
    }
}