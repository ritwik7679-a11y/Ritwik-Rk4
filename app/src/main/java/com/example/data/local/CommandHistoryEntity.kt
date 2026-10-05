package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commandText: String,
    val responseText: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasSuccessful: Boolean = true
)

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCommandsFlow(limit: Int = 30): Flow<List<CommandHistoryEntity>>

    @Insert
    suspend fun insertCommand(command: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}
