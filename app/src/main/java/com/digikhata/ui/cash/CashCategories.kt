package com.digikhata.ui.cash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

data class CashCategory(val key: String, val label: String, val icon: ImageVector)

object CashCategories {
    val all: List<CashCategory> = listOf(
        CashCategory("sales", "Sales", Icons.Default.ShoppingCart),
        CashCategory("purchase", "Purchase", Icons.Default.Inventory2),
        CashCategory("salary", "Salary", Icons.Default.Badge),
        CashCategory("rent", "Rent", Icons.Default.Home),
        CashCategory("utilities", "Utilities", Icons.Default.Bolt),
        CashCategory("transport", "Transport", Icons.Default.LocalShipping),
        CashCategory("food", "Food", Icons.Default.Restaurant),
        CashCategory("maintenance", "Maintenance", Icons.Default.Build),
        CashCategory("loan", "Loan", Icons.Default.AccountBalance),
        CashCategory("other", "Other", Icons.Default.MoreHoriz),
    )

    private val byKey: Map<String, CashCategory> = all.associateBy { it.key }

    fun labelOf(key: String): String = byKey[key]?.label ?: "Other"
    fun iconOf(key: String): ImageVector = byKey[key]?.icon ?: Icons.Default.MoreHoriz
}
