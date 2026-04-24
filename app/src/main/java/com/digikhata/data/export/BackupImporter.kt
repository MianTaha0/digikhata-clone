package com.digikhata.data.export

import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.TxEntity
import com.digikhata.domain.repository.DigiRepository
import com.digikhata.util.CsvParser
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * Result of an import pass. Negative [clientsImported] (etc) means the section was absent.
 */
data class ImportResult(
    val clientsImported: Int,
    val transactionsImported: Int,
    val cashImported: Int,
    val expensesImported: Int,
    val errors: List<String>
) {
    val total: Int get() = clientsImported + transactionsImported + cashImported + expensesImported
}

/**
 * Reads a ZIP produced by [BackupExporter] and upserts rows into the active book.
 *
 * Strategy:
 *  - Always insert as NEW rows (autoGenerate IDs). Never overwrite existing data.
 *  - Remap old client IDs → new client IDs so referring rows stay consistent.
 *  - Skip invoices for now — their items CSV isn't exported yet; a half-import would
 *    produce invoices with no line items. Leave to a later slice.
 */
class BackupImporter @Inject constructor(
    private val repo: DigiRepository
) {

    suspend fun importZip(input: InputStream, businessId: Long): ImportResult {
        val contents = readZipEntries(input)
        val errors = ArrayList<String>()

        val clientsCsv = contents["clients.csv"]
        val txCsv = contents["transactions.csv"]
        val cashCsv = contents["cash.csv"]
        val expCsv = contents["expenses.csv"]

        val idMap = HashMap<Long, Long>()
        var clientsImported = 0
        var txImported = 0
        var cashImported = 0
        var expensesImported = 0

        if (clientsCsv != null) {
            for (row in CsvParser.parseAsMaps(clientsCsv)) {
                try {
                    val oldId = row["id"]?.toLongOrNull() ?: continue
                    val type = when (row["type"]) { "supplier" -> 1; else -> 0 }
                    val client = Client(
                        businessId = businessId,
                        type = type,
                        name = row["name"].orEmpty().ifBlank { "Unnamed" },
                        phone = row["phone"].nullIfBlank(),
                        phone2 = row["phone2"].nullIfBlank(),
                        cnic = row["cnic"].nullIfBlank(),
                        address = row["address"].nullIfBlank(),
                        creditLimit = row["creditLimit"]?.toDoubleOrNull() ?: 0.0,
                        rating = row["rating"]?.toIntOrNull() ?: 0,
                        isPinned = row["isPinned"].toBool(),
                        isArchived = row["isArchived"].toBool()
                    )
                    val newId = repo.upsertClient(client)
                    idMap[oldId] = newId
                    clientsImported++
                } catch (t: Throwable) {
                    errors.add("clients.csv row: ${t.message}")
                }
            }
        }

        if (txCsv != null) {
            for (row in CsvParser.parseAsMaps(txCsv)) {
                try {
                    val oldClientId = row["clientId"]?.toLongOrNull() ?: continue
                    val newClientId = idMap[oldClientId] ?: continue
                    val tx = TxEntity(
                        clientId = newClientId,
                        businessId = businessId,
                        amount = row["amount"]?.toDoubleOrNull() ?: continue,
                        type = if (row["type"] == "got") 1 else 0,
                        notes = row["notes"].nullIfBlank(),
                        entryDate = row["entryDate"]?.toLongOrNull() ?: System.currentTimeMillis()
                    )
                    repo.addTransaction(tx, emptyList())
                    txImported++
                } catch (t: Throwable) {
                    errors.add("transactions.csv row: ${t.message}")
                }
            }
        }

        if (cashCsv != null) {
            for (row in CsvParser.parseAsMaps(cashCsv)) {
                try {
                    val entry = CashEntry(
                        businessId = businessId,
                        amount = row["amount"]?.toDoubleOrNull() ?: continue,
                        type = if (row["type"] == "in") 1 else 0,
                        category = row["category"].orEmpty().ifBlank { "Other" },
                        note = row["note"].nullIfBlank(),
                        entryDate = row["entryDate"]?.toLongOrNull() ?: System.currentTimeMillis()
                    )
                    repo.addCashEntry(entry, null)
                    cashImported++
                } catch (t: Throwable) {
                    errors.add("cash.csv row: ${t.message}")
                }
            }
        }

        if (expCsv != null) {
            for (row in CsvParser.parseAsMaps(expCsv)) {
                try {
                    val entry = ExpenseEntry(
                        businessId = businessId,
                        amount = row["amount"]?.toDoubleOrNull() ?: continue,
                        category = row["category"].orEmpty().ifBlank { "Other" },
                        paymentMethod = row["paymentMethod"].orEmpty().ifBlank { "Cash" },
                        note = row["note"].nullIfBlank(),
                        entryDate = row["entryDate"]?.toLongOrNull() ?: System.currentTimeMillis()
                    )
                    repo.addExpense(entry, null)
                    expensesImported++
                } catch (t: Throwable) {
                    errors.add("expenses.csv row: ${t.message}")
                }
            }
        }

        return ImportResult(clientsImported, txImported, cashImported, expensesImported, errors)
    }

    private fun readZipEntries(input: InputStream): Map<String, String> {
        val out = HashMap<String, String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    out[entry.name] = String(bytes, Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        return out
    }

    private fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }
    private fun String?.toBool(): Boolean = this.equals("true", ignoreCase = true)
}
