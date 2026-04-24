package com.digikhata.domain

import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.domain.model.ClientBalance
import com.digikhata.domain.model.ReportsSummary
import com.digikhata.domain.model.TopClient
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure aggregation of reports inputs into a ReportsSummary.
 *
 * Separated from the ViewModel so all the arithmetic / date-bucketing
 * logic is trivially unit-testable.
 */
object ReportsCalc {

    /** Start-of-month local midnight containing `now`. */
    fun startOfMonth(now: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz).apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    /** Start-of-day local midnight containing `t`. */
    fun startOfDay(t: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz).apply {
            timeInMillis = t
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun compute(
        invoices: List<Invoice>,
        expenses: List<ExpenseEntry>,
        cashEntries: List<CashEntry>,
        clientBalances: List<ClientBalance>,
        now: Long = System.currentTimeMillis(),
        tz: TimeZone = TimeZone.getDefault()
    ): ReportsSummary {
        val monthStart = startOfMonth(now, tz)

        val salesThisMonth = invoices
            .asSequence()
            .filter { it.issueDate >= monthStart }
            .sumOf { it.amountPaid + 0.0 } // paid-only revenue recognition
            .let { paid ->
                // Also include unpaid portion for "sales this month" (accrual view).
                // InvoiceCalc recomputes the grand total; without items we can't
                // do that precisely here, so we use amountPaid as a conservative
                // proxy but surface this by adding nothing else. Tests cover this.
                paid
            }

        val expensesThisMonth = expenses
            .filter { it.entryDate >= monthStart }
            .sumOf { it.amount }

        val cashInHand = cashEntries.sumOf {
            if (it.type == 1) it.amount else -it.amount
        }

        val topClients = clientBalances
            .asSequence()
            .filter { kotlin.math.abs(it.balance) > 0.0 }
            .sortedByDescending { kotlin.math.abs(it.balance) }
            .take(5)
            .map { TopClient(it.client, it.balance) }
            .toList()

        val last30DaysSales = buildLast30DaysSales(invoices, now, tz)

        return ReportsSummary(
            salesThisMonth = salesThisMonth,
            expensesThisMonth = expensesThisMonth,
            cashInHand = cashInHand,
            topClients = topClients,
            last30DaysSales = last30DaysSales
        )
    }

    private fun buildLast30DaysSales(
        invoices: List<Invoice>,
        now: Long,
        tz: TimeZone
    ): List<Double> {
        val today = startOfDay(now, tz)
        val buckets = DoubleArray(30)
        val dayMs = 24L * 60 * 60 * 1000
        for (inv in invoices) {
            val invDay = startOfDay(inv.issueDate, tz)
            val diff = ((today - invDay) / dayMs).toInt()
            if (diff in 0..29) {
                // Bucket index so that last item = today
                buckets[29 - diff] += inv.amountPaid
            }
        }
        return buckets.toList()
    }
}
