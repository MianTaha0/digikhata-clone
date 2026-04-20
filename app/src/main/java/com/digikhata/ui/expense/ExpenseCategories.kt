package com.digikhata.ui.expense

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

data class ExpenseCategory(val key: String, val label: String, val icon: ImageVector)

object ExpenseCategories {
    val all: List<ExpenseCategory> = listOf(
        ExpenseCategory("rent", "Rent", Icons.Default.Home),
        ExpenseCategory("utilities", "Utilities", Icons.Default.Bolt),
        ExpenseCategory("salaries", "Salaries", Icons.Default.Badge),
        ExpenseCategory("supplies", "Office Supplies", Icons.Default.Inventory2),
        ExpenseCategory("travel", "Travel", Icons.Default.LocalShipping),
        ExpenseCategory("food", "Food & Meals", Icons.Default.Restaurant),
        ExpenseCategory("marketing", "Marketing", Icons.Default.Campaign),
        ExpenseCategory("repairs", "Repairs", Icons.Default.Build),
        ExpenseCategory("tax", "Tax & Fees", Icons.Default.AccountBalance),
        ExpenseCategory("other", "Other", Icons.Default.MoreHoriz),
    )

    private val byKey: Map<String, ExpenseCategory> = all.associateBy { it.key }

    fun labelOf(key: String): String = byKey[key]?.label ?: "Other"
    fun iconOf(key: String): ImageVector = byKey[key]?.icon ?: Icons.Default.MoreHoriz
}
