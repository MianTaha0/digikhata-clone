package com.digikhata.ui.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digikhata.domain.model.InvoiceStatus
import com.digikhata.ui.theme.DigiGreen

@Composable
fun StatusBadge(status: InvoiceStatus, modifier: Modifier = Modifier) {
    val (bg, label) = when (status) {
        InvoiceStatus.PENDING -> Color(0xFFF59E0B) to "Pending"
        InvoiceStatus.PARTIAL -> Color(0xFF3B82F6) to "Partial"
        InvoiceStatus.PAID -> DigiGreen to "Paid"
    }
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = bg,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.labelSmall
    )
}
