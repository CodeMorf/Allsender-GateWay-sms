package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SMSLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSMSLogs(): Flow<List<SMSLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSMSLog(smsLog: SMSLog): Long

    @Update
    suspend fun updateSMSLog(smsLog: SMSLog)

    @Query("UPDATE sms_logs SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String?)

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteSMSLogById(id: Long)

    @Query("DELETE FROM sms_logs")
    suspend fun deleteAllSMSLogs()

    @Query("SELECT COUNT(*) FROM sms_logs")
    fun getTotalSMSLogsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE status = :status")
    fun getSMSLogsCountByStatus(status: String): Flow<Int>
}
