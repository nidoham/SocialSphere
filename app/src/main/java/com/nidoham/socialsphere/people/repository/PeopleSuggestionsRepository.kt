package com.nidoham.socialsphere.people.repository

import com.nidoham.socialsphere.auth.model.Account

interface PeopleSuggestionsRepository {
    suspend fun getRandomPeople(
        currentUid: String,
        limit: Int = 50
    ): List<Pair<String, Account>>
}