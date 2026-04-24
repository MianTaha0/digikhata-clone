package com.digikhata.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Phase 4a.3: carries a deep-link target from [com.digikhata.MainActivity] (which
 * receives the Intent) into [DigiApp] (which owns the NavController).
 *
 * Single global MutableStateFlow keeps things simple; DigiApp consumes-and-clears.
 */
object PendingNavIntent {
    /** Invoice id to open when the launcher intent carries one, else null. */
    val pendingInvoiceId = MutableStateFlow<Long?>(null)
}
