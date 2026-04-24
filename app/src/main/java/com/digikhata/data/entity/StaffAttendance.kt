package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_attendance",
    foreignKeys = [
        ForeignKey(
            entity = Staff::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["staffId", "date"], unique = true),
        Index("staffId")
    ]
)
data class StaffAttendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val date: Long,
    val status: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val serverUpdatedAt: Long? = null
)
