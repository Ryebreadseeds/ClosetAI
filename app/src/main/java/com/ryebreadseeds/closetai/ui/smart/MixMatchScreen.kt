package com.ryebreadseeds.closetai.ui.smart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.domain.ClothingCategory
import com.ryebreadseeds.closetai.domain.MixSlot
import com.ryebreadseeds.closetai.domain.Occasion
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.components.FlowRowHack
import com.ryebreadseeds.closetai.ui.components.ItemThumb
import com.ryebreadseeds.closetai.ui.components.LoadingCard
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard

@Composable
fun MixMatchScreen(vm: ClosetViewModel, onBack: () -> Unit) {
    val mix by vm.mix.collectAsState()
    val items by vm.items.collectAsState()
    val byId = remember(items) { items.associateBy { it.id } }
    var pickingSlot by remember { mutableStateOf<MixSlot?>(null) }

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
            SectionTitle("Mix & Match", "Never pants + dress/romper")
        }

        Text("Occasion", color = ClosetColors.Cream, modifier = Modifier.padding(horizontal = 20.dp))
        FlowRowHack(
            options = Occasion.entries.map { it.label },
            selected = mix.occasion.label,
            onSelect = { vm.setMixOccasion(Occasion.fromLabel(it)) }
        )

        Spacer(Modifier.height(8.dp))
        MixSlot.entries.forEach { slot ->
            val selected = mix.slots[slot]?.let { byId[it] }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .closetCard(16)
                    .clickable { pickingSlot = slot }
                    .padding(12.dp)
            ) {
                Text(slot.label, style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream)
                if (selected != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ItemThumb(selected, Modifier.size(56.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(selected.name, color = ClosetColors.Cream)
                            Text("${selected.category} · ${selected.color}")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { vm.setMixSlot(slot, null) }) {
                            Text("Clear", color = ClosetColors.Danger)
                        }
                    }
                } else {
                    Text("Tap to pick", color = ClosetColors.TextSecondary)
                }
            }
        }

        if (mix.working) LoadingCard("Completing your mix…")
        mix.message?.let {
            Text(it, color = ClosetColors.Mint, modifier = Modifier.padding(20.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.saveMixOutfit() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ClosetColors.Rose, contentColor = ClosetColors.Ink)
            ) { Text("Save outfit") }
            FilledTonalButton(
                onClick = { vm.askAiCompleteMix() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ClosetColors.Plum,
                    contentColor = ClosetColors.Cream
                )
            ) { Text("Ask AI") }
        }
        TextButton(onClick = { vm.clearMix() }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Clear all", color = ClosetColors.TextSecondary)
        }
    }

    pickingSlot?.let { slot ->
        val pool = items.filter { itemEligibleForSlot(it, slot) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingSlot = null },
            containerColor = ClosetColors.InkMid,
            title = { Text("Pick ${slot.label}", color = ClosetColors.Cream) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (pool.isEmpty()) {
                        Text("No matching items in closet.", color = ClosetColors.TextSecondary)
                    } else {
                        pool.forEach { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.setMixSlot(slot, item.id)
                                        pickingSlot = null
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ItemThumb(item, Modifier.size(48.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("${item.name} · ${item.color}", color = ClosetColors.Cream)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingSlot = null }) {
                    Text("Close", color = ClosetColors.Rose)
                }
            }
        )
    }
}

private fun itemEligibleForSlot(item: ClosetItemEntity, slot: MixSlot): Boolean {
    val cat = ClothingCategory.fromLabel(item.category)
    return when (slot) {
        MixSlot.BASE_TOP, MixSlot.LAYER_TOP -> cat == ClothingCategory.TOP
        MixSlot.BOTTOM -> cat == ClothingCategory.BOTTOM
        MixSlot.ONE_PIECE -> cat == ClothingCategory.DRESS || cat == ClothingCategory.ROMPER
        MixSlot.OUTERWEAR -> cat == ClothingCategory.OUTERWEAR
        MixSlot.SHOES -> cat == ClothingCategory.SHOES
        MixSlot.ACCESSORY -> cat == ClothingCategory.ACCESSORY || cat == ClothingCategory.OTHER
    }
}
