package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incoming_messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val senderNumber: String? = null,
    val sourceApp: String = "SMS", // "SMS", "WhatsApp", "Telegram", "Messages"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isReadAloud: Boolean = false
)
