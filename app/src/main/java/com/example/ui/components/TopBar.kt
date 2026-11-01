package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorldState
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.MonospaceStatStyle
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.StatNegative
import com.example.ui.theme.StatPositive

@Composable
fun TopBar(
    worldState: WorldState?,
    onOpenDrawer: () -> Unit,
    onAdvanceWeek: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val state = worldState ?: WorldState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_bar"),
        color = CharcoalSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Row 1: Drawer Toggle, Date & Title, Next Week Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("menu_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Dossier Drawer",
                            tint = SovereignGold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.dateString.uppercase(),
                                style = MonospaceStatStyle.copy(
                                    fontSize = 12.sp,
                                    color = SovereignGold
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF202632))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = state.lifeStage,
                                    fontSize = 10.sp,
                                    color = Color(0xFF90A4AE),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = "${state.playerTitle} · ${state.playerName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next Week Button
                ElevatedButton(
                    onClick = onAdvanceWeek,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = CharcoalCard,
                        contentColor = SovereignGold
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        .testTag("next_week_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Next Week",
                        modifier = Modifier.size(14.dp),
                        tint = SovereignGold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Next Week",
                        style = MonospaceStatStyle.copy(fontSize = 12.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Live Badges (Approval, Treasury, Energy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Approval Badge
                StatBadge(
                    label = "APPROVAL",
                    value = "${state.approvalRating.toInt()}%",
                    isPositive = state.approvalRating >= 50.0,
                    modifier = Modifier.weight(1f)
                )

                // Treasury / GDP Badge
                StatBadge(
                    label = "GDP / TREASURY",
                    value = "${if (state.gdpGrowth >= 0) "+" else ""}${state.gdpGrowth}% ($${state.treasuryBillions.toInt()}B)",
                    isPositive = state.gdpGrowth >= 0,
                    modifier = Modifier.weight(1.3f)
                )

                // Energy / Health Badge
                StatBadge(
                    label = "ENERGY",
                    value = "${state.energy.toInt()}%",
                    isPositive = state.energy >= 50,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CharcoalCard)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF78909C),
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    style = MonospaceStatStyle.copy(fontSize = 11.sp),
                    color = if (isPositive) StatPositive else StatNegative,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isPositive) StatPositive else StatNegative,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
