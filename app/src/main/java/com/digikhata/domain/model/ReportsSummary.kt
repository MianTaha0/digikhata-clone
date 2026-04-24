package com.digikhata.domain.model

import com.digikhata.data.entity.Client

data class ReportsSummary(
    val salesThisMonth: Double,
    val expensesThisMonth: Double,
    val cashInHand: Double,
    val topClients: List<TopClient>,
    val last30DaysSales: List<Double>
) {
    companion object {
        val EMPTY = ReportsSummary(
            salesThisMonth = 0.0,
            expensesThisMonth = 0.0,
            cashInHand = 0.0,
            topClients = emptyList(),
            last30DaysSales = List(30) { 0.0 }
        )
    }
}

data class TopClient(
    val client: Client,
    val balance: Double
)
