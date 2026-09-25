package com.ryebreadseeds.closetai.ui.smart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.components.LoadingCard
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard

@Composable
fun ShoppingBuddyScreen(vm: ClosetViewModel, onBack: () -> Unit) {
    val shop by vm.shop.collectAsState()

    LaunchedEffect(Unit) {
        if (shop.suggestions.isEmpty() && !shop.loading) vm.runShoppingBuddy()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = ClosetColors.Cream)
            }
            SectionTitle("Shopping Buddy", "Gap ideas only — no purchases in-app")
        }

        Button(
            onClick = { vm.runShoppingBuddy() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ClosetColors.Rose,
                contentColor = ClosetColors.Ink
            )
        ) { Text("Analyze closet gaps") }

        if (shop.loading) LoadingCard("Finding wardrobe gaps…")
        shop.message?.let {
            Text(it, color = ClosetColors.Mint, modifier = Modifier.padding(20.dp))
        }

        shop.suggestions.forEach { tip ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .closetCard()
                    .padding(14.dp)
            ) {
                Text(tip.name, style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream)
                Spacer(Modifier.height(2.dp))
                Text("${tip.category} · ${tip.occasionHint}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(tip.why, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (!shop.loading && shop.suggestions.isEmpty()) {
            Text(
                "Add a few closet items first, then run analysis.",
                color = ClosetColors.TextSecondary,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}
