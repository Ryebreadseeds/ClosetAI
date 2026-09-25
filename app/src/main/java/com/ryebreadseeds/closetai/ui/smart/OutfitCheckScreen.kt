package com.ryebreadseeds.closetai.ui.smart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ryebreadseeds.closetai.domain.Occasion
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.components.FlowRowHack
import com.ryebreadseeds.closetai.ui.components.ItemThumb
import com.ryebreadseeds.closetai.ui.components.LoadingCard
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard

@Composable
fun OutfitCheckScreen(vm: ClosetViewModel, onBack: () -> Unit) {
    val check by vm.check.collectAsState()
    val items by vm.items.collectAsState()
    val outfits by vm.outfits.collectAsState()
    val byId = remember(items) { items.associateBy { it.id } }

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
            SectionTitle("Outfit Check", "Color · coherence · occasion (1–10)")
        }

        Text("Occasion", color = ClosetColors.Cream, modifier = Modifier.padding(horizontal = 20.dp))
        FlowRowHack(
            options = Occasion.entries.map { it.label },
            selected = check.occasion.label,
            onSelect = { vm.setCheckOccasion(Occasion.fromLabel(it)) }
        )

        Text(
            "Load a saved outfit",
            color = ClosetColors.Cream,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyRow(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(outfits.take(12), key = { it.id }) { outfit ->
                FilterChip(
                    selected = check.fromOutfitId == outfit.id,
                    onClick = { vm.loadOutfitIntoCheck(outfit) },
                    label = { Text(outfit.title, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClosetColors.Rose.copy(alpha = 0.25f),
                        selectedLabelColor = ClosetColors.Cream,
                        containerColor = ClosetColors.Plum,
                        labelColor = ClosetColors.TextSecondary
                    )
                )
            }
        }

        Text(
            "Or tap items",
            color = ClosetColors.Cream,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        val selected = item.id in check.selectedIds
                        Row(
                            Modifier
                                .weight(1f)
                                .closetCard()
                                .clickable { vm.toggleCheckItem(item.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ItemThumb(item, Modifier.size(44.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, color = ClosetColors.Cream, maxLines = 1)
                                Text(if (selected) "Selected" else item.category, maxLines = 1)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.runOutfitCheck() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClosetColors.Rose,
                    contentColor = ClosetColors.Ink
                )
            ) { Text("Run check") }
            TextButton(onClick = { vm.clearCheck() }) {
                Text("Clear", color = ClosetColors.TextSecondary)
            }
        }

        if (check.loading) LoadingCard("Scoring your look…")
        check.message?.let {
            Text(it, color = ClosetColors.Mint, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }
        check.result?.let { result ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .closetCard()
                    .padding(16.dp)
            ) {
                Text("Overall ${result.overall}/10", style = MaterialTheme.typography.titleLarge, color = ClosetColors.Cream)
                Spacer(Modifier.height(8.dp))
                ScoreLine("Color", result.colorScore)
                ScoreLine("Coherence", result.coherenceScore)
                ScoreLine("Occasion", result.occasionScore)
                Spacer(Modifier.height(10.dp))
                Text("Tips", color = ClosetColors.Cream, style = MaterialTheme.typography.titleMedium)
                result.tips.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Selected: " + check.selectedIds.mapNotNull { byId[it]?.name }.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreLine(label: String, score: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text("$score / 10", color = ClosetColors.Cream)
    }
}
