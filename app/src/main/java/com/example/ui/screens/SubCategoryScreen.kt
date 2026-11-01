package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.MonospaceStatStyle
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.StatNegative
import com.example.ui.theme.StatPositive
import com.example.ui.theme.StatWarning
import com.example.ui.viewmodel.DrawerDestination
import com.example.ui.viewmodel.SubCategoryItem

@Composable
fun SubCategoryScreen(
    destination: DrawerDestination,
    worldState: WorldState?,
    subCategories: List<SubCategoryItem>,
    selectedSubCategory: SubCategoryItem?,
    onSelectSubCategory: (SubCategoryItem?) -> Unit,
    onExecuteActionFromDossier: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = selectedSubCategory != null) {
        onSelectSubCategory(null)
    }

    val categoryTitle = when (destination) {
        DrawerDestination.PERSONAL -> "PERSONAL DOSSIER"
        DrawerDestination.POLITICS -> "POLITICAL APPARATUS"
        DrawerDestination.NATION -> "NATIONAL INDICATORS"
        else -> "INTELLIGENCE DOMAIN"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .padding(16.dp)
            .testTag("subcategory_screen")
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (selectedSubCategory != null) {
                IconButton(
                    onClick = { onSelectSubCategory(null) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SovereignGold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column {
                Text(
                    text = if (selectedSubCategory != null) selectedSubCategory.title else categoryTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = SovereignGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedSubCategory != null) "BRIEFING PANE · CLASSIFIED" else "SELECT SUB-CATEGORY FOR BRIEFING",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SlateTextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedSubCategory == null) {
            // List of sub-categories with large numerals and trend arrows
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                subCategories.forEach { item ->
                    SubCategoryCard(
                        item = item,
                        onClick = { onSelectSubCategory(item) }
                    )
                }
            }
        } else {
            // Detailed SubCategory Dossier Pane
            DossierDetailPane(
                item = selectedSubCategory,
                onExecuteAction = onExecuteActionFromDossier,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SubCategoryCard(
    item: SubCategoryItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CharcoalSurface)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("subcategory_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SlateTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.secondaryStat,
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Large Numeral and Trend Arrow
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.primaryStat,
                        style = MonospaceStatStyle.copy(fontSize = 20.sp),
                        color = if (item.isPositiveTrend) StatPositive else StatNegative,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val trendIcon = when (item.trend) {
                    "UP" -> Icons.Default.TrendingUp
                    "DOWN" -> Icons.Default.TrendingDown
                    else -> Icons.Default.TrendingFlat
                }
                val trendColor = if (item.isPositiveTrend) StatPositive else StatNegative

                Icon(
                    imageVector = trendIcon,
                    contentDescription = item.trend,
                    tint = trendColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DossierDetailPane(
    item: SubCategoryItem,
    onExecuteAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // High Level Metric Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CharcoalCard)
                .border(1.dp, SovereignGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PRIMARY METRIC",
                        style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SovereignGold)
                    )
                    Text(
                        text = item.primaryStat,
                        style = MonospaceStatStyle.copy(fontSize = 26.sp),
                        color = if (item.isPositiveTrend) StatPositive else StatNegative
                    )
                    Text(
                        text = item.secondaryStat,
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TREND: ${item.trend}",
                        style = MonospaceStatStyle.copy(
                            fontSize = 11.sp,
                            color = if (item.isPositiveTrend) StatPositive else StatWarning
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Advisor Intelligence Memo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CharcoalSurface)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "ADVISOR DISPATCH & SITUATION MEMO",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SovereignGold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.advisorMemo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextPrimary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Detailed Metrics Table
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CharcoalSurface)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "DETAILED STATISTICAL BREAKDOWN",
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = Color(0xFF90A4AE))
                )
                Spacer(modifier = Modifier.height(8.dp))
                item.detailedMetrics.forEachIndexed { index, (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = SlateTextSecondary
                        )
                        Text(
                            text = value,
                            style = MonospaceStatStyle.copy(fontSize = 12.sp, color = SlateTextPrimary)
                        )
                    }
                    if (index < item.detailedMetrics.size - 1) {
                        Divider(color = CharcoalBorder, thickness = 0.5.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommended Executive Directives
        Text(
            text = "RECOMMENDED DIRECTIVES:",
            style = MonospaceStatStyle.copy(fontSize = 11.sp, color = SovereignGold),
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.recommendedActions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        .clickable { onExecuteAction(action) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "▶  $action",
                        fontSize = 12.sp,
                        color = SlateTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Execute directive",
                        tint = SovereignGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
