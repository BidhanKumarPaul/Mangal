package com.bkpit.mangal.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bkpit.mangal.data.db.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun getOnce(): SettingsEntity?

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}
