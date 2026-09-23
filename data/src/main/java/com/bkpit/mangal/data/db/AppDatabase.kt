package com.bkpit.mangal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bkpit.mangal.data.db.dao.ChatDao
import com.bkpit.mangal.data.db.dao.SettingsDao
import com.bkpit.mangal.data.db.entities.ChatMessageEntity
import com.bkpit.mangal.data.db.entities.SettingsEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [ChatMessageEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase {
            SQLiteDatabase.loadLibs(context) // one-time native SQLCipher init
            val passphrase = DatabasePassphrase.getOrCreate(context)
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mangal.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // acceptable pre-1.0; revisit before release
                .build()
        }
    }
}
