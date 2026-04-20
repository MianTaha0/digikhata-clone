package com.digikhata.ui.notifications

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digikhata.data.entity.DigiNotification
import com.digikhata.ui.theme.DigiGreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.util.CurrencyUtils
import com.digikhata.util.PhoneUtils
import java.net.URLEncoder
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    name: String,
    phone: String,
    amount: Double,
    currency: String,
    onDismiss: () -> Unit,
    notifVm: NotificationsViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val defaultMsg = "Assalam-o-Alaikum $name, please clear your balance of ${CurrencyUtils.format(abs(amount), currency)}. - via DigiKhata"
    var message by remember { mutableStateOf(defaultMsg) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Send Payment Reminder", style = MaterialTheme.typography.titleLarge)
            if (phone.isBlank()) {
                Text("No phone number on file for this customer.", color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (phone.isNotBlank()) {
                            val uri = Uri.parse("smsto:${PhoneUtils.cleanPhone(phone)}")
                            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                                putExtra("sms_body", message)
                            }
                            context.startActivity(intent)
                            notifVm.add(
                                DigiNotification(
                                    clientName = name, clientPhone = phone,
                                    amount = amount, balance = amount,
                                    currency = currency, details = message
                                )
                            )
                            onDismiss()
                        }
                    },
                    enabled = phone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = DigiRed, contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) { Text("Send SMS") }

                Button(
                    onClick = {
                        if (phone.isNotBlank()) {
                            val clean = PhoneUtils.cleanPhone(phone).removePrefix("+")
                            val encoded = URLEncoder.encode(message, "UTF-8")
                            val url = "https://api.whatsapp.com/send?phone=$clean&text=$encoded"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                            notifVm.add(
                                DigiNotification(
                                    clientName = name, clientPhone = phone,
                                    amount = amount, balance = amount,
                                    currency = currency, details = message
                                )
                            )
                            onDismiss()
                        }
                    },
                    enabled = phone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = DigiGreen, contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) { Text("WhatsApp") }
            }
        }
    }
}
