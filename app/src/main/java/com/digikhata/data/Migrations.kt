package com.digikhata.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `staff` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`businessId` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`role` TEXT, " +
                    "`phone` TEXT, " +
                    "`monthlySalary` REAL NOT NULL, " +
                    "`joiningDate` INTEGER NOT NULL, " +
                    "`imageLocalPath` TEXT, " +
                    "`notes` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_businessId` ON `staff` (`businessId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `staff_payments` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`staffId` INTEGER NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`paymentDate` INTEGER NOT NULL, " +
                    "`note` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`staffId`) REFERENCES `staff`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_payments_staffId` ON `staff_payments` (`staffId`)")
    }
}

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`businessId` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`sku` TEXT, " +
                    "`costPrice` REAL NOT NULL, " +
                    "`sellPrice` REAL NOT NULL, " +
                    "`quantity` REAL NOT NULL, " +
                    "`lowStockThreshold` REAL NOT NULL, " +
                    "`unit` TEXT NOT NULL, " +
                    "`imageLocalPath` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_businessId` ON `products` (`businessId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `stock_movements` (" +
                    "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`productId` INTEGER NOT NULL, " +
                    "`delta` REAL NOT NULL, " +
                    "`reason` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)")
    }
}

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
