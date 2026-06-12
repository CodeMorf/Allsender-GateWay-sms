package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SMSLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "INBOUND" or "OUTBOUND"
    val status: String, // "PENDING", "SENT", "FAILED"
    val gatewayMode: String, // "LOCAL_SERVER", "POLLING_CLIENT", "MANUAL"
    val errorMessage: String? = null
)
