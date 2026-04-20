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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBookScreen(
    onDone: () -> Unit,
    vm: BookViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(DIGI_CURRENCIES.first()) }
    var colorHex by remember { mutableStateOf(DIGI_BOOK_COLORS.first()) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Book") },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Business / Book name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Owner name") }, modifier = Modifier.fillMaxWidth())

            Text("Currency", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCurrencyPicker = true }
                    .padding(vertical = 12.dp)
            ) {
                Text(currency, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            Text("Book color", style = MaterialTheme.typography.labelLarge)
            ColorSwatchRow(selectedHex = colorHex, onSelect = { colorHex = it })

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.create(name, owner, currency, colorHex) { onDone() } },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DigiRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Create Book", fontWeight = FontWeight.SemiBold) }
        }
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            current = currency,
            onDismiss = { showCurrencyPicker = false },
            onPick = { currency = it; showCurrencyPicker = false }
        )
    }
}
