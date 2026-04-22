package com.digikhata.ui.invoice

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.digikhata.domain.model.InvoiceStatus
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.navigation.Routes
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import com.digikhata.util.InvoiceCalc
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    navController: NavController,
    vm: InvoiceDetailViewModel = hiltViewModel()
) {
    val inv by vm.invoice.collectAsState()
    val items by vm.items.collectAsState()
    val customer by vm.customer.collectAsState()
    val business by vm.business.collectAsState()
    val totals by vm.totals.collectAsState()
    val currency by vm.currency.collectAsState()
    val prefix by vm.prefix.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var showDelete by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        inv?.let { InvoiceCalc.displayNumber(prefix, it.sequenceNumber) }
                            ?: "Invoice"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { inv?.let { navController.navigate(Routes.invoiceEdit(it.id)) } },
                        enabled = inv != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDelete = true }, enabled = inv != null) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = digiTopBarColors()
            )
        },
        bottomBar = {
            val i = inv
            if (i != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (totals.status != InvoiceStatus.PAID) {
                        Button(
                            onClick = { showPayment = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DigiRed, contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(Modifier.height(0.dp))
                            Text("  Record Payment")
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val uri = vm.generatePdfUri(context) ?: return@launch
                                    val view = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(view, "Open PDF")
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.height(0.dp))
                            Text(" PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val uri = vm.generatePdfUri(context) ?: return@launch
                                    val biz = business
                                    val t = totals
                                    val summary = "Invoice ${InvoiceCalc.displayNumber(prefix, i.sequenceNumber)} — Total " +
                                        CurrencyUtils.format(t.grandTotal, currency)
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, summary)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        setPackage("com.whatsapp")
                                    }
                                    try {
                                        context.startActivity(send)
                                    } catch (_: ActivityNotFoundException) {
                                        send.setPackage(null)
                                        context.startActivity(
                                            Intent.createChooser(send, "Share invoice")
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.height(0.dp))
                            Text(" WhatsApp")
                        }
                    }
                }
            }
        }
    ) { padding ->
        val i = inv
        if (i == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            InvoiceCalc.displayNumber(prefix, i.sequenceNumber),
                            color = DigiRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            "Issued ${dateFmt.format(Date(i.issueDate))}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        i.dueDate?.let {
                            Text(
                                "Due ${dateFmt.format(Date(it))}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    StatusBadge(status = totals.status)
                }
                HorizontalDivider()
                Column {
                    Text(
                        "BILL TO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        customer?.name ?: "—",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    customer?.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    customer?.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                HorizontalDivider()
                Text("Items", fontWeight = FontWeight.SemiBold, color = DigiRed)
                items.forEach { it ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(it.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${itemDisplayNum(it.quantity)} × ${CurrencyUtils.format(it.unitPrice, currency)}" +
                                    if (it.taxPercent > 0) "  • tax ${itemDisplayNum(it.taxPercent)}%" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val line = it.quantity * it.unitPrice * (1 + it.taxPercent / 100.0)
                        Text(CurrencyUtils.format(line, currency), fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    TotalsLine("Subtotal", totals.subtotal, currency)
                    TotalsLine("Tax", totals.totalTax, currency)
                    TotalsLine(
                        if (i.discountIsPercent) "Discount (${itemDisplayNum(i.discountValue)}%)"
                        else "Discount",
                        -totals.discountAmount,
                        currency
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row {
                        Text("Grand Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(
                            CurrencyUtils.format(totals.grandTotal, currency),
                            color = DigiRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (i.amountPaid > 0) {
                        TotalsLine("Paid", i.amountPaid, currency)
                        Row {
                            Text("Balance", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(
                                CurrencyUtils.format(totals.balance, currency),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (!i.notes.isNullOrBlank()) {
                    Column {
                        Text(
                            "Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(i.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this invoice?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        vm.delete()
                        navController.navigateUp()
                    }
                }) { Text("Delete", color = DigiRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (showPayment) {
        RecordPaymentSheet(
            invoiceId = vm.invoiceId,
            balance = totals.balance,
            currency = currency,
            onDismiss = { showPayment = false },
            onSaved = {}
        )
    }
}

@Composable
private fun TotalsLine(label: String, amount: Double, currency: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(CurrencyUtils.format(amount, currency))
    }
}

private fun itemDisplayNum(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
