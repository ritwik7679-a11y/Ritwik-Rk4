package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM incoming_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM incoming_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 10): List<MessageEntity>

    @Query("SELECT * FROM incoming_messages WHERE senderName LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun searchMessagesBySender(query: String, limit: Int = 5): List<MessageEntity>

    @Query("SELECT * FROM incoming_messages WHERE isReadAloud = 0 ORDER BY timestamp DESC")
    suspend fun getUnreadMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE incoming_messages SET isReadAloud = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE incoming_messages SET isReadAloud = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM incoming_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM incoming_messages")
    suspend fun clearAll()
}
