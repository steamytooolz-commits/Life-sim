package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.download.ModelDownloadWorker
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.MonospaceStatStyle
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.StatPositive

@Composable
fun ModelDownloadScreen(
    isDownloading: Boolean,
    progressPercent: Int,
    statusMessage: String,
    onStartDownload: (Boolean) -> Unit,
    onSkipToOfflineSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var wifiOnly by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Crest
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CharcoalCard)
                .border(2.dp, SovereignGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sovereign_crest),
                contentDescription = "Sovereign Insignia",
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SOVEREIGN",
            style = MaterialTheme.typography.headlineLarge,
            color = SovereignGold,
            letterSpacing = 2.sp
        )

        Text(
            text = "ON-DEVICE NEURAL REFEREE",
            style = MonospaceStatStyle.copy(fontSize = 12.sp, color = SlateTextSecondary)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CharcoalSurface)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = SovereignGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Liquid AI LFM3-1.7B (q4f16 ONNX)",
                        style = MonospaceStatStyle.copy(fontSize = 12.sp, color = SlateTextPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To run fully offline and private with agentic tool calls, Sovereign downloads the 1.38 GB ONNX model checkpoint and tokenizer. Once downloaded, zero network connectivity is required.",
                    fontSize = 12.sp,
                    color = SlateTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SovereignGold,
                    trackColor = CharcoalBorder
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (statusMessage.isNotBlank()) statusMessage else "Ready for download (1.38 GB)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SovereignGold
                    )
                    Text(
                        text = "$progressPercent%",
                        style = MonospaceStatStyle.copy(fontSize = 12.sp, color = SlateTextPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // WiFi Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = wifiOnly,
                onCheckedChange = { wifiOnly = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = SovereignGold,
                    checkmarkColor = Color(0xFF141000),
                    uncheckedColor = SlateTextSecondary
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = SlateTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "WiFi Only (Unmetered connection required)",
                fontSize = 12.sp,
                color = SlateTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start / Resume Download Button
        Button(
            onClick = { onStartDownload(wifiOnly) },
            enabled = !isDownloading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("start_model_download_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SovereignGold,
                contentColor = Color(0xFF141000)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF141000)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DOWNLOADING MODEL...",
                    style = MonospaceStatStyle.copy(fontSize = 12.sp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (progressPercent > 0) "RESUME DOWNLOAD" else "DOWNLOAD LFM3 MODEL (1.38 GB)",
                    style = MonospaceStatStyle.copy(fontSize = 12.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Instant Play with Built-In Agentic Simulation Button
        OutlinedButton(
            onClick = onSkipToOfflineSimulation,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("launch_simulation_referee_button"),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SovereignGold
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SovereignGold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LAUNCH INSTANT AGENTIC SIMULATION",
                style = MonospaceStatStyle.copy(fontSize = 11.sp, color = SovereignGold)
            )
        }
    }
}
