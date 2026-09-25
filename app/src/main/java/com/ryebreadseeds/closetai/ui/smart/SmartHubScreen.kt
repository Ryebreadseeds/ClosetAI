package com.ryebreadseeds.closetai.ui.smart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.JoinInner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard

@Composable
fun SmartHubScreen(
    onMix: () -> Unit,
    onCheck: () -> Unit,
    onShop: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SectionTitle("Smart", "Your free stylist toolkit — no paywall")
        Spacer(Modifier.height(8.dp))
        SmartCard(
            icon = Icons.Outlined.JoinInner,
            title = "Mix & Match",
            subtitle = "Build looks slot by slot. Ask AI to fill the gaps.",
            onClick = onMix
        )
        SmartCard(
            icon = Icons.Outlined.Checklist,
            title = "Outfit Check",
            subtitle = "Score color, coherence, and occasion fit — offline or AI.",
            onClick = onCheck
        )
        SmartCard(
            icon = Icons.Outlined.ShoppingBag,
            title = "Shopping Buddy",
            subtitle = "Gap analysis with concrete piece ideas. No checkout, no affiliates.",
            onClick = onShop
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .closetCard()
                .padding(16.dp)
        ) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = ClosetColors.Rose)
            Spacer(Modifier.height(8.dp))
            Text("Tip", style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream)
            Text(
                "Add an OpenRouter key in Settings for Magic Upload, richer AI completes, and sharper tips. Everything still works offline with local rules.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SmartCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .closetCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ClosetColors.Rose)
        Text(title, style = MaterialTheme.typography.titleLarge, color = ClosetColors.Cream)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}
