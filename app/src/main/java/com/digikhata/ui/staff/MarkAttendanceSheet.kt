package com.digikhata.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digikhata.data.entity.StaffAttendance
import com.digikhata.domain.model.AttendanceStatus
import com.digikhata.ui.theme.DigiRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarkAttendanceSheet(
    dateMillis: Long,
    existing: StaffAttendance?,
    onDismiss: () -> Unit,
    onSave: (status: String, notes: String?) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember {
        mutableStateOf(existing?.let { AttendanceStatus.fromKey(it.status) } ?: AttendanceStatus.PRESENT)
    }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    val dateFmt = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                dateFmt.format(Date(dateMillis)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AttendanceStatus.values().forEach { s ->
                    FilterChip(
                        selected = selected == s,
                        onClick = { selected = s },
                        label = { Text(s.label) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Button(
                onClick = {
                    onSave(selected.key, notes.ifBlank { null })
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DigiRed,
                    contentColor = Color.White
                )
            ) {
                Text("Save")
            }

            if (existing != null) {
                TextButton(
                    onClick = {
                        onClear()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear", color = DigiRed)
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}
