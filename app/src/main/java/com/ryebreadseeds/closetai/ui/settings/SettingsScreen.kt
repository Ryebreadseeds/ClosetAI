package com.ryebreadseeds.closetai.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard

@Composable
fun SettingsScreen(vm: ClosetViewModel) {
    val apiKey by vm.apiKeyDraft.collectAsState()
    val hasKey by vm.apiKeyConfigured.collectAsState()
    val city by vm.settingsCity.collectAsState()
    val lat by vm.settingsLat.collectAsState()
    val lon by vm.settingsLon.collectAsState()
    val status by vm.statusMessage.collectAsState()
    var showKey by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SectionTitle("Settings", "Free forever · no subscriptions")

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .closetCard()
                .padding(16.dp)
        ) {
            Text("OpenRouter API key (optional)", style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream)
            Spacer(Modifier.height(6.dp))
            Text(
                "Stored encrypted on this device. Enables smarter photo tagging and LLM outfit tips. " +
                    "Sign up free at openrouter.ai — the app works fully offline without a key.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = vm::setApiKeyDraft,
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showKey = !showKey }) {
                Text(if (showKey) "Hide key" else "Show key", color = ClosetColors.Rose)
            }
            Button(
                onClick = { vm.saveApiKey() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ClosetColors.Rose, contentColor = ClosetColors.Ink)
            ) { Text(if (hasKey) "Update API key" else "Save API key") }
            if (hasKey) {
                TextButton(onClick = { vm.clearApiKey() }) {
                    Text("Clear API key", color = ClosetColors.Danger)
                }
            }
            Text(
                if (hasKey) "Status: key configured" else "Status: offline rules only",
                color = ClosetColors.Mint,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .closetCard()
                .padding(16.dp)
        ) {
            Text("Weather location", style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream)
            Spacer(Modifier.height(6.dp))
            Text(
                "Uses the free Open-Meteo API. Default is Little Falls, NJ. Set a city and/or lat/lon.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = city,
                onValueChange = vm::setCity,
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )
            OutlinedTextField(
                value = lat,
                onValueChange = { vm.setLatLon(it, lon) },
                label = { Text("Latitude") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )
            OutlinedTextField(
                value = lon,
                onValueChange = { vm.setLatLon(lat, it) },
                label = { Text("Longitude") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.saveLocation() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ClosetColors.Plum, contentColor = ClosetColors.Cream)
            ) { Text("Save location & refresh weather") }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .closetCard()
                .padding(16.dp)
        ) {
            Text("Data", style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream)
            Spacer(Modifier.height(6.dp))
            Text("Photos and outfits are stored only on this phone.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { confirmClear = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ClosetColors.Danger, contentColor = ClosetColors.Ink)
            ) { Text("Clear all closet data") }
        }

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = ClosetColors.Mint, modifier = Modifier.padding(horizontal = 20.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "ClosetAI v1.0 · free AI wardrobe assistant",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = ClosetColors.InkMid,
            title = { Text("Clear everything?", color = ClosetColors.Cream) },
            text = {
                Text(
                    "This deletes all items, photos, outfits, and disliked combos on this device. Your API key is kept.",
                    color = ClosetColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllData()
                    confirmClear = false
                }) { Text("Clear", color = ClosetColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", color = ClosetColors.TextSecondary)
                }
            }
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
