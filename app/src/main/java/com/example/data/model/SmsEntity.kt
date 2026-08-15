package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val body: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Personal",
    val isRead: Boolean = true
)
