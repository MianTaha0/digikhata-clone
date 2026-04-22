package com.digikhata.ui.detail

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.data.entity.TxEntity
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.notifications.ReminderSheet
import com.digikhata.ui.theme.DigiError
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import com.digikhata.util.PhoneUtils
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    onBack: () -> Unit,
    vm: DetailViewModel = hiltViewModel()
) {
    val client by vm.client.collectAsState()
    val txs by vm.transactions.collectAsState()
    val balance by vm.balance.collectAsState()
    val business by vm.business.collectAsState()
    val currency = business?.currency ?: "Pakistan Rupee-Rs"
    val context = LocalContext.current

    var addSheetType by remember { mutableStateOf<Int?>(null) }
    var detailTx by remember { mutableStateOf<TxEntity?>(null) }
    var showReminder by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.name ?: "Customer") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        val phone = client?.phone?.let { PhoneUtils.cleanPhone(it) }.orEmpty()
                        if (phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone"))
                            context.startActivity(intent)
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp") }
                    IconButton(onClick = { showReminder = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminder")
                    }
                },
                colors = digiTopBarColors()
            )
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { addSheetType = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = DigiError, contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("+ You Gave") }
                Button(
                    onClick = { addSheetType = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = DigiGreen, contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("+ You Got") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            BalanceCard(balance = balance, currency = currency, type = client?.type ?: 0)
            Spacer(Modifier.height(8.dp))
            if (txs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(txs, key = { it.id }) { tx ->
                        TxRow(
                            tx = tx,
                            currency = currency,
                            onClick = { detailTx = tx },
                            onEdit = { detailTx = tx },
                            onDelete = { vm.deleteTx(tx) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    addSheetType?.let { t ->
        AddTxSheet(
            type = t,
            onDismiss = { addSheetType = null },
            onSave = { amount, date, notes, image ->
                vm.addTransaction(amount, t, notes, date, image)
                addSheetType = null
            }
        )
    }

    detailTx?.let { tx ->
        TxDetailDialog(
            tx = tx,
            currency = currency,
            onDismiss = { detailTx = null },
            onDelete = {
                vm.deleteTx(tx)
                detailTx = null
            }
        )
    }

    if (showReminder) {
        ReminderSheet(
            name = client?.name ?: "",
            phone = client?.phone.orEmpty(),
            amount = balance,
            currency = currency,
            onDismiss = { showReminder = false }
        )
    }
}

@Composable
private fun BalanceCard(balance: Double, currency: String, type: Int) {
    val isPositive = balance >= 0
    val title = when {
        balance == 0.0 -> "All settled"
        isPositive && type == 0 -> "You will get"
        isPositive && type == 1 -> "You will receive"
        !isPositive && type == 0 -> "You will give"
        else -> "You will pay"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DigiRed)
            .padding(20.dp)
    ) {
        Column {
            Text(title, color = Color.White.copy(alpha = 0.85f))
            Text(
                CurrencyUtils.format(abs(balance), currency),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        }
    }
}
