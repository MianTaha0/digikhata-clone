package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: Product): Long

    @Update
    suspend fun update(p: Product)

    @Delete
    suspend fun delete(p: Product)

    @Query("SELECT * FROM products WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE businessId = :bid AND deletedAt IS NULL ORDER BY name COLLATE NOCASE ASC")
    fun getByBusiness(bid: Long): Flow<List<Product>>

    @Query("SELECT COALESCE(SUM(quantity * costPrice), 0) FROM products WHERE businessId = :bid AND deletedAt IS NULL")
    fun totalValue(bid: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :bid AND deletedAt IS NULL AND lowStockThreshold > 0 AND quantity <= lowStockThreshold")
    fun lowStockCount(bid: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :bid AND deletedAt IS NULL")
    fun itemCount(bid: Long): Flow<Int>
}
