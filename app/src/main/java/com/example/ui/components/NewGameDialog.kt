package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.CharacterOrigin
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.MonospaceStatStyle
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SovereignGold
import kotlin.random.Random

@Composable
fun NewGameDialog(
    onDismiss: () -> Unit,
    onStartGame: (CharacterOrigin, String) -> Unit
) {
    val presetOrigins = remember {
        listOf(
            CharacterOrigin(
                name = "Julian Vance",
                originType = "Working-Class Organizer",
                startingTitle = "Citizen Activist",
                age = 23,
                startingWealth = 14.5,
                startingReputation = 42.0,
                startingParty = "Democratic Reform Alliance",
                description = "Raised in the industrial district of Oakhaven. Deep labor ties and fiery oratorical skills, but virtually zero cash reserves."
            ),
            CharacterOrigin(
                name = "Eleanor Stirling",
                originType = "Dynastic Politician's Heir",
                startingTitle = "Parliamentary Aide",
                age = 26,
                startingWealth = 120.0,
                startingReputation = 65.0,
                startingParty = "National Conservative Union",
                description = "Daughter of a former Foreign Secretary. High starting wealth and establishment media contacts, accompanied by intense scrutiny."
            ),
            CharacterOrigin(
                name = "Dr. Paul Kincaid",
                originType = "Constitutional Jurist",
                startingTitle = "Legal Counsel",
                age = 29,
                startingWealth = 45.0,
                startingReputation = 55.0,
                startingParty = "Civic Justice Movement",
                description = "Acclaimed legal scholar and anti-corruption litigator with impeccable civil service goodwill and clean kompromat sheet."
            ),
            CharacterOrigin(
                name = "Victor Sterling",
                originType = "Military Veteran Commander",
                startingTitle = "Reserve Major",
                age = 31,
                startingWealth = 35.0,
                startingReputation = 60.0,
                startingParty = "Sovereign Coalition",
                description = "Former battalion officer with battle commendations. High prestige among armed forces and defense institutions."
            )
        )
    }

    var selectedOrigin by remember { mutableStateOf(presetOrigins[0]) }
    var playerName by remember { mutableStateOf(selectedOrigin.name) }
    var nationName by remember { mutableStateOf("Republic of Valeria") }

    val randomNames = listOf("Cassian Drake", "Helena Cross", "Dante Rhodes", "Valerie Hayes", "Arthur Sterling", "Miriam Holt")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("new_game_dialog"),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SovereignGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ORIGIN PROTOCOL",
                            style = MonospaceStatStyle.copy(fontSize = 11.sp, color = SovereignGold)
                        )
                        Text(
                            text = "Begin New Career",
                            style = MaterialTheme.typography.titleLarge,
                            color = SlateTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Character Name Input
                Text(
                    text = "CANDIDATE NAME",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SlateTextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CharcoalCard,
                            unfocusedContainerColor = CharcoalCard,
                            focusedBorderColor = SovereignGold,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { playerName = randomNames.random() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CharcoalCard)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = "Randomize", tint = SovereignGold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nation Name Input
                Text(
                    text = "SOVEREIGN NATION",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SlateTextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nationName,
                    onValueChange = { nationName = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CharcoalCard,
                        unfocusedContainerColor = CharcoalCard,
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Background Origin Selector
                Text(
                    text = "CHOOSE BACKGROUND ORIGIN",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SovereignGold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetOrigins.forEach { origin ->
                        val isSelected = selectedOrigin.originType == origin.originType
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CharcoalCard else CharcoalBackground)
                                .border(1.dp, if (isSelected) SovereignGold else CharcoalBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedOrigin = origin
                                    if (playerName in presetOrigins.map { it.name }) {
                                        playerName = origin.name
                                    }
                                }
                                .padding(10.dp)
                                .testTag("origin_${origin.originType.take(8)}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = origin.originType,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                        color = if (isSelected) SovereignGold else SlateTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Age ${origin.age} · $${origin.startingWealth.toInt()}k",
                                        style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SlateTextSecondary)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = origin.description,
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Confirm Button
                Button(
                    onClick = {
                        val configured = selectedOrigin.copy(name = playerName.ifBlank { "Julian Vance" })
                        onStartGame(configured, nationName.ifBlank { "Republic of Valeria" })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_new_game_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignGold,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "COMMENCE SIMULATION",
                        style = MonospaceStatStyle.copy(fontSize = 12.sp, color = Color(0xFF141000))
                    )
                }
            }
        }
    }
}
