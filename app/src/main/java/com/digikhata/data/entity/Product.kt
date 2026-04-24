package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Business::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId")]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val name: String,
    val sku: String? = null,
    val costPrice: Double,
    val sellPrice: Double,
    val quantity: Double,
    val lowStockThreshold: Double = 0.0,
    val unit: String = "pcs",
    val imageLocalPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val serverUpdatedAt: Long? = null
)
