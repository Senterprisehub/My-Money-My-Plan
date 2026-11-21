package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.security.BiometricAuthHelper
import com.example.ui.theme.ExpenseCoral

@Composable
fun LockScreen(
    pinError: String?,
    onBiometricSuccess: () -> Unit,
    onSubmitPin: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var biometricPromptInfo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (BiometricAuthHelper.isBiometricAvailable(context)) {
            BiometricAuthHelper.authenticate(
                context = context,
                onSuccess = onBiometricSuccess,
                onErrorOrFallback = { msg -> biometricPromptInfo = msg }
            )
        }
    }

    fun appendDigit(digit: String) {
        if (enteredPin.length < 4) {
            val next = enteredPin + digit
            enteredPin = next
            if (next.length == 4) {
                val ok = onSubmitPin(next)
                if (!ok) {
                    enteredPin = ""
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("biometric_pin_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "My Money My Plan (MMMP) Vault Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Authenticate with Biometrics or enter your 4-digit PIN (Default: 1234) to decrypt your offline ledger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 4-Digit PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                if (pinError != null || biometricPromptInfo != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pinError ?: biometricPromptInfo.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (pinError != null) ExpenseCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Numeric Keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowDigits ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowDigits.forEach { digit ->
                                PinKeyButton(
                                    label = digit,
                                    onClick = { appendDigit(digit) },
                                    modifier = Modifier.testTag("pin_key_$digit")
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                BiometricAuthHelper.authenticate(
                                    context = context,
                                    onSuccess = onBiometricSuccess,
                                    onErrorOrFallback = { msg -> biometricPromptInfo = msg }
                                )
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("trigger_biometric_button"),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Unlock with Biometrics"
                            )
                        }

                        PinKeyButton(
                            label = "0",
                            onClick = { appendDigit("0") },
                            modifier = Modifier.testTag("pin_key_0")
                        )

                        FilledTonalButton(
                            onClick = {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("pin_key_backspace"),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        BiometricAuthHelper.authenticate(
                            context = context,
                            onSuccess = onBiometricSuccess,
                            onErrorOrFallback = { msg -> biometricPromptInfo = msg }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authenticate with Biometric Sensor")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AES-256-GCM Hardware Keystore Encrypted • 100% Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = CircleShape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
