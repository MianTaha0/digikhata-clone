package com.digikhata.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.ui.theme.DigiRed

@Composable
fun DrawerContent(
    onClose: () -> Unit,
    onNavigateCreateBook: () -> Unit,
    onOpenBookSettings: (Long) -> Unit,
    onOpenStaff: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenSignIn: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    vm: DrawerViewModel = hiltViewModel()
) {
    val businesses by vm.businesses.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    val pendingSyncCount by vm.pendingSyncCount.collectAsState()
    val lastPullAt by vm.lastPullAt.collectAsState()

    ModalDrawerSheet(drawerContainerColor = Color.White) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(DigiRed)
                .padding(20.dp)
        ) {
            Text("DigiKhata", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Your Books",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(businesses, key = { it.id }) { b ->
                val isActive = b.id == activeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) DigiRed.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            vm.setActive(b.id)
                            onClose()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = if (isActive) DigiRed else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(b.name, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                        if (!b.ownerName.isNullOrBlank()) {
                            Text(b.ownerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .clickable { onOpenBookSettings(b.id) }
                            .padding(4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateCreateBook() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = DigiRed)
            Spacer(Modifier.width(12.dp))
            Text("Create New Book", color = DigiRed, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider()
        if (currentUser != null) {
            DrawerRow(
                icon = Icons.Default.AccountCircle,
                label = currentUser?.phoneNumber ?: "Account"
            ) { onOpenProfile() }
            SyncStatusPill(pendingSyncCount, lastPullAt)
        } else {
            DrawerRow(Icons.Default.CloudSync, "Sign in to sync") { onOpenSignIn() }
        }
        DrawerRow(Icons.Default.BarChart, "Reports") { onOpenReports() }
        DrawerRow(Icons.Default.Group, "Staff") { onOpenStaff() }
        DrawerRow(Icons.Default.Share, "Share App") { onClose() }
        DrawerRow(Icons.Default.Star, "Rate App") { onClose() }
        DrawerRow(Icons.Default.Settings, "Settings") { onOpenSettings() }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SyncStatusPill(pendingCount: Int, lastPullAt: Long) {
    val amber = Color(0xFFB45309)
    val green = Color(0xFF16A34A)
    val grey = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pendingCount > 0) {
                Text(
                    text = "Syncing $pendingCount changes…",
                    color = amber,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(green)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Synced",
                    color = green,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        val label = formatLastPull(lastPullAt)
        if (label != null) {
            Text(
                text = label,
                color = grey,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatLastPull(ts: Long): String? {
    if (ts <= 0L) return null
    val diffSec = ((System.currentTimeMillis() - ts) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> "Last synced: just now"
        diffSec < 3600 -> "Last synced: ${diffSec / 60} min ago"
        diffSec < 86400 -> "Last synced: ${diffSec / 3600} hr ago"
        else -> "Last synced: ${diffSec / 86400} d ago"
    }
}

@Composable
private fun DrawerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}
