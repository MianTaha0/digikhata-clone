package com.digikhata.ui.staff

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.digikhata.data.entity.Staff
import com.digikhata.data.entity.StaffPayment
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    navController: NavController,
    vm: StaffDetailViewModel = hiltViewModel()
) {
    val staff by vm.staff.collectAsState()
    val payments by vm.payments.collectAsState()
    val paidThisMonth by vm.paidThisMonth.collectAsState()
    val currency by vm.currency.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDelete by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showAddPayment by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<StaffPayment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }, enabled = staff != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDelete = true }, enabled = staff != null) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = digiTopBarColors()
            )
        },
        floatingActionButton = {
            if (staff != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddPayment = true },
                    containerColor = DigiRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Payment") }
                )
            }
        }
    ) { padding ->
        val s = staff
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (s.imageLocalPath != null) {
                        AsyncImage(
                            model = "file://${s.imageLocalPath}",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DigiRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                s.name.firstOrNull()?.uppercase() ?: "?",
                                color = DigiRed,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            s.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!s.role.isNullOrBlank()) {
                            Text(
                                s.role,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!s.phone.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${s.phone}"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = DigiRed)
                        Text(
                            s.phone,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DigiRed
                        )
                    }
                }

                Divider()

                ThisMonthCard(
                    monthlySalary = s.monthlySalary,
                    paidThisMonth = paidThisMonth,
                    currency = currency
                )

                Divider()

                Text(
                    "Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (payments.isEmpty()) {
                    Text(
                        "No payments yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column {
                        payments.forEach { p ->
                            PaymentRow(
                                payment = p,
                                currency = currency,
                                onLongPress = { paymentToDelete = p }
                            )
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }
                    }
                }

                Spacer(Modifier.height(72.dp))
            }
        }
    }

    if (showDelete) {
        val s = staff
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this staff member?") },
            text = { Text("This will also remove all payment history. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    if (s != null) {
                        scope.launch {
                            vm.delete(s)
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

    val editTarget = staff
    if (showEdit && editTarget != null) {
        AddEditStaffSheet(
            editing = editTarget,
            activeBookId = editTarget.businessId,
            onDismiss = { showEdit = false },
            onSaved = { showEdit = false }
        )
    }

    if (showAddPayment) {
        AddPaymentSheet(
            onDismiss = { showAddPayment = false },
            onSubmit = { amount, date, note ->
                vm.addPayment(amount, date, note)
            }
        )
    }

    paymentToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Delete this payment?") },
            confirmButton = {
                TextButton(onClick = {
                    paymentToDelete = null
                    scope.launch { vm.deletePayment(p) }
                }) { Text("Delete", color = DigiRed) }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Suppress("unused")
private fun unusedStaffRef(s: Staff) {}
