package com.digikhata.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tables = listOf(
            "businesses",
            "clients",
            "transactions",
            "cash_entries",
            "expense_entries",
            "invoices",
            "invoice_items",
            "products",
            "stock_movements",
            "staff",
            "staff_payments",
            "staff_attendance"
        )
        for (t in tables) {
            db.execSQL("ALTER TABLE `$t` ADD COLUMN `deletedAt` INTEGER")
            db.execSQL("ALTER TABLE `$t` ADD COLUMN `serverUpdatedAt` INTEGER")
        }
        // invoice_items, stock_movements, and staff_payments previously lacked updatedAt.
        db.execSQL("ALTER TABLE `invoice_items` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `stock_movements` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `staff_payments` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_ops` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `businessId` INTEGER,
                `collection` TEXT NOT NULL,
                `docId` TEXT NOT NULL,
                `opType` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL,
                `lastError` TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_ops_createdAt` ON `sync_ops` (`createdAt`)")
    }
}

val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `staff_attendance` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `staffId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`staffId`) REFERENCES `staff`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_staff_attendance_staffId_date` ON `staff_attendance` (`staffId`, `date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_staff_attendance_staffId` ON `staff_attendance` (`staffId`)")
    }
}

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
