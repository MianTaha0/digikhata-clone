package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.Staff
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(s: Staff): Long

    @Update
    suspend fun update(s: Staff)

    @Delete
    suspend fun delete(s: Staff)

    @Query("SELECT * FROM staff WHERE id = :id")
    fun getById(id: Long): Flow<Staff?>

    @Query("SELECT * FROM staff WHERE businessId = :bid ORDER BY name COLLATE NOCASE ASC")
    fun getByBusiness(bid: Long): Flow<List<Staff>>

    @Query("SELECT COUNT(*) FROM staff WHERE businessId = :bid")
    fun staffCount(bid: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(monthlySalary),0) FROM staff WHERE businessId = :bid")
    fun totalMonthlyPayroll(bid: Long): Flow<Double>
}
