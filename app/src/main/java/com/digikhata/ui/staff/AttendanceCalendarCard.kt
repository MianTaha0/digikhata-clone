package com.digikhata.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.data.entity.StaffAttendance
import com.digikhata.domain.model.AttendanceStatus
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun statusColor(status: AttendanceStatus, outline: Color): Color = when (status) {
    AttendanceStatus.PRESENT -> DigiGreen
    AttendanceStatus.ABSENT -> DigiRed
    AttendanceStatus.HALF_DAY -> Color(0xFFFFB300)
    AttendanceStatus.LEAVE -> Color(0xFF1E88E5)
    AttendanceStatus.WEEK_OFF -> outline
}

@Composable
fun AttendanceCalendarCard(
    currency: String,
    vm: AttendanceCalendarViewModel = hiltViewModel()
) {
    val visibleMonth by vm.visibleMonth.collectAsState()
    val records by vm.records.collectAsState()
    val summary by vm.summary.collectAsState()
    val staff by vm.staff.collectAsState()

    var sheetDate by remember { mutableStateOf<Long?>(null) }

    // If staff deleted while viewing, hide the card
    if (staff == null) return

    val outline = MaterialTheme.colorScheme.outline
    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    // Today for "ring"
    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { vm.prevMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                val cal = Calendar.getInstance().apply {
                    clear()
                    set(Calendar.YEAR, visibleMonth.year)
                    set(Calendar.MONTH, visibleMonth.month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                Text(
                    monthFmt.format(cal.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { vm.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendItem("P", statusColor(AttendanceStatus.PRESENT, outline))
                LegendItem("A", statusColor(AttendanceStatus.ABSENT, outline))
                LegendItem("H", statusColor(AttendanceStatus.HALF_DAY, outline))
                LegendItem("L", statusColor(AttendanceStatus.LEAVE, outline))
                LegendItem("W", statusColor(AttendanceStatus.WEEK_OFF, outline))
            }

            // Summary chip row
            Text(
                "P ${summary.present} · A ${summary.absent} · H ${summary.halfDay} · Earned ${CurrencyUtils.format(summary.earned, currency)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Weekday header (Monday-first)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            d,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Calendar grid
            val firstOfMonth = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, visibleMonth.year)
                set(Calendar.MONTH, visibleMonth.month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Monday-first leading blanks
            // Calendar.DAY_OF_WEEK: Sunday=1..Saturday=7. Monday = 2.
            val dow = firstOfMonth.get(Calendar.DAY_OF_WEEK)
            val leading = ((dow - Calendar.MONDAY) + 7) % 7

            val totalCells = leading + daysInMonth
            val rows = (totalCells + 6) / 7

            // Map day-of-month -> record for quick lookup
            val recordsByDay: Map<Int, StaffAttendance> = remember(records, visibleMonth) {
                records.mapNotNull { r ->
                    val c = Calendar.getInstance().apply { timeInMillis = r.date }
                    if (c.get(Calendar.YEAR) == visibleMonth.year && c.get(Calendar.MONTH) == visibleMonth.month) {
                        c.get(Calendar.DAY_OF_MONTH) to r
                    } else null
                }.toMap()
            }

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - leading + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val cellCal = Calendar.getInstance().apply {
                                    clear()
                                    set(Calendar.YEAR, visibleMonth.year)
                                    set(Calendar.MONTH, visibleMonth.month)
                                    set(Calendar.DAY_OF_MONTH, dayNum)
                                }
                                val cellMillis = cellCal.timeInMillis
                                val isToday = cellMillis == todayMillis
                                val record = recordsByDay[dayNum]
                                val dotColor = record?.let {
                                    statusColor(AttendanceStatus.fromKey(it.status), outline)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (isToday) Modifier.border(
                                                1.dp,
                                                DigiRed,
                                                RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .clickable { sheetDate = cellMillis },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(dotColor ?: Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(2.dp))
        }
    }

    val targetDate = sheetDate
    if (targetDate != null) {
        val existing = records.firstOrNull { it.date == targetDate }
        MarkAttendanceSheet(
            dateMillis = targetDate,
            existing = existing,
            onDismiss = { sheetDate = null },
            onSave = { status, notes -> vm.upsert(targetDate, status, notes) },
            onClear = { vm.clear(targetDate) }
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
