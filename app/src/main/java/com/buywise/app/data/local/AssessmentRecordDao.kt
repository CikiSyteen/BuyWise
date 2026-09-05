package com.buywise.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentRecordDao {

    @Insert
    suspend fun insert(record: AssessmentRecordEntity): Long

    @Query("DELETE FROM assessment_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM assessment_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AssessmentRecordEntity>>

    @Query("SELECT * FROM assessment_records WHERE id = :id")
    suspend fun getById(id: Long): AssessmentRecordEntity?

    @Query("UPDATE assessment_records SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
