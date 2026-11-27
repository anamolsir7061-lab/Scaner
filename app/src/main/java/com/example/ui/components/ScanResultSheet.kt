package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.analyzer.InspectionReport
import com.example.analyzer.ParsedPayload
import com.example.analyzer.ThreatLevel
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun ScanResultSheet(
    report: InspectionReport,
    onDismiss: () -> Unit,
    onInspectHex: (raw: String, format: String) -> Unit
) {
    val context = LocalContext.current
    var pendingUnsafeUrl by remember { mutableStateOf<String?>(null) }

    val threatColor = when (report.threatLevel) {
        ThreatLevel.SAFE -> StatusSuccess
        ThreatLevel.WARNING -> StatusWarning
        ThreatLevel.DANGER -> StatusDanger
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_result_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top bar: Format badge + Threat Pill + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = report.formatDisplayName,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = threatColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (report.threatLevel == ThreatLevel.SAFE) Icons.Rounded.Shield else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = threatColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = report.threatLevel.name,
                                color = threatColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dismiss_result_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Smart Payload Headline & Raw Content
            Text(
                text = payloadHeadline(report.payload),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = report.raw,
                fontFamily = JetBrainsMonoFont,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            )

            if (report.threatLevel != ThreatLevel.SAFE && report.securityReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.securityReasons.first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = threatColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contextual Primary Action Button
            when (val p = report.payload) {
                is ParsedPayload.Url -> {
                    Button(
                        onClick = {
                            if (report.threatLevel != ThreatLevel.SAFE) {
                                pendingUnsafeUrl = p.url
                            } else {
                                openUriSafely(context, p.url)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (report.threatLevel == ThreatLevel.SAFE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                threatColor
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_open_url")
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_open_link))
                    }
                }

                is ParsedPayload.WiFi -> {
                    Button(
                        onClick = { connectToWifi(context, p) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_connect_wifi")
                    ) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_connect_wifi))
                    }
                }

                is ParsedPayload.VCard -> {
                    Button(
                        onClick = { saveContact(context, p) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_save_contact")
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save_contact))
                    }
                }

                is ParsedPayload.Upi -> {
                    Button(
                        onClick = { openUriSafely(context, report.raw) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_pay_upi))
                    }
                }

                is ParsedPayload.Phone -> {
                    Button(
                        onClick = { openUriSafely(context, "tel:${p.number}") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_call_number))
                    }
                }

                is ParsedPayload.Sms -> {
                    Button(
                        onClick = {
                            val uri = "smsto:${p.number}"
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uri)).apply {
                                if (!p.message.isNullOrBlank()) putExtra("sms_body", p.message)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_send_sms))
                    }
                }

                is ParsedPayload.Email -> {
                    Button(
                        onClick = { openUriSafely(context, "mailto:${p.address}") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_send_email))
                    }
                }

                is ParsedPayload.Geo -> {
                    Button(
                        onClick = { openUriSafely(context, "geo:${p.latitude},${p.longitude}") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_open_location))
                    }
                }

                is ParsedPayload.Text -> Unit
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Universal Action Row: Copy, Share, Inspect Hex
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Scanned Code", report.raw))
                        Toast.makeText(context, context.getString(R.string.history_copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_copy_result")
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.copy), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, report.raw)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_file)))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_share_result")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.share_file), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onInspectHex(report.raw, report.formatDisplayName) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_inspect_hex")
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_hex), fontSize = 12.sp)
                }
            }
        }
    }

    pendingUnsafeUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingUnsafeUrl = null },
            title = { Text(stringResource(R.string.unsafe_url_title)) },
            text = {
                Text(
                    text = "${report.securityReasons.joinToString("\n")}\n\n$url"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingUnsafeUrl = null
                        openUriSafely(context, url)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = threatColor)
                ) {
                    Text(stringResource(R.string.action_open_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnsafeUrl = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun payloadHeadline(payload: ParsedPayload): String = when (payload) {
    is ParsedPayload.Url -> "URL • ${payload.host}"
    is ParsedPayload.WiFi -> "Wi-Fi • ${payload.ssid}"
    is ParsedPayload.Upi -> "UPI Payment • ${payload.payeeName}"
    is ParsedPayload.VCard -> "Contact • ${payload.formattedName}"
    is ParsedPayload.Phone -> "Phone • ${payload.number}"
    is ParsedPayload.Sms -> "SMS • ${payload.number}"
    is ParsedPayload.Email -> "Email • ${payload.address}"
    is ParsedPayload.Geo -> "Location • ${payload.latitude}, ${payload.longitude}"
    is ParsedPayload.Text -> "Text / Raw Data"
}

private fun openUriSafely(context: Context, uriString: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun saveContact(context: Context, contact: ParsedPayload.VCard) {
    try {
        val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
            putExtra(ContactsContract.Intents.Insert.NAME, contact.formattedName)
            contact.phoneNumbers.firstOrNull()?.let {
                putExtra(ContactsContract.Intents.Insert.PHONE, it)
            }
            contact.emails.firstOrNull()?.let {
                putExtra(ContactsContract.Intents.Insert.EMAIL, it)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open Contacts app", Toast.LENGTH_SHORT).show()
    }
}

private fun connectToWifi(context: Context, wifi: ParsedPayload.WiFi) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    if (wifi.password.isNotBlank()) {
        cm.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", wifi.password))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(wifi.ssid)
                .apply {
                    if (wifi.password.isNotBlank() && !wifi.authType.equals("NONE", true)) {
                        setWpa2Passphrase(wifi.password)
                    }
                }
                .build()
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.addNetworkSuggestions(listOf(suggestion))
        } catch (_: Exception) {}
        try {
            context.startActivity(Intent(Settings.Panel.ACTION_WIFI))
            return
        } catch (_: Exception) {}
    }
    Toast.makeText(context, "Wi-Fi password copied for ${wifi.ssid}", Toast.LENGTH_SHORT).show()
}
