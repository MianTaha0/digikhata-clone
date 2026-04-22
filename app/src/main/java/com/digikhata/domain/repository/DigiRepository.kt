package com.digikhata.domain.repository

import com.digikhata.data.entity.Business
import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.DigiNotification
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.data.entity.Product
import com.digikhata.data.entity.Staff
import com.digikhata.data.entity.StaffAttendance
import com.digikhata.data.entity.StaffPayment
import com.digikhata.data.entity.StockMovement
import com.digikhata.data.entity.TxEntity
import com.digikhata.domain.model.CashTotals
import kotlinx.coroutines.flow.Flow

interface DigiRepository {
    val businesses: Flow<List<Business>>
    suspend fun upsertBusiness(business: Business): Long
    suspend fun deleteBusiness(business: Business)
    fun getBusiness(id: Long): Flow<Business?>

    fun clients(businessId: Long, type: Int): Flow<List<Client>>
    fun searchClients(businessId: Long, type: Int, query: String): Flow<List<Client>>
    suspend fun upsertClient(client: Client): Long
    suspend fun deleteClient(client: Client)
    fun getClient(id: Long): Flow<Client?>

    fun transactions(clientId: Long): Flow<List<TxEntity>>
    fun balanceForClient(clientId: Long): Flow<Double>
    suspend fun addTransaction(tx: TxEntity, images: List<String>): Long
    suspend fun updateTransaction(tx: TxEntity)
    suspend fun deleteTransaction(tx: TxEntity)

    val notifications: Flow<List<DigiNotification>>
    suspend fun addNotification(notification: DigiNotification): Long
    suspend fun markNotificationSeen(id: Long)

    fun cashEntries(businessId: Long, from: Long, to: Long): Flow<List<CashEntry>>
    fun cashTotals(businessId: Long, from: Long, to: Long): Flow<CashTotals>
    fun getCashEntry(id: Long): Flow<CashEntry?>
    suspend fun addCashEntry(entry: CashEntry, imagePath: String?): Long
    suspend fun updateCashEntry(entry: CashEntry)
    suspend fun deleteCashEntry(entry: CashEntry)

    fun expenses(businessId: Long, from: Long, to: Long): Flow<List<ExpenseEntry>>
    fun expenseTotal(businessId: Long, from: Long, to: Long): Flow<Double>
    fun getExpense(id: Long): Flow<ExpenseEntry?>
    suspend fun addExpense(entry: ExpenseEntry, imagePath: String?): Long
    suspend fun updateExpense(entry: ExpenseEntry)
    suspend fun deleteExpense(entry: ExpenseEntry)

    fun invoices(businessId: Long): Flow<List<Invoice>>
    fun getInvoice(id: Long): Flow<Invoice?>
    fun invoiceItems(invoiceId: Long): Flow<List<InvoiceItem>>
    fun recentItemNames(businessId: Long): Flow<List<String>>
    suspend fun nextInvoiceSequence(businessId: Long): Int
    suspend fun saveInvoice(inv: Invoice, items: List<InvoiceItem>): Long
    suspend fun recordPayment(invoiceId: Long, amount: Double)
    suspend fun deleteInvoice(inv: Invoice)

    fun products(businessId: Long): Flow<List<Product>>
    fun getProduct(id: Long): Flow<Product?>
    fun productMovements(productId: Long): Flow<List<StockMovement>>
    fun inventoryItemCount(businessId: Long): Flow<Int>
    fun inventoryTotalValue(businessId: Long): Flow<Double>
    fun lowStockCount(businessId: Long): Flow<Int>
    suspend fun addProduct(product: Product, imagePath: String?): Long
    suspend fun updateProduct(product: Product)
    suspend fun deleteProduct(product: Product)
    suspend fun adjustStock(productId: Long, delta: Double, reason: String?)

    fun staffList(businessId: Long): Flow<List<Staff>>
    fun getStaff(id: Long): Flow<Staff?>
    fun staffCount(businessId: Long): Flow<Int>
    fun totalMonthlyPayroll(businessId: Long): Flow<Double>
    fun paidThisMonthForBusiness(businessId: Long, from: Long, to: Long): Flow<Double>
    fun paidByStaffInRange(businessId: Long, from: Long, to: Long): Flow<Map<Long, Double>>
    suspend fun addStaff(staff: Staff, imagePath: String?): Long
    suspend fun updateStaff(staff: Staff)
    suspend fun deleteStaff(staff: Staff)

    fun staffPayments(staffId: Long): Flow<List<StaffPayment>>
    fun paidInRange(staffId: Long, from: Long, to: Long): Flow<Double>
    suspend fun addStaffPayment(payment: StaffPayment): Long
    suspend fun deleteStaffPayment(payment: StaffPayment)

    fun observeAttendance(staffId: Long, from: Long, to: Long): Flow<List<StaffAttendance>>
    suspend fun upsertAttendance(record: StaffAttendance): Long
    suspend fun clearAttendance(staffId: Long, date: Long)
}
