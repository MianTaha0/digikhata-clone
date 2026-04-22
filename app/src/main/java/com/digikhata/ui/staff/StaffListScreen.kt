package com.digikhata.ui.staff

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.navigation.Routes
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(
    navController: NavController,
    vm: StaffListViewModel = hiltViewModel()
) {
    val staff by vm.staff.collectAsState()
    val count by vm.staffCount.collectAsState()
    val totalPayroll by vm.totalPayroll.collectAsState()
    val paidThisMonth by vm.paidThisMonth.collectAsState()
    val paidByStaff by vm.paidThisMonthByStaff.collectAsState()
    val currency by vm.currency.collectAsState()
    val bookId by vm.activeBookId.collectAsState()

    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                    text = { Text("Add Staff") }
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
            StaffSummaryCard(
                count = count,
                totalPayroll = totalPayroll,
                paidThisMonth = paidThisMonth,
                currency = currency
            )
            Spacer(Modifier.height(4.dp))
            if (staff.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No staff yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(staff, key = { it.id }) { s ->
                        StaffRow(
                            staff = s,
                            paidThisMonth = paidByStaff[s.id] ?: 0.0,
                            currency = currency,
                            onClick = { navController.navigate(Routes.staffDetail(s.id)) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    val bid = bookId
    if (showSheet && bid != null) {
        AddEditStaffSheet(
            editing = null,
            activeBookId = bid,
            onDismiss = { showSheet = false },
            onSaved = { showSheet = false }
        )
    }
}

@Composable
private fun StaffSummaryCard(
    count: Int,
    totalPayroll: Double,
    paidThisMonth: Double,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryCell(
                value = count.toString(),
                label = "Staff"
            )
            SummaryCell(
                value = CurrencyUtils.format(totalPayroll, currency),
                label = "Monthly",
                valueColor = DigiRed
            )
            SummaryCell(
                value = CurrencyUtils.format(paidThisMonth, currency),
                label = "Paid this month"
            )
        }
    }
}

@Composable
private fun SummaryCell(
    value: String,
    label: String,
    valueColor: Color = Color.Unspecified
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
