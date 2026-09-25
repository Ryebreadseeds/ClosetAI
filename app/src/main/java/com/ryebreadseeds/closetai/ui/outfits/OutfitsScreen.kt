package com.ryebreadseeds.closetai.ui.outfits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.components.OutfitItemRow
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OutfitsScreen(vm: ClosetViewModel) {
    val outfits by vm.outfits.collectAsState()
    val allItems by vm.items.collectAsState()
    val byId = remember(allItems) { allItems.associateBy { it.id } }
    val fmt = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        SectionTitle("Outfits", "Saved & history — delete any suggestion")
        if (outfits.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .closetCard()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Generated outfits will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClosetColors.TextSecondary
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(outfits, key = { it.id }) { outfit ->
                    val pieces = outfit.itemIds().mapNotNull { byId[it] }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .closetCard()
                            .padding(14.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(outfit.title, style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream)
                                Text(
                                    "${outfit.occasion} · ${outfit.source} · ${fmt.format(Date(outfit.createdAt))}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            when (outfit.liked) {
                                true -> Icon(Icons.Outlined.ThumbUp, null, tint = ClosetColors.Mint)
                                false -> Icon(Icons.Outlined.ThumbDown, null, tint = ClosetColors.Danger)
                                null -> {}
                            }
                            IconButton(onClick = { vm.deleteOutfit(outfit.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ClosetColors.Danger)
                            }
                        }
                        if (outfit.rationale.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(outfit.rationale, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (pieces.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            OutfitItemRow(pieces)
                        } else {
                            Spacer(Modifier.height(6.dp))
                            Text("Items no longer in closet", color = ClosetColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
