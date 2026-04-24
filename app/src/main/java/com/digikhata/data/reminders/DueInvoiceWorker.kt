package com.digikhata.data.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digikhata.ActiveBookHolder
import com.digikhata.domain.repository.DigiRepository
import com.digikhata.util.InvoiceCalc
import com.digikhata.util.NotificationChannels
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Phase 4a.1: scans the active book for overdue / due-soon invoices once per run
 * and posts system notifications.
 *
 * Intentionally silent on empty results — no "you have 0 overdue invoices" spam.
 */
@HiltWorker
class DueInvoiceWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val bid = active.id.value ?: return Result.success()

        val invoices = repo.invoices(bid).first()
        if (invoices.isEmpty()) return Result.success()

        // Build candidates by joining invoice → items (for grand total) → client name.
        val candidates = invoices.mapNotNull { inv ->
            val items = repo.invoiceItems(inv.id).first()
            if (items.isEmpty()) return@mapNotNull null
            val totals = InvoiceCalc.compute(inv, items)
            val client = repo.getClient(inv.customerId).first()
            DueInvoiceReminders.DueCandidate(
                invoiceId = inv.id,
                sequenceNumber = inv.sequenceNumber,
                customerId = inv.customerId,
                customerName = client?.name ?: "Customer",
                dueDate = inv.dueDate,
                amountPaid = inv.amountPaid,
                grandTotal = totals.grandTotal
            )
        }

        val hits = DueInvoiceReminders.pickDueInvoices(candidates, System.currentTimeMillis())
        if (hits.isEmpty()) return Result.success()

        NotificationChannels.ensureCreated(ctx)
        if (!hasPostPermission()) return Result.success()

        val nm = NotificationManagerCompat.from(ctx)
        for (hit in hits) {
            nm.notify(notificationId(hit.candidate.invoiceId), buildNotification(hit))
        }
        return Result.success()
    }

    private fun buildNotification(hit: DueInvoiceReminders.DueHit): android.app.Notification {
        val c = hit.candidate
        val title = when (hit.status) {
            DueInvoiceReminders.DueStatus.OVERDUE ->
                "Invoice overdue (${hit.days}d): ${c.customerName}"
            DueInvoiceReminders.DueStatus.DUE_SOON ->
                if (hit.days == 0) "Invoice due today: ${c.customerName}"
                else "Invoice due in ${hit.days}d: ${c.customerName}"
        }
        val body = "#${InvoiceCalc.displayNumber("INV-", c.sequenceNumber)} • " +
                "Balance ${"%.2f".format(c.grandTotal - c.amountPaid)}"

        // Deep-link into the invoice detail. The launcher activity handles the nav.
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_INVOICE_ID, c.invoiceId)
        }
        val pi = launch?.let {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            PendingIntent.getActivity(ctx, c.invoiceId.toInt(), it, flags)
        }

        return NotificationCompat.Builder(ctx, NotificationChannels.DUE_INVOICES_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .apply { if (pi != null) setContentIntent(pi) }
            .build()
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx,
            "android.permission.POST_NOTIFICATIONS"
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationId(invoiceId: Long): Int =
        (invoiceId and 0x7FFFFFFF).toInt()

    companion object {
        const val EXTRA_INVOICE_ID = "extra_invoice_id"
    }
}
