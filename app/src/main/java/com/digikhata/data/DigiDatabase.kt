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
import com.digikhata.data.dao.TransactionDao
import com.digikhata.data.dao.TransactionImageDao
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.DigiNotification
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
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
        InvoiceItem::class
    ],
    version = 4,
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
}
