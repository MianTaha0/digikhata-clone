package com.digikhata.di

import android.content.Context
import androidx.room.Room
import com.digikhata.data.DigiDatabase
import com.digikhata.data.dao.BusinessDao
import com.digikhata.data.dao.ClientDao
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBusinessDao(db: DigiDatabase): BusinessDao = db.businessDao()
    @Provides fun provideClientDao(db: DigiDatabase): ClientDao = db.clientDao()
    @Provides fun provideTransactionDao(db: DigiDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideTransactionImageDao(db: DigiDatabase): TransactionImageDao = db.transactionImageDao()
    @Provides fun provideNotificationDao(db: DigiDatabase): NotificationDao = db.notificationDao()
}
