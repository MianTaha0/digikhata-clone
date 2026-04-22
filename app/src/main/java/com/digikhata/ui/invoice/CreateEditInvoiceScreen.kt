package com.digikhata.ui.invoice

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditInvoiceScreen(
    navController: NavController,
    vm: CreateEditInvoiceViewModel = hiltViewModel()
) {
    val customer by vm.customer.collectAsState()
    val issueDate by vm.issueDate.collectAsState()
    val dueDate by vm.dueDate.collectAsState()
    val notes by vm.notes.collectAsState()
    val discountStr by vm.discountValueStr.collectAsState()
    val discountIsPct by vm.discountIsPercent.collectAsState()
    val items by vm.items.collectAsState()
    val totals by vm.totals.collectAsState()
    val currency by vm.currency.collectAsState()
    val recentNames by vm.recentItemNames.collectAsState()

    val scope = rememberCoroutineScope()
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showIssuePicker by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEdit) "Edit Invoice" else "New Invoice") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val id = vm.save()
                                if (id != null) navController.navigateUp()
                            }
                        },
                        enabled = vm.canSave()
                    ) { Text("Save", color = Color.White) }
                },
                colors = digiTopBarColors()
            )
        },
        bottomBar = {
            TotalsCard(
                subtotal = totals.subtotal,
                tax = totals.totalTax,
                discount = totals.discountAmount,
                grand = totals.grandTotal,
                currency = currency
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Customer row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCustomerPicker = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Customer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        customer?.name ?: "Select customer",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (customer == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = { showCustomerPicker = true }) {
                    Text(if (customer == null) "Pick" else "Change")
                }
            }
            HorizontalDivider()

            // Dates row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateFmt.format(Date(issueDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Issue Date") },
                    modifier = Modifier.weight(1f).clickable { showIssuePicker = true }
                )
                OutlinedTextField(
                    value = dueDate?.let { dateFmt.format(Date(it)) } ?: "—",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due Date") },
                    modifier = Modifier.weight(1f).clickable { showDuePicker = true }
                )
            }

            // Items
            Text("Items", fontWeight = FontWeight.SemiBold, color = DigiRed)
            items.forEachIndexed { idx, it ->
                InvoiceItemRowEdit(
                    item = it,
                    recentNames = recentNames,
                    onChange = { upd -> vm.updateItem(idx, upd) },
                    onDelete = { vm.removeItem(idx) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
            OutlinedButton(onClick = { vm.addItem() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(" Add Item")
            }

            // Discount
            Text("Discount", fontWeight = FontWeight.SemiBold, color = DigiRed)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { v ->
                        vm.discountValueStr.value = v.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text(if (discountIsPct) "Discount %" else "Discount Amount") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !discountIsPct,
                    onClick = { vm.discountIsPercent.value = false },
                    label = { Text("Rs") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DigiRed.copy(alpha = 0.15f),
                        selectedLabelColor = DigiRed
                    )
                )
                FilterChip(
                    selected = discountIsPct,
                    onClick = { vm.discountIsPercent.value = true },
                    label = { Text("%") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DigiRed.copy(alpha = 0.15f),
                        selectedLabelColor = DigiRed
                    )
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { vm.notes.value = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showCustomerPicker) {
        CustomerPickerSheet(
            onDismiss = { showCustomerPicker = false },
            onPicked = { vm.setCustomer(it) }
        )
    }

    if (showIssuePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = issueDate)
        DatePickerDialog(
            onDismissRequest = { showIssuePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.issueDate.value = it }
                    showIssuePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showIssuePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    if (showDuePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDuePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.dueDate.value = state.selectedDateMillis
                    showDuePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.dueDate.value = null
                    showDuePicker = false
                }) { Text("Clear") }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun TotalsCard(
    subtotal: Double,
    tax: Double,
    discount: Double,
    grand: Double,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Subtotal", modifier = Modifier.weight(1f))
                Text(CurrencyUtils.format(subtotal, currency))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Tax", modifier = Modifier.weight(1f))
                Text(CurrencyUtils.format(tax, currency))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Discount", modifier = Modifier.weight(1f))
                Text("- ${CurrencyUtils.format(discount, currency)}")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Grand Total",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    CurrencyUtils.format(grand, currency),
                    color = DigiRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
