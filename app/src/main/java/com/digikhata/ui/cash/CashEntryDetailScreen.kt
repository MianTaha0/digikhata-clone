package com.digikhata.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.digikhata.ui.components.ZoomableImageDialog
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashEntryDetailScreen(
    navController: NavController,
    vm: CashEntryDetailViewModel = hiltViewModel()
) {
    val entry by vm.entry.collectAsState()
    val currency by vm.currency.collectAsState()
    val scope = rememberCoroutineScope()

    var showDelete by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var zoomPath by remember { mutableStateOf<String?>(null) }

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Entry") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }, enabled = entry != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDelete = true }, enabled = entry != null) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        val e = entry
        if (e == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Loading…") }
        } else {
            val accent = if (e.type == 1) DigiGreen else DigiRed
            val title = if (e.type == 1) "Cash In" else "Cash Out"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(title, color = accent, fontWeight = FontWeight.SemiBold)
                Text(
                    CurrencyUtils.format(e.amount, currency),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                )
                AssistChip(
                    onClick = {},
                    label = { Text(CashCategories.labelOf(e.category)) },
                    leadingIcon = { Icon(CashCategories.iconOf(e.category), contentDescription = null) }
                )
                Row {
                    Text("Date: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateFmt.format(Date(e.entryDate)), fontWeight = FontWeight.SemiBold)
                }
                if (!e.note.isNullOrBlank()) {
                    Column {
                        Text("Note", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(e.note, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                e.imageLocalPath?.let { path ->
                    AsyncImage(
                        model = "file://$path",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { zoomPath = path }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showEdit = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }
    }

    if (showDelete) {
        val e = entry
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    if (e != null) {
                        scope.launch {
                            vm.delete(e)
                            navController.navigateUp()
                        }
                    }
                }) { Text("Delete", color = DigiRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }

    zoomPath?.let { path ->
        ZoomableImageDialog(path = path, onDismiss = { zoomPath = null })
    }

    val currentEntry = entry
    if (showEdit && currentEntry != null) {
        AddCashEntrySheet(
            type = currentEntry.type,
            editing = currentEntry,
            activeBookId = currentEntry.businessId,
            onDismiss = { showEdit = false },
            onSaved = { showEdit = false }
        )
    }
}
