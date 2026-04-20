package com.digikhata.ui.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Select currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(DIGI_CURRENCIES) { c ->
                Text(
                    c,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(c) }
                        .padding(16.dp),
                    fontWeight = if (c == current) FontWeight.Bold else FontWeight.Normal
                )
                HorizontalDivider()
            }
        }
    }
}
