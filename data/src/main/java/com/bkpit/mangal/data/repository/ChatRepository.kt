package com.bkpit.mangal.data.repository

import com.bkpit.mangal.data.db.AppDatabase
import com.bkpit.mangal.data.db.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val db: AppDatabase) {
    fun observeMessages(): Flow<List<ChatMessageEntity>> = db.chatDao().observeAll()

    suspend fun recent(limit: Int = 20): List<ChatMessageEntity> = db.chatDao().recent(limit)

    suspend fun addMessage(role: String, content: String, toolName: String? = null) {
        db.chatDao().insert(
            ChatMessageEntity(
                role = role,
                content = content,
                timestampMillis = System.currentTimeMillis(),
                toolNameIfAny = toolName
            )
        )
    }

    /** Phase 5: user-clearable conversation memory. */
    suspend fun clearHistory() = db.chatDao().clearAll()
}
