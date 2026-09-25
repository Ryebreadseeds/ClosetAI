package com.ryebreadseeds.closetai.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ryebreadseeds.closetai.domain.Occasion
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.components.FlowRowHack
import com.ryebreadseeds.closetai.ui.components.LoadingCard
import com.ryebreadseeds.closetai.ui.components.OutfitItemRow
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard

@Composable
fun TodayScreen(vm: ClosetViewModel) {
    val today by vm.today.collectAsState()
    val items by vm.items.collectAsState()

    LaunchedEffect(items.size) {
        if (today.current == null && items.isNotEmpty() && !today.generating) {
            vm.generateOutfit()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SectionTitle(
            "Today",
            subtitle = when {
                today.weatherLoading -> "Fetching weather…"
                today.weather != null -> {
                    val w = today.weather!!
                    "${w.cityLabel} · ${w.temperatureF.toInt()}°F · ${w.description}"
                }
                else -> "Weather unavailable — still works offline"
            }
        )

        Text(
            "Occasion",
            style = MaterialTheme.typography.titleMedium,
            color = ClosetColors.Cream,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        FlowRowHack(
            options = Occasion.entries.map { it.label },
            selected = today.occasion.label,
            onSelect = { vm.setOccasion(Occasion.fromLabel(it)) }
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = today.mood,
            onValueChange = vm::setMood,
            label = { Text("Mood (optional)") },
            placeholder = { Text("cozy, bold, minimal…") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            colors = fieldColors()
        )
        // Persist mood when leaving field — also on regenerate
        LaunchedEffect(today.mood) { /* draft only; persisted on generate */ }

        Spacer(Modifier.height(12.dp))

        when {
            today.generating -> LoadingCard("Styling your outfit…")
            today.current != null -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .closetCard()
                        .padding(16.dp)
                ) {
                    Text(today.current!!.title, style = MaterialTheme.typography.titleLarge, color = ClosetColors.Cream)
                    Spacer(Modifier.height(4.dp))
                    Text(today.current!!.rationale, style = MaterialTheme.typography.bodyMedium)
                    if (today.current!!.layered) {
                        Spacer(Modifier.height(4.dp))
                        Text("Layered look", color = ClosetColors.Mint, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(14.dp))
                    OutfitItemRow(today.currentItems)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.dislikeCurrent() }) {
                            Icon(
                                Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (today.liked == false) ClosetColors.Danger else ClosetColors.TextSecondary
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                vm.persistMood()
                                vm.generateOutfit()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ClosetColors.Plum,
                                contentColor = ClosetColors.Cream
                            )
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Regenerate")
                        }
                        IconButton(onClick = { vm.likeCurrent() }) {
                            Icon(
                                Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (today.liked == true) ClosetColors.Mint else ClosetColors.TextSecondary
                            )
                        }
                    }
                }
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .closetCard()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (items.isEmpty()) "Your closet is empty"
                        else "No outfit yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = ClosetColors.Cream
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (items.isEmpty()) "Add pieces from the Closet tab, then come back."
                        else "Tap generate for a suggestion.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    if (items.isNotEmpty()) {
                        Button(
                            onClick = {
                                vm.persistMood()
                                vm.generateOutfit()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClosetColors.Rose,
                                contentColor = ClosetColors.Ink
                            )
                        ) { Text("Generate outfit") }
                    }
                }
            }
        }

        today.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = ClosetColors.Mint,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (today.hasApiKey) "AI assist on (OpenRouter)" else "Offline rules engine · add free API key in Settings for smarter picks",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ClosetColors.Rose,
    unfocusedBorderColor = ClosetColors.CardStroke,
    focusedLabelColor = ClosetColors.Rose,
    cursorColor = ClosetColors.Rose,
    focusedTextColor = ClosetColors.TextPrimary,
    unfocusedTextColor = ClosetColors.TextPrimary
)

