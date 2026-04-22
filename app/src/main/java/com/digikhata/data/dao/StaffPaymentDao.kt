package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digikhata.data.entity.StaffPayment
import kotlinx.coroutines.flow.Flow

data class StaffPaidAgg(val staffId: Long, val amount: Double)

@Dao
interface StaffPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: StaffPayment): Long

    @Delete
    suspend fun delete(p: StaffPayment)

    @Query("SELECT * FROM staff_payments WHERE staffId = :sid ORDER BY paymentDate DESC, id DESC")
    fun getByStaff(sid: Long): Flow<List<StaffPayment>>

    @Query("SELECT COALESCE(SUM(amount),0) FROM staff_payments WHERE staffId = :sid AND paymentDate BETWEEN :from AND :to")
    fun paidBetween(sid: Long, from: Long, to: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(sp.amount),0) FROM staff_payments sp INNER JOIN staff s ON sp.staffId = s.id WHERE s.businessId = :bid AND sp.paymentDate BETWEEN :from AND :to")
    fun paidThisMonthForBusiness(bid: Long, from: Long, to: Long): Flow<Double>

    @Query("SELECT sp.staffId AS staffId, COALESCE(SUM(sp.amount),0) AS amount FROM staff_payments sp INNER JOIN staff s ON sp.staffId = s.id WHERE s.businessId = :bid AND sp.paymentDate BETWEEN :from AND :to GROUP BY sp.staffId")
    fun paidByStaffInRange(bid: Long, from: Long, to: Long): Flow<List<StaffPaidAgg>>
}
