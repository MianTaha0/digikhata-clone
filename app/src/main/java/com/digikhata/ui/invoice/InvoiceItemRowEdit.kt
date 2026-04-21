package com.digikhata.ui.invoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceItemRowEdit(
    item: DraftItem,
    recentNames: List<String>,
    onChange: (DraftItem) -> Unit,
    onDelete: () -> Unit
) {
    var dropdownOpen by remember { mutableStateOf(false) }
    val filtered = remember(recentNames, item.name) {
        if (item.name.isBlank()) recentNames
        else recentNames.filter { it.contains(item.name, ignoreCase = true) && it != item.name }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = dropdownOpen && filtered.isNotEmpty(),
                onExpandedChange = { dropdownOpen = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = {
                        onChange(item.copy(name = it))
                        dropdownOpen = true
                    },
                    label = { Text("Item name") },
                    singleLine = true,
                    trailingIcon = {
                        if (recentNames.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen)
                        }
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownOpen && filtered.isNotEmpty(),
                    onDismissRequest = { dropdownOpen = false }
                ) {
                    filtered.take(10).forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onChange(item.copy(name = name))
                                dropdownOpen = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Remove")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = item.qtyStr,
                onValueChange = { v -> onChange(item.copy(qtyStr = v.filter { it.isDigit() || it == '.' })) },
                label = { Text("Qty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = item.priceStr,
                onValueChange = { v -> onChange(item.copy(priceStr = v.filter { it.isDigit() || it == '.' })) },
                label = { Text("Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = item.taxStr,
                onValueChange = { v -> onChange(item.copy(taxStr = v.filter { it.isDigit() || it == '.' })) },
                label = { Text("Tax %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
