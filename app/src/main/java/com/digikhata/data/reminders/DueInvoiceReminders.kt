package com.digikhata.data.reminders

/**
 * Pure logic for selecting invoices that deserve a "due soon" or "overdue" reminder.
 * Separated from WorkManager / Android so the rules stay trivially unit-testable.
 */
object DueInvoiceReminders {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    data class DueCandidate(
        val invoiceId: Long,
        val sequenceNumber: Int,
        val customerId: Long,
        val customerName: String,
        val dueDate: Long?,
        val amountPaid: Double,
        val grandTotal: Double
    )

    enum class DueStatus { OVERDUE, DUE_SOON }

    data class DueHit(
        val candidate: DueCandidate,
        val status: DueStatus,
        /** Whole days overdue (>=1) or until due (>=0). */
        val days: Int
    )

    /**
     * Returns unpaid invoices whose `dueDate` is either past [now] (overdue) or
     * falls within `[now, now + windowDays * 1 day]` (due soon).
     *
     * Skips: null due date, fully paid (amountPaid >= grandTotal - 0.001),
     * and invoices with a non-positive total (nothing billed).
     */
    fun pickDueInvoices(
        candidates: List<DueCandidate>,
        now: Long,
        windowDays: Int = 3
    ): List<DueHit> {
        val windowEnd = now + windowDays.coerceAtLeast(0) * DAY_MS
        val out = ArrayList<DueHit>()
        for (c in candidates) {
            val due = c.dueDate ?: continue
            if (c.grandTotal <= 0.0) continue
            if (c.amountPaid >= c.grandTotal - 0.001) continue
            when {
                due < now -> {
                    val days = ((now - due) / DAY_MS).toInt().coerceAtLeast(1)
                    out.add(DueHit(c, DueStatus.OVERDUE, days))
                }
                due <= windowEnd -> {
                    val days = ((due - now) / DAY_MS).toInt().coerceAtLeast(0)
                    out.add(DueHit(c, DueStatus.DUE_SOON, days))
                }
                // else: further in the future, skip
            }
        }
        // Most urgent first: overdue (longest overdue first), then due-soon (soonest first).
        return out.sortedWith(
            compareBy<DueHit> { if (it.status == DueStatus.OVERDUE) 0 else 1 }
                .thenByDescending { if (it.status == DueStatus.OVERDUE) it.days else 0 }
                .thenBy { if (it.status == DueStatus.DUE_SOON) it.days else 0 }
        )
    }
}
