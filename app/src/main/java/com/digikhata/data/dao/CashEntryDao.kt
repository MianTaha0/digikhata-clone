package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.CashEntry
import com.digikhata.domain.model.CashTotals
import kotlinx.coroutines.flow.Flow

@Dao
interface CashEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CashEntry): Long

    @Update
    suspend fun update(entry: CashEntry)

    @Delete
    suspend fun delete(entry: CashEntry)

    @Query("SELECT * FROM cash_entries WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Long): Flow<CashEntry?>

    @Query("SELECT * FROM cash_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to AND deletedAt IS NULL ORDER BY entryDate DESC, id DESC")
    fun getInRange(bid: Long, from: Long, to: Long): Flow<List<CashEntry>>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type=1 THEN amount ELSE 0 END),0) as totalIn, " +
                "COALESCE(SUM(CASE WHEN type=0 THEN amount ELSE 0 END),0) as totalOut " +
                "FROM cash_entries WHERE businessId = :bid AND entryDate BETWEEN :from AND :to AND deletedAt IS NULL"
    )
    fun totalsForPeriod(bid: Long, from: Long, to: Long): Flow<CashTotals>
}
