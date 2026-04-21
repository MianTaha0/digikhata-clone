package com.digikhata.di

import android.content.Context
import androidx.room.Room
import com.digikhata.data.DigiDatabase
import com.digikhata.data.MIGRATION_1_2
import com.digikhata.data.MIGRATION_2_3
import com.digikhata.data.MIGRATION_3_4
import com.digikhata.data.dao.BusinessDao
import com.digikhata.data.dao.CashEntryDao
import com.digikhata.data.dao.ClientDao
import com.digikhata.data.dao.ExpenseEntryDao
import com.digikhata.data.dao.InvoiceDao
import com.digikhata.data.dao.InvoiceItemDao
import com.digikhata.data.dao.NotificationDao
import com.digikhata.data.dao.TransactionDao
import com.digikhata.data.dao.TransactionImageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DigiDatabase =
        Room.databaseBuilder(context, DigiDatabase::class.java, "digikhata.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBusinessDao(db: DigiDatabase): BusinessDao = db.businessDao()
    @Provides fun provideClientDao(db: DigiDatabase): ClientDao = db.clientDao()
    @Provides fun provideTransactionDao(db: DigiDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideTransactionImageDao(db: DigiDatabase): TransactionImageDao = db.transactionImageDao()
    @Provides fun provideNotificationDao(db: DigiDatabase): NotificationDao = db.notificationDao()
    @Provides fun provideCashEntryDao(db: DigiDatabase): CashEntryDao = db.cashEntryDao()
    @Provides fun provideExpenseEntryDao(db: DigiDatabase): ExpenseEntryDao = db.expenseEntryDao()
    @Provides fun provideInvoiceDao(db: DigiDatabase): InvoiceDao = db.invoiceDao()
    @Provides fun provideInvoiceItemDao(db: DigiDatabase): InvoiceItemDao = db.invoiceItemDao()
}
