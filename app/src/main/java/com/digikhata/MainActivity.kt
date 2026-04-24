package com.digikhata

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.digikhata.data.reminders.DueInvoiceWorker
import com.digikhata.ui.navigation.DigiApp
import com.digikhata.ui.navigation.PendingNavIntent
import com.digikhata.ui.theme.DigiKhataTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        readPendingInvoiceId(intent)
        setContent {
            DigiKhataTheme {
                DigiApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readPendingInvoiceId(intent)
    }

    private fun readPendingInvoiceId(intent: Intent?) {
        val id = intent?.getLongExtra(DueInvoiceWorker.EXTRA_INVOICE_ID, -1L) ?: -1L
        if (id > 0L) PendingNavIntent.pendingInvoiceId.value = id
    }
}
