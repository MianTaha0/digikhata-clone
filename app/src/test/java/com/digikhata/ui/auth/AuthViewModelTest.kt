package com.digikhata.ui.auth

import android.app.Activity
import com.digikhata.data.auth.AuthRepository
import com.digikhata.data.auth.DigiUser
import com.digikhata.data.auth.OtpSendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher(TestScope().testScheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- Phone validation ----

    @Test
    fun `isValidE164 rejects empty string`() {
        assertFalse(AuthViewModel.isValidE164(""))
    }

    @Test
    fun `isValidE164 rejects missing plus`() {
        assertFalse(AuthViewModel.isValidE164("919876543210"))
    }

    @Test
    fun `isValidE164 rejects too-short number`() {
        assertFalse(AuthViewModel.isValidE164("+123"))
    }

    @Test
    fun `isValidE164 rejects non-digit characters`() {
        assertFalse(AuthViewModel.isValidE164("+9198abc12345"))
    }

    @Test
    fun `isValidE164 accepts well-formed Indian number`() {
        assertTrue(AuthViewModel.isValidE164("+919876543210"))
    }

    @Test
    fun `isValidE164 accepts 15-digit max`() {
        assertTrue(AuthViewModel.isValidE164("+123456789012345"))
    }

    @Test
    fun `isValidE164 rejects 16-digit too-long`() {
        assertFalse(AuthViewModel.isValidE164("+1234567890123456"))
    }

    // ---- State transitions ----

    @Test
    fun `sendOtp invalid phone transitions to Error without calling repo`() = runTest(dispatcher) {
        val repo = FakeAuthRepo()
        val vm = AuthViewModel(repo)
        val activity = Mockito.mock(Activity::class.java)

        vm.sendOtp("12345", activity)
        advanceUntilIdle()

        assertTrue(vm.state.value is AuthUiState.Error)
        assertEquals(0, repo.sendOtpCalls)
    }

    @Test
    fun `sendOtp success transitions Idle to SendingOtp to AwaitingCode`() = runTest(dispatcher) {
        val repo = FakeAuthRepo()
        repo.sendOtpResult = OtpSendResult.CodeSent("verid-1", null)
        val vm = AuthViewModel(repo)
        val activity = Mockito.mock(Activity::class.java)

        assertTrue(vm.state.value is AuthUiState.Idle)
        vm.sendOtp("+919876543210", activity)
        // SendingOtp should be observable synchronously before the coroutine resumes
        assertTrue(vm.state.value is AuthUiState.SendingOtp)
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue("expected AwaitingCode, got $s", s is AuthUiState.AwaitingCode)
        assertEquals("verid-1", (s as AuthUiState.AwaitingCode).verificationId)
        assertEquals("+919876543210", s.phone)
    }

    @Test
    fun `sendOtp error result transitions SendingOtp to Error`() = runTest(dispatcher) {
        val repo = FakeAuthRepo()
        repo.sendOtpResult = OtpSendResult.Error("boom")
        val vm = AuthViewModel(repo)
        val activity = Mockito.mock(Activity::class.java)

        vm.sendOtp("+919876543210", activity)
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s is AuthUiState.Error)
        assertEquals("boom", (s as AuthUiState.Error).message)
    }

    @Test
    fun `resend without prior AwaitingCode transitions to Error`() = runTest(dispatcher) {
        val repo = FakeAuthRepo()
        val vm = AuthViewModel(repo)
        val activity = Mockito.mock(Activity::class.java)

        vm.resend(activity)
        advanceUntilIdle()

        assertTrue(vm.state.value is AuthUiState.Error)
        assertEquals(0, repo.resendCalls)
    }

    @Test
    fun `resend after AwaitingCode calls repo and returns to AwaitingCode`() = runTest(dispatcher) {
        val repo = FakeAuthRepo()
        repo.sendOtpResult = OtpSendResult.CodeSent("verid-1", null)
        val vm = AuthViewModel(repo)
        val activity = Mockito.mock(Activity::class.java)

        vm.sendOtp("+919876543210", activity)
        advanceUntilIdle()
        assertTrue(vm.state.value is AuthUiState.AwaitingCode)

        repo.resendResult = OtpSendResult.CodeSent("verid-2", null)
        vm.resend(activity)
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s is AuthUiState.AwaitingCode)
        assertEquals("verid-2", (s as AuthUiState.AwaitingCode).verificationId)
        assertEquals(1, repo.resendCalls)
    }

    @Test
    fun `verifyCode without AwaitingCode transitions to Error`() = runTest(dispatcher) {
        val repo = FakeAuthRepo()
        val vm = AuthViewModel(repo)
        vm.verifyCode("123456")
        advanceUntilIdle()
        assertTrue(vm.state.value is AuthUiState.Error)
    }

    @Test
    fun `reset returns state to Idle`() = runTest(dispatcher) {
        val repo = FakeAuthRepo()
        val vm = AuthViewModel(repo)
        vm.verifyCode("123456") // sets Error
        advanceUntilIdle()
        vm.reset()
        assertTrue(vm.state.value is AuthUiState.Idle)
    }

    private class FakeAuthRepo : AuthRepository {
        override val currentUser: StateFlow<DigiUser?> = MutableStateFlow(null)
        var sendOtpResult: OtpSendResult = OtpSendResult.Error("unset")
        var resendResult: OtpSendResult = OtpSendResult.Error("unset")
        var sendOtpCalls = 0
        var resendCalls = 0

        override suspend fun sendOtp(phoneE164: String, activity: Activity): OtpSendResult {
            sendOtpCalls += 1
            return sendOtpResult
        }

        override suspend fun verifyOtp(verificationId: String, code: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun resendOtp(
            phoneE164: String,
            activity: Activity,
            resendToken: Any?
        ): OtpSendResult {
            resendCalls += 1
            return resendResult
        }

        override fun signOut() {}
    }
}
