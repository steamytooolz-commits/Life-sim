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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.ChronicleEntry
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.MonospaceStatStyle
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.StatPositive
import com.example.ui.theme.StatWarning

@Composable
fun ChronicleScreen(
    entries: List<ChronicleEntry>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredEntries = remember(entries, selectedFilter) {
        if (selectedFilter == "ALL") {
            entries
        } else {
            entries.filter { it.category.equals(selectedFilter, ignoreCase = true) }
        }
    }

    val filterOptions = listOf(
        "ALL" to "All Records",
        "NARRATIVE" to "Briefings",
        "ACTION" to "Actions",
        "TOOL_CALL" to "Tool Calls",
        "STAT_DELTA" to "Stat Changes",
        "DICE_ROLL" to "Dice Checks",
        "EVENT" to "Events & Crises"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .padding(16.dp)
            .testTag("chronicle_screen")
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "THE CHRONICLE",
                    style = MaterialTheme.typography.titleLarge,
                    color = SovereignGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "IMMUTABLE TIMELINE & TELEMETRY LOG",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SlateTextSecondary)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CharcoalCard)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${entries.size} LOGS",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SovereignGold)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SovereignGold else CharcoalCard)
                        .border(1.dp, if (isSelected) SovereignGold else CharcoalBorder, RoundedCornerShape(12.dp))
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) Color(0xFF141000) else SlateTextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Entries List
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log records found for category '$selectedFilter'.",
                    style = MonospaceStatStyle.copy(fontSize = 12.sp, color = SlateTextSecondary)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    ChronicleItemCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun ChronicleItemCard(entry: ChronicleEntry) {
    var isExpanded by remember { mutableStateOf(false) }

    val categoryColor = when (entry.category) {
        "TOOL_CALL" -> Color(0xFF48CAE4)
        "STAT_DELTA" -> Color(0xFF9D4EDD)
        "DICE_ROLL" -> SovereignGold
        "EVENT", "CRISIS" -> Color(0xFFFF5252)
        "ACTION" -> Color(0xFF80ED99)
        else -> SlateTextSecondary
    }

    val severityColor = when (entry.severity) {
        "SUCCESS" -> StatPositive
        "WARNING" -> StatWarning
        "CRITICAL" -> CrimsonLight
        else -> Color(0xFF607D8B)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CharcoalSurface)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp)
            .testTag("chronicle_item_${entry.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Week / Year Tag
                    Text(
                        text = "WK ${entry.week}, ${entry.year}",
                        style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SovereignGold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .border(0.5.dp, categoryColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = entry.category,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Severity Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.severity,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = severityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = SlateTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.details,
                style = if (entry.category == "TOOL_CALL") MonospaceStatStyle.copy(fontSize = 11.sp, color = Color(0xFFB0BEC5))
                else MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = SlateTextSecondary),
                maxLines = if (isExpanded) Int.MAX_VALUE else 3
            )

            if (!isExpanded && entry.details.length > 120) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "[Tap to expand full telemetry]",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SovereignGold
                )
            }
        }
    }
}
