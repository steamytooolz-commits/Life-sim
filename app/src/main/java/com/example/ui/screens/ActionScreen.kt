package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorldState
import com.example.engine.orchestrator.TurnState
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.MonospaceStatStyle
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.StatPositive
import com.example.ui.theme.StatWarning

@Composable
fun ActionScreen(
    worldState: WorldState?,
    turnState: TurnState,
    actionText: String,
    quickActions: List<String>,
    onActionTextChanged: (String) -> Unit,
    onRunTurn: () -> Unit,
    onSelectQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = worldState ?: WorldState()
    val isBusy = turnState is TurnState.Generating || turnState is TurnState.ExecutingTools

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .testTag("action_screen")
    ) {
        // Main Scrollable Dossier Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Active Crisis Banner (if applicable)
            if (!state.activeCrisis.isNullOrBlank()) {
                CrisisAlertBanner(crisisText = state.activeCrisis)
                Spacer(modifier = Modifier.height(14.dp))
            }

            // State Headline Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CharcoalCard)
                    .border(1.dp, SovereignGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SovereignGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STATE PRESS WIRE & INTELLIGENCE DISPATCH",
                            style = MonospaceStatStyle.copy(
                                fontSize = 10.sp,
                                color = SovereignGold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${state.latestHeadline}”",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Narrative Briefing Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                    .padding(16.dp)
                    .testTag("narrative_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONSEQUENCE NARRATIVE · WEEK ${state.currentWeek}",
                            style = MonospaceStatStyle.copy(
                                fontSize = 11.sp,
                                color = SovereignGold
                            )
                        )

                        if (isBusy) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = SovereignGold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (turnState is TurnState.ExecutingTools) "EXECUTING TOOLS" else "REFEREE INFERENCE",
                                    style = MonospaceStatStyle.copy(fontSize = 9.sp, color = SovereignGold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text display based on turnState
                    val displayedNarrative = when (turnState) {
                        is TurnState.Generating -> {
                            val text = turnState.streamedText
                            if (text.isBlank()) "Game Master is analyzing your action..." else text
                        }
                        is TurnState.ExecutingTools -> {
                            "Resolving tool ${turnState.currentTool} (${turnState.completed + 1}/${turnState.toolCount})..."
                        }
                        is TurnState.Completed -> turnState.narrative
                        is TurnState.Error -> "Transmission Interrupted: ${turnState.message}"
                        else -> state.latestNarrative
                    }

                    Text(
                        text = displayedNarrative,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SlateTextPrimary,
                        lineHeight = 24.sp
                    )

                    // If turn completed with tool reports, show badges
                    if (turnState is TurnState.Completed && turnState.toolReports.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "RESOLVED SYSTEM ACTIONS:",
                            style = MonospaceStatStyle.copy(fontSize = 10.sp, color = Color(0xFF90A4AE))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            turnState.toolReports.forEach { report ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CharcoalCard)
                                        .border(0.5.dp, CharcoalBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "⚙ <function=${report.toolCall.name}> → ${report.resultString}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (report.isSuccess) StatPositive else StatWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Bottom Action Dock
        ActionBottomDock(
            actionText = actionText,
            quickActions = quickActions,
            isBusy = isBusy,
            onActionTextChanged = onActionTextChanged,
            onRunTurn = onRunTurn,
            onSelectQuickAction = onSelectQuickAction
        )
    }
}

@Composable
private fun CrisisAlertBanner(crisisText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF330C0D))
            .border(1.dp, CrimsonAccent, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Crisis Alert",
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "NATIONAL CRISIS IN EFFECT",
                    style = MonospaceStatStyle.copy(fontSize = 11.sp, color = Color(0xFFFF5252))
                )
                Text(
                    text = crisisText,
                    fontSize = 13.sp,
                    color = Color(0xFFFFD1D1),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ActionBottomDock(
    actionText: String,
    quickActions: List<String>,
    isBusy: Boolean,
    onActionTextChanged: (String) -> Unit,
    onRunTurn: () -> Unit,
    onSelectQuickAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalSurface)
            .border(1.dp, CharcoalBorder)
            .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
    ) {
        // Quick Action Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickActions.forEach { chipText ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = !isBusy) {
                            onSelectQuickAction(chipText)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("chip_${chipText.take(10)}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SovereignGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = chipText,
                            fontSize = 11.sp,
                            color = SlateTextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Text Field & Run Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = actionText,
                onValueChange = onActionTextChanged,
                placeholder = {
                    Text(
                        text = "What do you do? (e.g., Hold rally in Oakhaven, negotiate with unions...)",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                },
                enabled = !isBusy,
                trailingIcon = {
                    if (actionText.isNotEmpty() && !isBusy) {
                        IconButton(onClick = { onActionTextChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_text_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CharcoalCard,
                    unfocusedContainerColor = CharcoalCard,
                    disabledContainerColor = CharcoalCard,
                    focusedBorderColor = SovereignGold,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = SlateTextPrimary,
                    unfocusedTextColor = SlateTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRunTurn,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SovereignGold,
                    contentColor = Color(0xFF141000),
                    disabledContainerColor = CharcoalBorder,
                    disabledContentColor = SlateTextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("run_week_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF141000)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Run Week",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Run Week",
                            style = MonospaceStatStyle.copy(fontSize = 12.sp)
                        )
                    }
                }
            }
        }
    }
}
