package com.digikhata.ui.invoice

import androidx.lifecycle.ViewModel
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    private val repo: DigiRepository
) : ViewModel() {
    suspend fun record(invoiceId: Long, amount: Double) {
        if (amount > 0) repo.recordPayment(invoiceId, amount)
    }
}
