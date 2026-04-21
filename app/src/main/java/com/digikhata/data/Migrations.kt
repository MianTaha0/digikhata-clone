package com.digikhata.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `invoices` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`businessId` INTEGER NOT NULL, " +
                    "`customerId` INTEGER NOT NULL, " +
                    "`sequenceNumber` INTEGER NOT NULL, " +
                    "`issueDate` INTEGER NOT NULL, " +
                    "`dueDate` INTEGER, " +
                    "`notes` TEXT, " +
                    "`discountValue` REAL NOT NULL, " +
                    "`discountIsPercent` INTEGER NOT NULL, " +
                    "`amountPaid` REAL NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`customerId`) REFERENCES `clients`(`id`) ON DELETE RESTRICT)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_businessId` ON `invoices` (`businessId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_customerId` ON `invoices` (`customerId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `invoice_items` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`invoiceId` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`quantity` REAL NOT NULL, " +
                    "`unitPrice` REAL NOT NULL, " +
                    "`taxPercent` REAL NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoiceId` ON `invoice_items` (`invoiceId`)")

        db.execSQL("ALTER TABLE `businesses` ADD COLUMN `invoicePrefix` TEXT NOT NULL DEFAULT 'INV-'")
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expense_entries` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`businessId` INTEGER NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`paymentMethod` TEXT NOT NULL, " +
                    "`note` TEXT, " +
                    "`entryDate` INTEGER NOT NULL, " +
                    "`imageLocalPath` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_entries_businessId` ON `expense_entries` (`businessId`)")
    }
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_entries` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`businessId` INTEGER NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`type` INTEGER NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`note` TEXT, " +
                    "`entryDate` INTEGER NOT NULL, " +
                    "`imageLocalPath` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_entries_businessId` ON `cash_entries` (`businessId`)")
    }
}
