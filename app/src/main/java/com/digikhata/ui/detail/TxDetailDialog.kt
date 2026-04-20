package com.digikhata.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.digikhata.data.entity.TxEntity
import com.digikhata.ui.components.ZoomableImageDialog
import com.digikhata.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TxDetailDialog(
    tx: TxEntity,
    currency: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showZoom by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tx.type == 0) "You Gave" else "You Got") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amount: ${CurrencyUtils.format(tx.amount, currency)}")
                Text("Date: ${dateFmt.format(Date(tx.entryDate))}")
                if (!tx.notes.isNullOrBlank()) Text("Note: ${tx.notes}")
                if (tx.imageLocalPath != null) {
                    AsyncImage(
                        model = "file://${tx.imageLocalPath}",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(top = 4.dp)
                    )
                    TextButton(onClick = { showZoom = true }) { Text("View fullscreen") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    )

    if (showZoom && tx.imageLocalPath != null) {
        ZoomableImageDialog(path = tx.imageLocalPath, onDismiss = { showZoom = false })
    }
}
