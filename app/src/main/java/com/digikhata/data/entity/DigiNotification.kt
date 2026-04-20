package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class DigiNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val clientPhone: String,
    val amount: Double,
    val balance: Double,
    val currency: String,
    val details: String? = null,
    val ledgerLink: String? = null,
    val isSeen: Boolean = false,
    val isPost: Boolean = false,
    val type: Int = 0,
    val entryDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
