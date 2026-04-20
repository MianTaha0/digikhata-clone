package com.digikhata.ui.customer

import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.digikhata.data.entity.Client
import com.digikhata.ui.theme.DigiRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerSheet(
    initial: Client? = null,
    type: Int = 0,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var phone2 by remember { mutableStateOf(initial?.phone2.orEmpty()) }
    var cnic by remember { mutableStateOf(initial?.cnic.orEmpty()) }
    var creditLimit by remember { mutableStateOf(if (initial?.creditLimit != null && initial.creditLimit > 0) initial.creditLimit.toString() else "") }
    val context = LocalContext.current

    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null, null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val n = c.getString(0)
                        val p = if (c.columnCount > 1) c.getString(1) else null
                        if (!n.isNullOrBlank()) name = n
                        if (!p.isNullOrBlank()) phone = p
                    }
                }
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (initial == null) (if (type == 0) "Add Customer" else "Add Supplier")
                else "Edit",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone2,
                onValueChange = { phone2 = it },
                label = { Text("Phone 2 (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value = cnic, onValueChange = { cnic = it }, label = { Text("CNIC (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = creditLimit,
                onValueChange = { creditLimit = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Credit Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { contactLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ContactPhone, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Import from Contacts")
            }
            Button(
                onClick = {
                    val c = (initial ?: Client(businessId = 0L, type = type, name = "")).copy(
                        name = name.trim(),
                        phone = phone.trim().ifBlank { null },
                        phone2 = phone2.trim().ifBlank { null },
                        cnic = cnic.trim().ifBlank { null },
                        creditLimit = creditLimit.toDoubleOrNull() ?: 0.0,
                        type = type
                    )
                    if (c.name.isNotBlank()) onSave(c)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DigiRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
