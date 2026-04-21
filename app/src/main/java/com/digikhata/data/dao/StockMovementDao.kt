package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digikhata.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(m: StockMovement): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :pid ORDER BY createdAt DESC, id DESC")
    fun getByProduct(pid: Long): Flow<List<StockMovement>>
}
