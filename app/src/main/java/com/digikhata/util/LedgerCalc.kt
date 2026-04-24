package com.digikhata.util

import com.digikhata.data.entity.TxEntity

/**
 * Pure ledger math — no Android deps so it's trivially unit-testable.
 *
 * Convention (matches TransactionDao.balanceForClient): type 0 ("you gave") adds,
 * type 1 ("you got") subtracts.
 */
object LedgerCalc {

    /**
     * Run a running balance over [transactions] in the order given, starting from [openingBalance].
     * Returns the balance AFTER each transaction.
     */
    fun computeRunningBalance(openingBalance: Double, transactions: List<TxEntity>): List<Double> {
        var running = openingBalance
        val out = ArrayList<Double>(transactions.size)
        for (t in transactions) {
            running += if (t.type == 0) t.amount else -t.amount
            out.add(running)
        }
        return out
    }
}
