package com.digikhata.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.digikhata.data.dao.BusinessDao
import com.digikhata.data.dao.CashEntryDao
import com.digikhata.data.dao.ClientDao
import com.digikhata.data.dao.ExpenseEntryDao
import com.digikhata.data.dao.InvoiceDao
import com.digikhata.data.dao.InvoiceItemDao
import com.digikhata.data.dao.NotificationDao
import com.digikhata.data.dao.ProductDao
import com.digikhata.data.dao.StaffDao
import com.digikhata.data.dao.StaffPaymentDao
import com.digikhata.data.dao.StockMovementDao
import com.digikhata.data.dao.TransactionDao
import com.digikhata.data.dao.TransactionImageDao
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.DigiNotification
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.data.entity.Product
import com.digikhata.data.entity.Staff
import com.digikhata.data.entity.StaffPayment
import com.digikhata.data.entity.StockMovement
import com.digikhata.data.entity.TransactionImage
import com.digikhata.data.entity.TxEntity

@Database(
    entities = [
        Business::class,
        Client::class,
        TxEntity::class,
        TransactionImage::class,
        DigiNotification::class,
        CashEntry::class,
        ExpenseEntry::class,
        Invoice::class,
        InvoiceItem::class,
        Product::class,
        StockMovement::class,
        Staff::class,
        StaffPayment::class
    ],
    version = 6,
    exportSchema = false
)
abstract class DigiDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun clientDao(): ClientDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionImageDao(): TransactionImageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun cashEntryDao(): CashEntryDao
    abstract fun expenseEntryDao(): ExpenseEntryDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun staffDao(): StaffDao
    abstract fun staffPaymentDao(): StaffPaymentDao
}
