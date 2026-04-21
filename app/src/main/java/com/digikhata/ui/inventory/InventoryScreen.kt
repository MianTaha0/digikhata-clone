package com.digikhata.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.navigation.Routes
import com.digikhata.ui.theme.DigiRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    vm: InventoryViewModel = hiltViewModel()
) {
    val products by vm.products.collectAsState()
    val itemCount by vm.itemCount.collectAsState()
    val totalValue by vm.totalValue.collectAsState()
    val lowCount by vm.lowCount.collectAsState()
    val filter by vm.filter.collectAsState()
    val currency by vm.currency.collectAsState()
    val bookId by vm.activeBookId.collectAsState()

    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock") },
                colors = digiTopBarColors()
            )
        },
        floatingActionButton = {
            val bid = bookId
            if (bid != null) {
                ExtendedFloatingActionButton(
                    onClick = { showSheet = true },
                    containerColor = DigiRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Product") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(Modifier.height(8.dp))
            InventorySummaryCard(
                itemCount = itemCount,
                totalValue = totalValue,
                lowCount = lowCount,
                currency = currency
            )
            FilterChipRow(current = filter, onSelect = { vm.setFilter(it) })
            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when (filter) {
                            InventoryFilter.ALL -> "No products yet"
                            InventoryFilter.LOW -> "No low-stock products"
                            InventoryFilter.OUT -> "No out-of-stock products"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            currency = currency,
                            onClick = { navController.navigate(Routes.productDetail(product.id)) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    val bid = bookId
    if (showSheet && bid != null) {
        AddEditProductSheet(
            editing = null,
            activeBookId = bid,
            onDismiss = { showSheet = false },
            onSaved = { showSheet = false }
        )
    }
}

@Composable
private fun FilterChipRow(current: InventoryFilter, onSelect: (InventoryFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InventoryFilter.values().forEach { f ->
            FilterChip(
                selected = current == f,
                onClick = { onSelect(f) },
                label = { Text(f.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
