package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.ExpenseEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExpenseEntry): Long

    @Update
    suspend fun update(entry: ExpenseEntry)

    @Delete
    suspend fun delete(entry: ExpenseEntry)

    @Query("SELECT * FROM expense_entries WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Long): Flow<ExpenseEntry?>

    @Query("SELECT * FROM expense_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to AND deletedAt IS NULL ORDER BY entryDate DESC, id DESC")
    fun getInRange(bid: Long, from: Long, to: Long): Flow<List<ExpenseEntry>>

    @Query("SELECT COALESCE(SUM(amount),0) FROM expense_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to AND deletedAt IS NULL")
    fun totalForPeriod(bid: Long, from: Long, to: Long): Flow<Double>
}
