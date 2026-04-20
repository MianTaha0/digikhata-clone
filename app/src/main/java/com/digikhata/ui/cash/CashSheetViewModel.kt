package com.digikhata.ui.cash

import androidx.lifecycle.ViewModel
import com.digikhata.data.entity.CashEntry
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CashSheetViewModel @Inject constructor(
    private val repo: DigiRepository
) : ViewModel() {

    suspend fun save(entry: CashEntry): Long =
        repo.addCashEntry(entry, entry.imageLocalPath)

    suspend fun update(entry: CashEntry) {
        repo.updateCashEntry(entry)
    }
}
