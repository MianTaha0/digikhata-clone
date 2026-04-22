package com.digikhata.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.digikhata.data.entity.Staff
import com.digikhata.ui.components.PhotoPickerRow
import com.digikhata.ui.theme.DigiRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStaffSheet(
    editing: Staff?,
    activeBookId: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    vm: StaffSheetViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var role by remember { mutableStateOf(editing?.role.orEmpty()) }
    var phone by remember { mutableStateOf(editing?.phone.orEmpty()) }
    var salary by remember {
        mutableStateOf(
            editing?.monthlySalary?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var joiningDate by remember { mutableStateOf(editing?.joiningDate ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(editing?.notes.orEmpty()) }
    var imagePath by remember { mutableStateOf(editing?.imageLocalPath) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = joiningDate)
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val focusRequester = remember { FocusRequester() }

    val title = if (editing == null) "Add Staff" else "Edit Staff"

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val canSave = name.isNotBlank() && (salary.toDoubleOrNull() ?: -1.0) >= 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(DigiRed))
                Text(title, color = DigiRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )

            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Role (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = salary,
                onValueChange = { v -> salary = v.filter { it.isDigit() || it == '.' } },
                label = { Text("Monthly salary") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dateFmt.format(Date(joiningDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Joining date") },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
            )
            TextButton(onClick = { showDatePicker = true }) { Text("Change date") }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            if (imagePath != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "file://$imagePath",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp))
                    )
                    IconButton(onClick = { imagePath = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }
            PhotoPickerRow(onPicked = { imagePath = it }, scope = scope)

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (!canSave) return@Button
                    val now = System.currentTimeMillis()
                    val s = Staff(
                        id = editing?.id ?: 0,
                        businessId = activeBookId,
                        name = name.trim(),
                        role = role.trim().ifBlank { null },
                        phone = phone.trim().ifBlank { null },
                        monthlySalary = salary.toDoubleOrNull() ?: 0.0,
                        joiningDate = joiningDate,
                        imageLocalPath = imagePath,
                        notes = notes.trim().ifBlank { null },
                        createdAt = editing?.createdAt ?: now,
                        updatedAt = now
                    )
                    scope.launch {
                        if (editing == null) vm.save(s, imagePath)
                        else vm.update(s)
                        onSaved()
                        onDismiss()
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = DigiRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (editing == null) "Save" else "Update") }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { joiningDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
