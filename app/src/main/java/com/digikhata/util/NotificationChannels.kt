package com.digikhata.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Centralized channel registration. Safe to call multiple times.
 */
object NotificationChannels {

    const val DUE_INVOICES_ID = "due_invoices"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(DUE_INVOICES_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    DUE_INVOICES_ID,
                    "Invoice reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Overdue and soon-due invoice alerts."
                }
            )
        }
    }
}
