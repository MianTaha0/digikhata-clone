package com.digikhata.ui.staff

import androidx.lifecycle.ViewModel
import com.digikhata.data.entity.Staff
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StaffSheetViewModel @Inject constructor(
    private val repo: DigiRepository
) : ViewModel() {

    suspend fun save(staff: Staff, imagePath: String?): Long =
        repo.addStaff(staff, imagePath)

    suspend fun update(staff: Staff) {
        repo.updateStaff(staff)
    }
}
