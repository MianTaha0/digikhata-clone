package com.digikhata.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.digikhata.data.entity.Client
import com.digikhata.ui.components.EmptyState
import com.digikhata.ui.customer.AddEditCustomerSheet
import com.digikhata.ui.theme.DigiError
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils

@Composable
fun HomeScreen(
    onOpenClient: (Long) -> Unit,
    onOpenSupplier: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val clients by vm.clients.collectAsState()
    val totals by vm.totals.collectAsState()
    val business by vm.business.collectAsState()
    val currency = business?.currency ?: "Pakistan Rupee-Rs"
    var showSheet by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TotalsStrip(
                willGet = totals.totalWillGet,
                willGive = totals.totalWillGive,
                currency = currency
            )
            TabBar(onSupplierClick = onOpenSupplier)
            if (clients.isEmpty()) {
                EmptyState(
                    title = "No customers yet",
                    subtitle = "Tap + Add Customer to add your first entry."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(clients, key = { it.client.id }) { item ->
                        ClientCard(
                            item = item,
                            currency = currency,
                            onClick = { onOpenClient(item.client.id) },
                            onPinToggle = { vm.togglePin(it) },
                            onArchiveToggle = { vm.toggleArchive(it) },
                            onDelete = { vm.deleteClient(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editingClient = null; showSheet = true },
            containerColor = DigiRed,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
            text = { Text("Add Customer") }
        )
    }

    if (showSheet) {
        AddEditCustomerSheet(
            initial = editingClient,
            type = 0,
            onDismiss = { showSheet = false },
            onSave = {
                vm.upsertClient(it)
                showSheet = false
            }
        )
    }
}

@Composable
private fun TotalsStrip(willGet: Double, willGive: Double, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TotalBox("You will give", willGive, DigiError, currency, modifier = Modifier.weight(1f))
        TotalBox("You will get", willGet, DigiGreen, currency, modifier = Modifier.weight(1f))
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

@Composable
private fun TabBar(onSupplierClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Tab("Customers", selected = true, onClick = {})
        Tab("Suppliers", selected = false, onClick = onSupplierClick)
    }
    HorizontalDivider()
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Tab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = if (selected) DigiRed else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) DigiRed else Color.Transparent)
        )
    }
}
