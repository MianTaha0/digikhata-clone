package com.digikhata.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.digikhata.data.entity.TxEntity
import com.digikhata.ui.theme.DigiError
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TxRow(
    tx: TxEntity,
    currency: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isGave = tx.type == 0
    val accent = if (isGave) DigiError else DigiGreen
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(dateFmt.format(Date(tx.entryDate)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!tx.notes.isNullOrBlank()) {
                Text(tx.notes, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text(if (isGave) "You gave" else "You got", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (tx.imageLocalPath != null) {
            AsyncImage(
                model = "file://${tx.imageLocalPath}",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                CurrencyUtils.format(tx.amount, currency),
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (isGave) "Gave" else "Got",
                color = accent.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
        }
    }
}
