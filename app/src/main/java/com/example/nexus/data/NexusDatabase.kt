package com.example.nexus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.nexus.data.dao.AuditDao
import com.example.nexus.data.dao.ConversationDao
import com.example.nexus.data.dao.MemoryDao
import com.example.nexus.data.dao.TaskDao
import com.example.nexus.data.entity.AuditLogEntity
import com.example.nexus.data.entity.ConversationEntity
import com.example.nexus.data.entity.MemoryItemEntity
import com.example.nexus.data.entity.MessageEntity
import com.example.nexus.data.entity.TaskEntity
import com.example.nexus.data.entity.TaskStepEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        TaskEntity::class,
        TaskStepEntity::class,
        MemoryItemEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun taskDao(): TaskDao
    abstract fun memoryDao(): MemoryDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getInstance(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_room.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
