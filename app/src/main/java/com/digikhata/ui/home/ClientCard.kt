package com.digikhata.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digikhata.data.entity.Client
import com.digikhata.domain.model.ClientBalance
import com.digikhata.ui.components.AvatarCircle
import com.digikhata.ui.components.BalancePill

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClientCard(
    item: ClientBalance,
    currency: String,
    onClick: () -> Unit,
    onPinToggle: (Client) -> Unit,
    onArchiveToggle: (Client) -> Unit,
    onDelete: (Client) -> Unit,
    type: Int = 0
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = item.client.name)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.client.name,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.client.isPinned) {
                    Spacer(Modifier.width(6.dp))
                    Text("\uD83D\uDCCC", fontWeight = FontWeight.Normal)
                }
            }
            val sub = item.client.phone ?: ""
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BalancePill(balance = item.balance, currency = currency, type = type)

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (item.client.isPinned) "Unpin" else "Pin") },
                onClick = { menuOpen = false; onPinToggle(item.client) }
            )
            DropdownMenuItem(
                text = { Text(if (item.client.isArchived) "Unarchive" else "Archive") },
                onClick = { menuOpen = false; onArchiveToggle(item.client) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { menuOpen = false; onDelete(item.client) }
            )
        }
    }
}
