package com.digikhata.ui.auth

import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.ui.components.digiTopBarColors
import com.digikhata.ui.theme.DigiRed
import kotlinx.coroutines.delay

private data class CountryCode(val code: String, val label: String)

private val countryCodes = listOf(
    CountryCode("+91", "India"),
    CountryCode("+1", "USA"),
    CountryCode("+44", "UK"),
    CountryCode("+92", "Pakistan"),
    CountryCode("+971", "UAE"),
    CountryCode("+966", "Saudi Arabia"),
    CountryCode("+61", "Australia"),
    CountryCode("+65", "Singapore"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val snackbar = remember { SnackbarHostState() }

    // Navigate on success
    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            onSignedIn()
        }
    }
    // Error snackbar
    LaunchedEffect(state) {
        val s = state
        if (s is AuthUiState.Error) {
            snackbar.showSnackbar(s.message)
            vm.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = digiTopBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            when (val s = state) {
                is AuthUiState.AwaitingCode -> OtpEntrySection(
                    phone = s.phone,
                    sending = false,
                    verifying = false,
                    onVerify = { code -> vm.verifyCode(code) },
                    onResend = { activity?.let { vm.resend(it) } }
                )
                AuthUiState.Verifying -> OtpEntrySection(
                    phone = (vm.state.value as? AuthUiState.AwaitingCode)?.phone ?: "",
                    sending = false,
                    verifying = true,
                    onVerify = { },
                    onResend = { }
                )
                else -> PhoneEntrySection(
                    sending = state is AuthUiState.SendingOtp,
                    onSend = { phone ->
                        activity?.let { vm.sendOtp(phone, it) }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneEntrySection(
    sending: Boolean,
    onSend: (String) -> Unit
) {
    var selectedCountry by remember { mutableStateOf(countryCodes.first()) }
    var phone by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Text(
        "Enter your phone number",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "We'll send a 6-digit verification code via SMS.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            OutlinedTextField(
                value = selectedCountry.code,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .width(110.dp),
                label = { Text("Code") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                countryCodes.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.code}  ${c.label}") },
                        onClick = {
                            selectedCountry = c
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { v -> phone = v.filter { it.isDigit() }.take(15) },
            label = { Text("Phone number") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
        )
    }

    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { onSend(selectedCountry.code + phone) },
        enabled = !sending && phone.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = DigiRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (sending) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text("Send code")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtpEntrySection(
    phone: String,
    sending: Boolean,
    verifying: Boolean,
    onVerify: (String) -> Unit,
    onResend: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(30) }

    LaunchedEffect(Unit) {
        secondsLeft = 30
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    Text(
        "Enter 6-digit code",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Sent to $phone",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))

    OutlinedTextField(
        value = code,
        onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) },
        label = { Text("Verification code") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )

    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { onVerify(code) },
        enabled = !verifying && code.length == 6,
        colors = ButtonDefaults.buttonColors(containerColor = DigiRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (verifying) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text("Verify")
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onResend,
            enabled = secondsLeft == 0 && !sending
        ) {
            Text(
                if (secondsLeft > 0) "Resend code in ${secondsLeft}s" else "Resend code",
                color = if (secondsLeft == 0) DigiRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PhoneBadgeCircle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(DigiRed.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Phone, contentDescription = null, tint = DigiRed, modifier = Modifier.size(36.dp))
    }
}
