package com.digikhata.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.digikhata.data.dao.BusinessDao
import com.digikhata.data.dao.ClientDao
import com.digikhata.data.dao.NotificationDao
import com.digikhata.data.dao.TransactionDao
import com.digikhata.data.dao.TransactionImageDao
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.DigiNotification
import com.digikhata.data.entity.TransactionImage
import com.digikhata.data.entity.TxEntity

@Database(
    entities = [
        Business::class,
        Client::class,
        TxEntity::class,
        TransactionImage::class,
        DigiNotification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DigiDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun clientDao(): ClientDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionImageDao(): TransactionImageDao
    abstract fun notificationDao(): NotificationDao
}
