package com.digikhata.data.auth

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Auth repository for Firebase Phone Auth (Phase 3b.1).
 * No data sync yet — just sign-in.
 */
interface AuthRepository {
    val currentUser: StateFlow<DigiUser?>
    suspend fun sendOtp(phoneE164: String, activity: Activity): OtpSendResult
    suspend fun verifyOtp(verificationId: String, code: String): Result<Unit>
    suspend fun resendOtp(phoneE164: String, activity: Activity, resendToken: Any?): OtpSendResult
    fun signOut()
}

data class DigiUser(
    val uid: String,
    val phoneNumber: String?
)

sealed class OtpSendResult {
    data class CodeSent(val verificationId: String, val resendToken: Any?) : OtpSendResult()
    data class AutoVerified(val success: Boolean) : OtpSendResult()
    data class Error(val message: String) : OtpSendResult()
}
