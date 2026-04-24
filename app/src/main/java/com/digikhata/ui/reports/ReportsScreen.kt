package com.digikhata.ui.reports

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.domain.model.ReportsSummary
import com.digikhata.domain.model.TopClient
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    vm: ReportsViewModel = hiltViewModel()
) {
    val summary by vm.summary.collectAsState()
    val business by vm.business.collectAsState()
    val currency = business?.currency ?: "Pakistan Rupee-Rs"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = digiTopBarColors()
            )
        }
    ) { padding ->
        val isEmpty = summary.salesThisMonth == 0.0 &&
                summary.expensesThisMonth == 0.0 &&
                summary.cashInHand == 0.0 &&
                summary.topClients.isEmpty() &&
                summary.last30DaysSales.all { it == 0.0 }

        if (isEmpty) {
            EmptyReports(padding)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                StatCardsRow(summary, currency)
                Spacer(Modifier.height(20.dp))
                SalesChartSection(summary.last30DaysSales)
                Spacer(Modifier.height(20.dp))
                TopClientsSection(summary.topClients, currency)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EmptyReports(padding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No data yet — add transactions and invoices to see reports.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatCardsRow(summary: ReportsSummary, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Sales",
            sub = "This month",
            value = CurrencyUtils.format(summary.salesThisMonth, currency),
            color = DigiGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Expenses",
            sub = "This month",
            value = CurrencyUtils.format(summary.expensesThisMonth, currency),
            color = DigiRed,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Cash",
            sub = "In hand",
            value = CurrencyUtils.format(summary.cashInHand, currency),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    sub: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun SalesChartSection(points: List<Double>) {
    Text(
        "Sales — last 30 days",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.padding(12.dp)) {
            LineChart(
                points = points,
                lineColor = DigiRed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
    }
}

@Composable
private fun LineChart(
    points: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val max = (points.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1).toFloat()
        val path = Path()
        val fill = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - ((v / max).toFloat() * (h - 4f)) - 2f
            if (i == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, h)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo((points.size - 1) * stepX, h)
        fill.close()
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.20f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        drawPath(path, color = lineColor, style = Stroke(width = 3f))
    }
}

@Composable
private fun TopClientsSection(top: List<TopClient>, currency: String) {
    Text(
        "Top customers",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    if (top.isEmpty()) {
        Text(
            "No balances to show.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            top.forEachIndexed { i, tc ->
                TopClientRow(tc, currency)
                if (i != top.lastIndex) HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun TopClientRow(tc: TopClient, currency: String) {
    val positive = tc.balance > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DigiRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = DigiRed, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tc.client.name, fontWeight = FontWeight.SemiBold)
            Text(
                if (positive) "Will get" else "Will give",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            CurrencyUtils.format(kotlin.math.abs(tc.balance), currency),
            fontWeight = FontWeight.Bold,
            color = if (positive) DigiGreen else DigiRed
        )
    }
}
