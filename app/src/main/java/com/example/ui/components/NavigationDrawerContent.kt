package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.WorldState
import com.example.engine.llm.EngineStatus
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.MonospaceStatStyle
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.DrawerDestination

@Composable
fun NavigationDrawerContent(
    worldState: WorldState?,
    currentDestination: DrawerDestination,
    engineStatus: EngineStatus,
    onSelectDestination: (DrawerDestination) -> Unit,
    onStartNewCareer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = worldState ?: WorldState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp)
            .background(CharcoalBackground)
            .border(1.dp, CharcoalBorder)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("navigation_drawer_content")
    ) {
        // Dossier Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CharcoalCard)
                    .border(1.5.dp, SovereignGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sovereign_crest),
                    contentDescription = "State Crest",
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "SOVEREIGN",
                    style = MaterialTheme.typography.titleLarge,
                    color = SovereignGold,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.nationName.uppercase(),
                    style = MonospaceStatStyle.copy(fontSize = 11.sp, color = SlateTextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leader Dossier Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CharcoalSurface)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "DOSSIER: " + state.playerName.uppercase(),
                    style = MonospaceStatStyle.copy(fontSize = 12.sp, color = SovereignGold)
                )
                Text(
                    text = "${state.playerTitle} · ${state.lifeStage}",
                    fontSize = 12.sp,
                    color = SlateTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Party: ${state.politicalParty}",
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = CharcoalBorder)
        Spacer(modifier = Modifier.height(12.dp))

        // Categories List
        Text(
            text = "INTELLIGENCE DOMAINS",
            style = MonospaceStatStyle.copy(fontSize = 10.sp, color = Color(0xFF607D8B)),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        DrawerCategoryItem(
            title = "Action Directives",
            subtitle = "Executive orders & prompt input",
            icon = Icons.Default.Bolt,
            isSelected = currentDestination == DrawerDestination.ACTIONS,
            onClick = { onSelectDestination(DrawerDestination.ACTIONS) },
            tag = "drawer_item_actions"
        )

        DrawerCategoryItem(
            title = "Personal",
            subtitle = "Health, wealth, family, scandals",
            icon = Icons.Default.Person,
            isSelected = currentDestination == DrawerDestination.PERSONAL,
            onClick = { onSelectDestination(DrawerDestination.PERSONAL) },
            tag = "drawer_item_personal"
        )

        DrawerCategoryItem(
            title = "Politics",
            subtitle = "Parliament, coalition, factions",
            icon = Icons.Default.AccountBalance,
            isSelected = currentDestination == DrawerDestination.POLITICS,
            onClick = { onSelectDestination(DrawerDestination.POLITICS) },
            tag = "drawer_item_politics"
        )

        DrawerCategoryItem(
            title = "Nation",
            subtitle = "Economy, military, society, media",
            icon = Icons.Default.Public,
            isSelected = currentDestination == DrawerDestination.NATION,
            onClick = { onSelectDestination(DrawerDestination.NATION) },
            tag = "drawer_item_nation"
        )

        DrawerCategoryItem(
            title = "Chronicle",
            subtitle = "Full timeline & tool telemetry",
            icon = Icons.Default.History,
            isSelected = currentDestination == DrawerDestination.CHRONICLE,
            onClick = { onSelectDestination(DrawerDestination.CHRONICLE) },
            tag = "drawer_item_chronicle"
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = CharcoalBorder)
        Spacer(modifier = Modifier.height(12.dp))

        // New Game / Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CharcoalCard)
                .clickable { onStartNewCareer() }
                .padding(10.dp)
                .testTag("drawer_new_game_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = SovereignGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Start New Career",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = SovereignGold,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Begin life simulation afresh",
                    fontSize = 10.sp,
                    color = SlateTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Engine Status Footnote
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF101318))
                .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = SovereignGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = engineStatus.modelName,
                    style = MonospaceStatStyle.copy(fontSize = 10.sp, color = SlateTextPrimary)
                )
                Text(
                    text = "Offline GenAI · XML Tool Calling",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64B5F6)
                )
            }
        }
    }
}

@Composable
private fun DrawerCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val bgColor = if (isSelected) CharcoalSurface else Color.Transparent
    val borderColor = if (isSelected) SovereignGold else Color.Transparent
    val iconColor = if (isSelected) SovereignGold else SlateTextSecondary
    val textColor = if (isSelected) SovereignGold else SlateTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = SlateTextSecondary,
                maxLines = 1
            )
        }
    }
}
