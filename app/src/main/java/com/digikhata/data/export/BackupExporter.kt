package com.digikhata.data.export

import android.content.Context
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.TxEntity
import com.digikhata.domain.repository.DigiRepository
import com.digikhata.util.CsvExporter
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Snapshots the active book's data and writes a ZIP of CSVs to cache.
 *
 * Returns the resulting file for sharing via FileProvider.
 */
class BackupExporter @Inject constructor(
    private val repo: DigiRepository
) {

    suspend fun export(context: Context, business: Business): File {
        val bid = business.id
        val customers = repo.clients(bid, 0).first()
        val suppliers = repo.clients(bid, 1).first()
        val allClients = customers + suppliers

        val transactions = ArrayList<TxEntity>()
        for (c in allClients) {
            transactions.addAll(repo.transactions(c.id).first())
        }

        val cash = repo.cashEntries(bid, 0L, Long.MAX_VALUE).first()
        val expenses = repo.expenses(bid, 0L, Long.MAX_VALUE).first()
        val invoices = repo.invoices(bid).first()

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = business.name.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val dir = File(context.cacheDir, "backups").also { it.mkdirs() }
        val file = File(dir, "digikhata_${safeName}_$stamp.zip")

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            writeEntry(zip, "clients.csv", CsvExporter.clientsCsv(allClients))
            writeEntry(zip, "transactions.csv", CsvExporter.transactionsCsv(transactions))
            writeEntry(zip, "cash.csv", CsvExporter.cashCsv(cash))
            writeEntry(zip, "expenses.csv", CsvExporter.expensesCsv(expenses))
            writeEntry(zip, "invoices.csv", CsvExporter.invoicesCsv(invoices))
            writeEntry(zip, "README.txt", buildReadme(business, stamp, allClients.size, transactions.size, cash.size, expenses.size, invoices.size))
        }
        return file
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildReadme(
        biz: Business,
        stamp: String,
        clients: Int,
        txs: Int,
        cash: Int,
        expenses: Int,
        invoices: Int
    ): String = """
        DigiKhata export
        Business: ${biz.name}
        Exported: $stamp

        Files:
          clients.csv       ($clients rows)
          transactions.csv  ($txs rows)
          cash.csv          ($cash rows)
          expenses.csv      ($expenses rows)
          invoices.csv      ($invoices rows)

        Dates are Unix milliseconds (UTC).
        Transaction type: gave = you gave, got = you got.
        Cash type: out / in.
    """.trimIndent()
}
