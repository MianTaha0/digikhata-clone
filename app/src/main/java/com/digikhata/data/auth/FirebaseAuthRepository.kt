package com.digikhata.data.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toDigi())
    override val currentUser: StateFlow<DigiUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { fa ->
            _currentUser.value = fa.currentUser?.toDigi()
        }
    }

    override suspend fun sendOtp(phoneE164: String, activity: Activity): OtpSendResult =
        verifyInternal(phoneE164, activity, null)

    override suspend fun resendOtp(
        phoneE164: String,
        activity: Activity,
        resendToken: Any?
    ): OtpSendResult =
        verifyInternal(
            phoneE164,
            activity,
            resendToken as? PhoneAuthProvider.ForceResendingToken
        )

    private suspend fun verifyInternal(
        phoneE164: String,
        activity: Activity,
        token: PhoneAuthProvider.ForceResendingToken?
    ): OtpSendResult = suspendCancellableCoroutine { cont ->
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (cont.isActive) {
                            cont.resume(OtpSendResult.AutoVerified(task.isSuccessful))
                        }
                    }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                if (cont.isActive) {
                    val msg = when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "Invalid phone number"
                        else -> e.localizedMessage ?: "Verification failed"
                    }
                    cont.resume(OtpSendResult.Error(msg))
                }
            }

            override fun onCodeSent(
                verificationId: String,
                resendToken: PhoneAuthProvider.ForceResendingToken
            ) {
                if (cont.isActive) {
                    cont.resume(OtpSendResult.CodeSent(verificationId, resendToken))
                }
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (token != null) builder.setForceResendingToken(token)

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    override suspend fun verifyOtp(verificationId: String, code: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (!cont.isActive) return@addOnCompleteListener
                        if (task.isSuccessful) {
                            cont.resume(Result.success(Unit))
                        } else {
                            cont.resume(
                                Result.failure(
                                    task.exception ?: RuntimeException("Verification failed")
                                )
                            )
                        }
                    }
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(Result.failure(t))
            }
        }

    override fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toDigi(): DigiUser = DigiUser(uid = uid, phoneNumber = phoneNumber)
