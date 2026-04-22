package com.digikhata.ui.inventory

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.digikhata.data.entity.Product
import com.digikhata.ui.components.ZoomableImageDialog
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    vm: ProductDetailViewModel = hiltViewModel()
) {
    val product by vm.product.collectAsState()
    val movements by vm.movements.collectAsState()
    val currency by vm.currency.collectAsState()
    val scope = rememberCoroutineScope()

    var showDelete by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var zoomPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editing = product }, enabled = product != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDelete = true }, enabled = product != null) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        val p = product
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
        } else {
            val isLow = p.lowStockThreshold > 0 && p.quantity <= p.lowStockThreshold
            val isOut = p.quantity <= 0
            val margin = p.sellPrice - p.costPrice

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                p.imageLocalPath?.let { path ->
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

                Column {
                    Text(
                        p.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!p.sku.isNullOrBlank()) {
                        Text(
                            "SKU: ${p.sku}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${formatQty(p.quantity)} ${p.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = if (isOut) DigiRed else MaterialTheme.colorScheme.onSurface
                    )
                    when {
                        isOut -> AssistChip(
                            onClick = {},
                            label = { Text("Out of stock") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DigiRed.copy(alpha = 0.15f),
                                labelColor = DigiRed
                            )
                        )
                        isLow -> AssistChip(
                            onClick = {},
                            label = { Text("Low stock") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DigiRed.copy(alpha = 0.15f),
                                labelColor = DigiRed
                            )
                        )
                    }
                }

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PriceCell("Cost", CurrencyUtils.format(p.costPrice, currency), MaterialTheme.colorScheme.onSurface)
                    PriceCell("Sell", CurrencyUtils.format(p.sellPrice, currency), MaterialTheme.colorScheme.onSurface)
                    PriceCell(
                        "Margin",
                        CurrencyUtils.format(margin, currency),
                        if (margin >= 0) DigiGreen else DigiRed
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { showAdjust = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DigiRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Adjust Stock") }

                OutlinedButton(
                    onClick = { editing = p },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit product")
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Movements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (movements.isEmpty()) {
                    Text(
                        "No stock changes yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column {
                        movements.forEach { m ->
                            MovementRow(movement = m, unit = p.unit)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        val p = product
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this product?") },
            text = { Text("This will also remove its stock history. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    if (p != null) {
                        scope.launch {
                            vm.delete(p)
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

    if (showAdjust && product != null) {
        AdjustStockSheet(
            unit = product!!.unit,
            onDismiss = { showAdjust = false },
            onSubmit = { delta, reason -> vm.adjust(delta, reason) }
        )
    }

    zoomPath?.let { path ->
        ZoomableImageDialog(path = path, onDismiss = { zoomPath = null })
    }

    val editTarget = editing
    if (editTarget != null) {
        AddEditProductSheet(
            editing = editTarget,
            activeBookId = editTarget.businessId,
            onDismiss = { editing = null },
            onSaved = { editing = null }
        )
    }
}

@Composable
private fun PriceCell(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
