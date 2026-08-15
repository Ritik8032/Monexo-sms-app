package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSms(): Flow<List<SmsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSms(sms: SmsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(smsList: List<SmsEntity>)

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteSmsById(id: Long)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM sms_logs")
    suspend fun getCount(): Int
}
