package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    foreignKeys = [ForeignKey(
        entity = Business::class,
        parentColumns = ["id"],
        childColumns = ["businessId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("businessId")]
)
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val type: Int, // 0 = customer, 1 = supplier
    val name: String,
    val phone: String? = null,
    val phone2: String? = null,
    val cnic: String? = null,
    val address: String? = null,
    val creditLimit: Double = 0.0,
    val rating: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val serverUpdatedAt: Long? = null
)
