package com.bkpit.mangal.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bkpit.mangal.data.db.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMessageEntity>

    /** Phase 5: "conversation memory ... user-clearable". */
    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
