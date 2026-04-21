package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerName: String? = null,
    val phone: String? = null,
    val currency: String = "Pakistan Rupee-Rs",
    val colorHex: String = "#E74425",
    val address: String? = null,
    val city: String? = null,
    val type: String? = null,
    val category: String? = null,
    val tagline: String? = null,
    val logoLocalPath: String? = null,
    val invoicePrefix: String = "INV-",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
