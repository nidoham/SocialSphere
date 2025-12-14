package com.nidoham.socialsphere.stories.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nidoham.socialsphere.stories.model.Story
import kotlinx.coroutines.tasks.await

class StoriesHelper(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val storiesCollection = firestore.collection("stories")

    /**
     * Upload a new story
     */
    suspend fun uploadStory(story: Story): Result<Unit> {
        return try {
            val storyId = story.storyId.ifEmpty {
                storiesCollection.document().id
            }

            val finalStory = story.copy(
                storyId = storyId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            storiesCollection
                .document(storyId)
                .set(finalStory)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch top 30 stories from last 24 hours
     */
    suspend fun fetchRecentStories(): Result<List<Story>> {
        return try {
            val twentyFourHoursAgo =
                System.currentTimeMillis() - (24 * 60 * 60 * 1000)

            val snapshot = storiesCollection
                .whereGreaterThanOrEqualTo("createdAt", twentyFourHoursAgo)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()

            val stories = snapshot.toObjects(Story::class.java)

            Result.success(stories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}