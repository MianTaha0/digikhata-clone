package com.digikhata.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.data.auth.AuthRepository
import com.digikhata.data.auth.OtpSendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object SendingOtp : AuthUiState()
    data class AwaitingCode(
        val verificationId: String,
        val phone: String,
        val resendToken: Any?
    ) : AuthUiState()
    data object Verifying : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data object Success : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun sendOtp(phoneE164: String, activity: Activity) {
        val normalized = phoneE164.trim()
        if (!isValidE164(normalized)) {
            _state.value = AuthUiState.Error("Enter a valid phone number in E.164 format (e.g. +919876543210)")
            return
        }
        _state.value = AuthUiState.SendingOtp
        viewModelScope.launch {
            when (val r = repo.sendOtp(normalized, activity)) {
                is OtpSendResult.CodeSent ->
                    _state.value = AuthUiState.AwaitingCode(r.verificationId, normalized, r.resendToken)
                is OtpSendResult.AutoVerified ->
                    _state.value = if (r.success) AuthUiState.Success else AuthUiState.Error("Auto-verification failed")
                is OtpSendResult.Error ->
                    _state.value = AuthUiState.Error(r.message)
            }
        }
    }

    fun verifyCode(code: String) {
        val s = _state.value
        if (s !is AuthUiState.AwaitingCode) {
            _state.value = AuthUiState.Error("No pending verification")
            return
        }
        if (code.length !in 4..8 || !code.all { it.isDigit() }) {
            _state.value = AuthUiState.Error("Enter the 6-digit code")
            return
        }
        _state.value = AuthUiState.Verifying
        viewModelScope.launch {
            val result = repo.verifyOtp(s.verificationId, code)
            _state.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Verification failed")
            }
        }
    }

    fun resend(activity: Activity) {
        val s = _state.value
        if (s !is AuthUiState.AwaitingCode) {
            _state.value = AuthUiState.Error("Nothing to resend")
            return
        }
        val phone = s.phone
        val token = s.resendToken
        _state.value = AuthUiState.SendingOtp
        viewModelScope.launch {
            when (val r = repo.resendOtp(phone, activity, token)) {
                is OtpSendResult.CodeSent ->
                    _state.value = AuthUiState.AwaitingCode(r.verificationId, phone, r.resendToken)
                is OtpSendResult.AutoVerified ->
                    _state.value = if (r.success) AuthUiState.Success else AuthUiState.Error("Auto-verification failed")
                is OtpSendResult.Error ->
                    _state.value = AuthUiState.Error(r.message)
            }
        }
    }

    fun reset() {
        _state.value = AuthUiState.Idle
    }

    fun dismissError() {
        if (_state.value is AuthUiState.Error) _state.value = AuthUiState.Idle
    }

    companion object {
        fun isValidE164(phone: String): Boolean {
            if (!phone.startsWith("+")) return false
            val digits = phone.drop(1)
            if (digits.isEmpty()) return false
            if (!digits.all { it.isDigit() }) return false
            return digits.length in 10..15
        }
    }
}
