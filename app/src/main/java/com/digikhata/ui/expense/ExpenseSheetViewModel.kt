package com.digikhata.ui.expense

import androidx.lifecycle.ViewModel
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExpenseSheetViewModel @Inject constructor(
    private val repo: DigiRepository
) : ViewModel() {

    suspend fun save(entry: ExpenseEntry, imagePath: String?): Long =
        repo.addExpense(entry, imagePath)

    suspend fun update(entry: ExpenseEntry) {
        repo.updateExpense(entry)
    }
}
