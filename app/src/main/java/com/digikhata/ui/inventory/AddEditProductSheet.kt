package com.digikhata.ui.inventory

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.digikhata.data.entity.Product
import com.digikhata.ui.components.PhotoPickerRow
import com.digikhata.ui.theme.DigiRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductSheet(
    editing: Product?,
    activeBookId: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    vm: ProductSheetViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var sku by remember { mutableStateOf(editing?.sku.orEmpty()) }
    var cost by remember { mutableStateOf(editing?.costPrice?.let(::formatNumber) ?: "") }
    var sell by remember { mutableStateOf(editing?.sellPrice?.let(::formatNumber) ?: "") }
    var qty by remember { mutableStateOf(editing?.quantity?.let(::formatNumber) ?: "") }
    var low by remember { mutableStateOf(editing?.lowStockThreshold?.let(::formatNumber) ?: "0") }
    var unit by remember { mutableStateOf(editing?.unit ?: "pcs") }
    var imagePath by remember { mutableStateOf(editing?.imageLocalPath) }

    val focusRequester = remember { FocusRequester() }
    val title = if (editing == null) "Add Product" else "Edit Product"
    val isEdit = editing != null

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val canSave = name.isNotBlank()
        && (cost.toDoubleOrNull() ?: -1.0) >= 0
        && (sell.toDoubleOrNull() ?: -1.0) >= 0
        && (qty.toDoubleOrNull() ?: -1.0) >= 0

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
                value = name,
                onValueChange = { name = it },
                label = { Text("Product name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU / code (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = cost,
                    onValueChange = { v -> cost = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("Cost price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sell,
                    onValueChange = { v -> sell = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("Sell price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = qty,
                    onValueChange = { v -> if (!isEdit) qty = v.filter { it.isDigit() || it == '.' } },
                    label = { Text(if (isEdit) "Quantity (use Adjust Stock)" else "Initial quantity") },
                    singleLine = true,
                    enabled = !isEdit,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = low,
                onValueChange = { v -> low = v.filter { it.isDigit() || it == '.' } },
                label = { Text("Low-stock threshold (0 = off)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    if (!canSave) return@Button
                    val now = System.currentTimeMillis()
                    val product = Product(
                        id = editing?.id ?: 0,
                        businessId = activeBookId,
                        name = name.trim(),
                        sku = sku.trim().ifBlank { null },
                        costPrice = cost.toDoubleOrNull() ?: 0.0,
                        sellPrice = sell.toDoubleOrNull() ?: 0.0,
                        quantity = if (isEdit) editing!!.quantity else (qty.toDoubleOrNull() ?: 0.0),
                        lowStockThreshold = low.toDoubleOrNull() ?: 0.0,
                        unit = unit.trim().ifBlank { "pcs" },
                        imageLocalPath = imagePath,
                        createdAt = editing?.createdAt ?: now,
                        updatedAt = now
                    )
                    scope.launch {
                        if (editing == null) vm.save(product, imagePath)
                        else vm.update(product)
                        onSaved()
                        onDismiss()
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = DigiRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (editing == null) "Save" else "Update") }
        }
    }
}

private fun formatNumber(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
