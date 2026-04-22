package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digikhata.data.entity.StaffAttendance
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffAttendanceDao {
    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(staffId: Long, from: Long, to: Long): Flow<List<StaffAttendance>>

    @Query("SELECT * FROM staff_attendance WHERE staffId = :staffId AND date = :date LIMIT 1")
    suspend fun find(staffId: Long, date: Long): StaffAttendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: StaffAttendance): Long

    @Query("DELETE FROM staff_attendance WHERE staffId = :staffId AND date = :date")
    suspend fun clear(staffId: Long, date: Long)
}
