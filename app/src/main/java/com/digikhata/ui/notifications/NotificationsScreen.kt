package com.digikhata.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.ui.components.EmptyState
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    vm: NotificationsViewModel = hiltViewModel()
) {
    val items by vm.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            if (items.isEmpty()) {
                EmptyState(title = "No reminders yet", subtitle = "Tap the bell on a customer's detail screen to send one.")
            } else {
                val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { n ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (n.isSeen) Color.White else DigiRed.copy(alpha = 0.06f))
                                .clickable { vm.markSeen(n.id) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = DigiRed)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(n.clientName, fontWeight = FontWeight.SemiBold)
                                val detail = n.details ?: "Reminder for ${CurrencyUtils.format(n.amount, n.currency)}"
                                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFmt.format(Date(n.entryDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
