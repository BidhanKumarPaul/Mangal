package com.bkpit.mangal.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,           // "user" | "assistant" | "tool_result"
    val content: String,
    val timestampMillis: Long,
    val toolNameIfAny: String? = null
)
