package com.digikhata.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.ui.components.PhotoPickerRow
import com.digikhata.ui.theme.DigiRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    editing: ExpenseEntry?,
    activeBookId: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    vm: ExpenseSheetViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var amount by remember {
        mutableStateOf(
            editing?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var category by remember { mutableStateOf(editing?.category ?: "other") }
    var payment by remember {
        mutableStateOf(editing?.paymentMethod?.let(PaymentMethod::fromKey) ?: PaymentMethod.CASH)
    }
    var selectedDate by remember { mutableStateOf(editing?.entryDate ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(editing?.note.orEmpty()) }
    var imagePath by remember { mutableStateOf(editing?.imageLocalPath) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showPaymentPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val focusRequester = remember { FocusRequester() }

    val title = if (editing == null) "Add Expense" else "Edit Expense"

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(DigiRed))
                Text(title, color = DigiRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' } },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showCategoryPicker = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(ExpenseCategories.iconOf(category), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ExpenseCategories.labelOf(category), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { showCategoryPicker = true }) { Text("Change") }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showPaymentPicker = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(payment.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Payment Method", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(payment.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { showPaymentPicker = true }) { Text("Change") }
            }

            OutlinedTextField(
                value = dateFmt.format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
            )
            TextButton(onClick = { showDatePicker = true }) { Text("Change date") }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (imagePath != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "file://$imagePath",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp))
                    )
                    IconButton(onClick = { imagePath = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }
            PhotoPickerRow(onPicked = { imagePath = it }, scope = scope)

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        scope.launch {
                            val entry = ExpenseEntry(
                                id = editing?.id ?: 0,
                                businessId = activeBookId,
                                amount = amt,
                                category = category,
                                paymentMethod = payment.key,
                                note = note.trim().ifBlank { null },
                                entryDate = selectedDate,
                                imageLocalPath = imagePath,
                                createdAt = editing?.createdAt ?: 0,
                                updatedAt = System.currentTimeMillis()
                            )
                            if (editing == null) {
                                vm.save(entry, imagePath)
                            } else {
                                vm.update(entry)
                            }
                            onSaved()
                            onDismiss()
                        }
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = DigiRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (editing == null) "Save" else "Update") }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showCategoryPicker) {
        ExpenseCategoryPicker(
            selected = category,
            onSelect = { category = it },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showPaymentPicker) {
        PaymentMethodPicker(
            selected = payment,
            onSelect = { payment = it },
            onDismiss = { showPaymentPicker = false }
        )
    }
}
