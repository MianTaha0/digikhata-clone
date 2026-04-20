package com.digikhata.ui.supplier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.ui.components.EmptyState
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.customer.AddEditCustomerSheet
import com.digikhata.ui.home.ClientCard
import com.digikhata.ui.theme.DigiError
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    onBack: () -> Unit,
    onOpenClient: (Long) -> Unit,
    vm: SupplierViewModel = hiltViewModel()
) {
    val suppliers by vm.suppliers.collectAsState()
    val totals by vm.totals.collectAsState()
    val business by vm.business.collectAsState()
    val currency = business?.currency ?: "Pakistan Rupee-Rs"
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TotalBox("You will pay", totals.totalWillGive, DigiError, currency, modifier = Modifier.weight(1f))
                    TotalBox("You will receive", totals.totalWillGet, DigiGreen, currency, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
                if (suppliers.isEmpty()) {
                    EmptyState(title = "No suppliers yet", subtitle = "Tap + Add Supplier to begin.")
                } else {
                    LazyColumn {
                        items(suppliers, key = { it.client.id }) { item ->
                            ClientCard(
                                item = item,
                                currency = currency,
                                onClick = { onOpenClient(item.client.id) },
                                onPinToggle = { vm.togglePin(it) },
                                onArchiveToggle = { vm.toggleArchive(it) },
                                onDelete = { vm.delete(it) },
                                type = 1
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = { showSheet = true },
                containerColor = DigiRed,
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Supplier") }
            )
        }
    }

    if (showSheet) {
        AddEditCustomerSheet(
            initial = null,
            type = 1,
            onDismiss = { showSheet = false },
            onSave = {
                vm.upsert(it)
                showSheet = false
            }
        )
    }
}

@Composable
private fun TotalBox(label: String, amount: Double, color: Color, currency: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), shape = MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Text(label, color = color.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        Text(
            CurrencyUtils.format(amount, currency),
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
