package com.nidoham.socialsphere.people.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.nidoham.socialsphere.auth.model.Account
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PeopleSuggestionsRepositoryImpl(
    private val firestore: FirebaseFirestore
) : PeopleSuggestionsRepository {

    override suspend fun getRandomPeople(
        currentUid: String,
        limit: Int
    ): List<Pair<String, Account>> {
        return try {
            val collectionRef = firestore.collection("users")

            // ১. একটি র‍্যান্ডম ID জেনারেট করি, এটি হবে আমাদের খোঁজার শুরুর পয়েন্ট
            val randomKey = UUID.randomUUID().toString()

            // ২. বর্তমান ইউজার বাদ পড়তে পারে, তাই ব্যাকআপ হিসেবে ৩-৪টি অতিরিক্ত ডাটা ফেচ করি
            val fetchLimit = (limit + 10).toLong() // আরো বেশি ফেচ করা হচ্ছে

            // ৩. র‍্যান্ডম ID-র সমান বা বড় ID গুলো খুঁজি (Efficient Random Query)
            var snapshot = collectionRef
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), randomKey)
                .limit(fetchLimit)
                .get()
                .await()

            // ৪. যদি র‍্যান্ডম পয়েন্টটি তালিকার শেষে হয় এবং পর্যাপ্ত ডাটা না পাওয়া যায়,
            // তাহলে তালিকার শুরু থেকে বাকি ডাটা আনি (Wrap Around)
            val documents = if (snapshot.size() < limit) {
                val remaining = fetchLimit - snapshot.size()
                val startSnapshot = collectionRef
                    .limit(remaining)
                    .get()
                    .await()

                // দুটি লিস্ট যোগ করি
                snapshot.documents + startSnapshot.documents
            } else {
                snapshot.documents
            }

            // ৫. ডেটা প্রসেস এবং ফিল্টার করা
            documents
                .mapNotNull { doc ->
                    try {
                        val account = doc.toObject<Account>()
                        if (account != null && doc.id.isNotEmpty()) {
                            doc.id to account
                        } else null
                    } catch (e: Exception) {
                        // যদি কোন ডকুমেন্ট পার্স করতে সমস্যা হয়, তাহলে স্কিপ করি
                        e.printStackTrace()
                        null
                    }
                }
                .filter { (uid, _) -> uid != currentUid } // বর্তমান ইউজারকে লিস্ট থেকে বাদ দেওয়া
                .distinctBy { it.first } // Duplicate UIDs রিমুভ করা
                .shuffled() // রেজাল্টগুলো একটু ওলট-পালট করে দেওয়া যাতে অর্ডারে না থাকে
                .take(limit) // এক্সাক্ট লিমিট রিটার্ন করা

        } catch (e: Exception) {
            // যদি কোন এরর হয়, খালি লিস্ট রিটার্ন করি
            e.printStackTrace()
            emptyList()
        }
    }
}