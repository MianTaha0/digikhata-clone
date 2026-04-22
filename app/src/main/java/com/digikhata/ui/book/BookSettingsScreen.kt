package com.digikhata.ui.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSettingsScreen(
    onBack: () -> Unit,
    vm: BookViewModel = hiltViewModel()
) {
    val business by vm.current.collectAsState()
    val b = business
    var name by remember(b?.id) { mutableStateOf(b?.name.orEmpty()) }
    var colorHex by remember(b?.id) { mutableStateOf(b?.colorHex ?: "#E74425") }
    var currency by remember(b?.id) { mutableStateOf(b?.currency ?: "Pakistan Rupee-Rs") }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        if (b == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Book name") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Currency", style = MaterialTheme.typography.labelLarge)
                    Text(currency, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            HorizontalDivider()

            Text("Theme color", style = MaterialTheme.typography.labelLarge)
            ColorSwatchRow(selectedHex = colorHex, onSelect = { colorHex = it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.update(b.copy(name = name.trim().ifBlank { b.name }, currency = currency, colorHex = colorHex)) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save") }

            Button(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.buttonColors(containerColor = DigiError, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Delete Book") }
        }
    }

    if (showPicker) {
        CurrencyPickerSheet(
            current = currency,
            onDismiss = { showPicker = false },
            onPick = { currency = it; showPicker = false }
        )
    }

    if (confirmDelete && b != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this book?") },
            text = { Text("All customers, suppliers and transactions in this book will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(b) {
                        confirmDelete = false
                        onBack()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
