package com.example.data.security

import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Helper for triggering the native Android hardware BiometricPrompt (Fingerprint / Face Unlock)
 * with graceful fallback to the in-app 4-digit PIN pad on unsupported or unconfigured devices.
 */
object BiometricAuthHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        }
        return try {
            val manager = context.getSystemService(BiometricManager::class.java)
            when (manager?.canAuthenticate()) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onErrorOrFallback: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onErrorOrFallback("Biometric hardware requires Android 9+; please use your 4-digit PIN.")
            return
        }
        try {
            val executor = ContextCompat.getMainExecutor(context)
            val cancellationSignal = CancellationSignal()
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Unlock My Money My Plan (MMMP) Vault")
                .setSubtitle("Verify your identity to decrypt your offline financial ledger")
                .setDescription("Use fingerprint or face recognition, or tap Use PIN to unlock with your 4-digit PIN.")
                .setNegativeButton(
                    "Use 4-Digit PIN",
                    executor,
                    DialogInterface.OnClickListener { _, _ ->
                        onErrorOrFallback("Switched to 4-digit PIN entry")
                    }
                )
                .build()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        onErrorOrFallback(errString?.toString() ?: "Use your 4-digit PIN below")
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onErrorOrFallback("Biometric not recognized. Try again or enter your 4-digit PIN.")
                    }
                }
            )
        } catch (t: Throwable) {
            onErrorOrFallback("Biometric sensor unavailable on this device. Please enter your 4-digit PIN.")
        }
    }
}
