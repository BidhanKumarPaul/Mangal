package com.bkpit.mangal.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table — id is always 0. Simpler than SharedPreferences to keep
 *  everything queryable/observable via Room's Flow support in one place. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val activeLlmModelFileName: String? = null,
    val activeWhisperModelFileName: String? = null,
    val ttsSpeechRate: Float = 1.0f,
    val maxContextLength: Int = 2048,
    val wakeWordEnabled: Boolean = false
)
