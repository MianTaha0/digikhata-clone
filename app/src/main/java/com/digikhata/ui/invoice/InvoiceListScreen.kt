package com.digikhata.ui.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
fun InvoiceListScreen(
    navController: NavController,
    vm: InvoiceListViewModel = hiltViewModel()
) {
    val cards by vm.cards.collectAsState()
    val filter by vm.filter.collectAsState()
    val currency by vm.currency.collectAsState()
    val prefix by vm.prefix.collectAsState()
    val bookId by vm.activeBookId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices") },
                colors = digiTopBarColors()
            )
        },
        floatingActionButton = {
            if (bookId != null) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.INVOICE_CREATE) },
                    containerColor = DigiRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Invoice") }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InvoiceListFilter.values().forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { vm.setFilter(f) },
                        label = { Text(f.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            if (cards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No invoices yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(cards, key = { it.invoice.id }) { card ->
                        InvoiceCard(
                            data = card,
                            prefix = prefix,
                            currency = currency,
                            onClick = {
                                navController.navigate(Routes.invoiceDetail(card.invoice.id))
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
