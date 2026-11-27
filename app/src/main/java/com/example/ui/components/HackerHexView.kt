package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MonospaceFont
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SolarAmber

enum class HexInspectorTab {
    HEX_DUMP,
    ENCODINGS,
    DECIMAL_BYTES,
    BINARY_BITS,
    ECC_CHECKSUM
}

@Composable
fun HackerHexView(
    hexDump: String,
    byteCount: Int,
    charSet: String,
    utf8Decoded: String = "",
    iso8859Decoded: String = "",
    asciiDecoded: String = "",
    decimalBytes: String = "",
    bitStream: String = "",
    errorCorrectionInfo: String = "",
    checksumStatus: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(HexInspectorTab.HEX_DUMP) }
    val scrollState = rememberScrollState()

    val currentContentToCopy = when (currentTab) {
        HexInspectorTab.HEX_DUMP -> hexDump
        HexInspectorTab.ENCODINGS -> "UTF-8:\n$utf8Decoded\n\nISO-8859-1:\n$iso8859Decoded\n\nASCII:\n$asciiDecoded"
        HexInspectorTab.DECIMAL_BYTES -> decimalBytes
        HexInspectorTab.BINARY_BITS -> bitStream
        HexInspectorTab.ECC_CHECKSUM -> "ECC: $errorCorrectionInfo\nChecksum: $checksumStatus"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledBlack, RoundedCornerShape(12.dp))
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SUPER-USER RAW BYTES & ENCODING INSPECTOR",
                    color = MatrixGreen,
                    fontFamily = MonospaceFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = "$byteCount BYTES • $charSet",
                    color = CyberTextSecondary,
                    fontFamily = MonospaceFont,
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Inspector Data", currentContentToCopy))
                    Toast.makeText(context, "Inspector Data Copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentTab == HexInspectorTab.HEX_DUMP,
                onClick = { currentTab = HexInspectorTab.HEX_DUMP },
                label = { Text("HEX DUMP", fontFamily = MonospaceFont, fontSize = 9.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MatrixGreen,
                    selectedLabelColor = Color.Black
                )
            )

            FilterChip(
                selected = currentTab == HexInspectorTab.ENCODINGS,
                onClick = { currentTab = HexInspectorTab.ENCODINGS },
                label = { Text("CHARSETS", fontFamily = MonospaceFont, fontSize = 9.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan,
                    selectedLabelColor = Color.Black
                )
            )

            FilterChip(
                selected = currentTab == HexInspectorTab.DECIMAL_BYTES,
                onClick = { currentTab = HexInspectorTab.DECIMAL_BYTES },
                label = { Text("DECIMAL BYTES", fontFamily = MonospaceFont, fontSize = 9.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolarAmber,
                    selectedLabelColor = Color.Black
                )
            )

            FilterChip(
                selected = currentTab == HexInspectorTab.BINARY_BITS,
                onClick = { currentTab = HexInspectorTab.BINARY_BITS },
                label = { Text("BINARY BITS", fontFamily = MonospaceFont, fontSize = 9.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFBD00FF),
                    selectedLabelColor = Color.White
                )
            )

            FilterChip(
                selected = currentTab == HexInspectorTab.ECC_CHECKSUM,
                onClick = { currentTab = HexInspectorTab.ECC_CHECKSUM },
                label = { Text("ECC & CHECKSUM", fontFamily = MonospaceFont, fontSize = 9.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00E5FF),
                    selectedLabelColor = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF141F30), RoundedCornerShape(8.dp))
                .padding(12.dp)
                .horizontalScroll(scrollState)
        ) {
            when (currentTab) {
                HexInspectorTab.HEX_DUMP -> {
                    Text(
                        text = hexDump,
                        color = Color(0xFF38EF7D),
                        fontFamily = MonospaceFont,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                HexInspectorTab.ENCODINGS -> {
                    Column {
                        Text(text = "▶ UTF-8 (STANDARD UNICODE):", color = NeonCyan, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = utf8Decoded.ifBlank { "(empty)" }, color = Color.White, fontFamily = MonospaceFont, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "▶ ISO-8859-1 (LATIN-1):", color = MatrixGreen, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = iso8859Decoded.ifBlank { "(empty)" }, color = CyberTextPrimary, fontFamily = MonospaceFont, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "▶ US-ASCII (7-BIT CLEAN):", color = SolarAmber, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = asciiDecoded.ifBlank { "(empty)" }, color = CyberTextSecondary, fontFamily = MonospaceFont, fontSize = 11.sp)
                    }
                }

                HexInspectorTab.DECIMAL_BYTES -> {
                    Column {
                        Text(text = "RAW 8-BIT UNSIGNED BYTE ARRAY [0..255]:", color = SolarAmber, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = decimalBytes,
                            color = Color(0xFFFFD54F),
                            fontFamily = MonospaceFont,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                HexInspectorTab.BINARY_BITS -> {
                    Column {
                        Text(text = "RAW BINARY BITSTREAM (MSB FIRST, 8 BITS/BYTE):", color = Color(0xFFBD00FF), fontFamily = MonospaceFont, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bitStream,
                            color = Color(0xFFE1BEE7),
                            fontFamily = MonospaceFont,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                HexInspectorTab.ECC_CHECKSUM -> {
                    Column {
                        Text(text = "ERROR CORRECTION CODE (ECC) LEVEL:", color = NeonCyan, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = errorCorrectionInfo, color = Color.White, fontFamily = MonospaceFont, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "CHECKSUM INTEGRITY VERIFICATION:", color = MatrixGreen, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = checksumStatus, color = Color(0xFF38EF7D), fontFamily = MonospaceFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
