package com.aventaro.nandan

import android.content.Context
import androidx.room.*

@Entity(tableName = "memories")
data class MemoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val content: String, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "tasks")
data class TaskEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val details: String = "", val completed: Boolean = false, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "reminders")
data class ReminderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val note: String = "", val triggerAt: Long, val acknowledged: Boolean = false)

@Dao
interface NandanDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC") suspend fun memories(): List<MemoryEntity>
    @Insert suspend fun addMemory(item: MemoryEntity)
    @Query("DELETE FROM memories WHERE id = :id") suspend fun deleteMemory(id: Long)
    @Query("SELECT * FROM tasks ORDER BY completed ASC, createdAt DESC") suspend fun tasks(): List<TaskEntity>
    @Insert suspend fun addTask(item: TaskEntity)
    @Query("UPDATE tasks SET completed = :completed WHERE id = :id") suspend fun setTaskCompleted(id: Long, completed: Boolean)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)
    @Query("SELECT * FROM reminders ORDER BY triggerAt ASC") suspend fun reminders(): List<ReminderEntity>
    @Insert suspend fun addReminder(item: ReminderEntity): Long
    @Query("UPDATE reminders SET acknowledged = 1 WHERE id = :id") suspend fun acknowledgeReminder(id: Long)
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun deleteReminder(id: Long)
}

@Database(entities = [MemoryEntity::class, TaskEntity::class, ReminderEntity::class], version = 1, exportSchema = false)
abstract class NandanDatabase : RoomDatabase() {
    abstract fun dao(): NandanDao
    companion object {
        @Volatile private var INSTANCE: NandanDatabase? = null
        fun get(context: Context): NandanDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, NandanDatabase::class.java, "nandan-local.db").build().also { INSTANCE = it }
        }
    }
}
