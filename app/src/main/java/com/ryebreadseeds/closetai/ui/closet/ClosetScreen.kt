package com.ryebreadseeds.closetai.ui.closet

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.domain.ClothingCategory
import com.ryebreadseeds.closetai.domain.Season
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.components.ItemThumb
import com.ryebreadseeds.closetai.ui.components.LoadingCard
import com.ryebreadseeds.closetai.ui.components.SectionTitle
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(vm: ClosetViewModel) {
    val items by vm.items.collectAsState()
    val draft by vm.draft.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSourceSheet by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var itemPendingDelete by remember { mutableStateOf<ClosetItemEntity?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) vm.startAddFromUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists()) {
            vm.startAddFromPath(file.absolutePath)
        }
        pendingCameraFile = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val file = vm.createCameraFile()
                pendingCameraFile = file
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                cameraLauncher.launch(uri)
            }
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            scope.launch {
                val file = vm.createCameraFile()
                pendingCameraFile = file
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                cameraLauncher.launch(uri)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SectionTitle("Closet", "${items.size} items · tap to edit")
            if (items.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .closetCard()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tap + to add from camera or gallery.\nPhotos stay on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClosetColors.TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        Column(
                            Modifier
                                .closetCard(16)
                                .clickable { vm.startEdit(item) }
                                .padding(10.dp)
                        ) {
                            ItemThumb(item, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text(item.name, style = MaterialTheme.typography.titleMedium, color = ClosetColors.Cream, maxLines = 1)
                            Text("${item.category} · ${item.color}", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { itemPendingDelete = item }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ClosetColors.Danger)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showSourceSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = ClosetColors.Rose,
            contentColor = ClosetColors.Ink
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add item")
        }
    }

    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            containerColor = ClosetColors.InkMid,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add clothing item", style = MaterialTheme.typography.titleLarge, color = ClosetColors.Cream)
                Button(
                    onClick = {
                        showSourceSheet = false
                        launchCamera()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ClosetColors.Rose, contentColor = ClosetColors.Ink)
                ) {
                    Icon(Icons.Outlined.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Take photo")
                }
                Button(
                    onClick = {
                        showSourceSheet = false
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ClosetColors.Plum, contentColor = ClosetColors.Cream)
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose from gallery")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    draft?.let { d ->
        AlertDialog(
            onDismissRequest = { if (!d.analyzing) vm.dismissDraft() },
            containerColor = ClosetColors.InkMid,
            title = {
                Text(if (d.id == null) "New item" else "Edit item", color = ClosetColors.Cream)
            },
            text = {
                if (d.analyzing) {
                    LoadingCard("Analyzing photo…")
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        if (d.photoPath.isNotBlank()) {
                            ItemThumb(
                                ClosetItemEntity(
                                    name = d.name, category = d.category, color = d.color,
                                    season = d.season, photoPath = d.photoPath
                                ),
                                Modifier.fillMaxWidth(),
                                aspect = 4f / 3f
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        d.error?.let {
                            Text(it, color = ClosetColors.Danger)
                            Spacer(Modifier.height(8.dp))
                        }
                        Field("Name", d.name) { vm.updateDraft { cur -> cur.copy(name = it) } }
                        DropdownField(
                            "Category",
                            d.category,
                            ClothingCategory.entries.map { it.label }
                        ) { vm.updateDraft { cur -> cur.copy(category = it) } }
                        Field("Color", d.color) { vm.updateDraft { cur -> cur.copy(color = it) } }
                        DropdownField(
                            "Season",
                            d.season,
                            Season.entries.map { it.label }
                        ) { vm.updateDraft { cur -> cur.copy(season = it) } }
                        Field("Notes", d.notes) { vm.updateDraft { cur -> cur.copy(notes = it) } }
                    }
                }
            },
            confirmButton = {
                if (!d.analyzing) {
                    TextButton(onClick = { vm.saveDraft() }) {
                        Text("Save", color = ClosetColors.Rose)
                    }
                }
            },
            dismissButton = {
                IconButton(onClick = { vm.dismissDraft() }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = ClosetColors.TextSecondary)
                }
            }
        )
    }

    itemPendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            containerColor = ClosetColors.InkMid,
            title = { Text("Delete item?", color = ClosetColors.Cream) },
            text = { Text("Remove \"${item.name}\" and its photo from this device.", color = ClosetColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteItem(item)
                    itemPendingDelete = null
                }) { Text("Delete", color = ClosetColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) {
                    Text("Cancel", color = ClosetColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = label != "Notes",
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClosetColors.Rose,
            unfocusedBorderColor = ClosetColors.CardStroke,
            focusedLabelColor = ClosetColors.Rose,
            cursorColor = ClosetColors.Rose,
            focusedTextColor = ClosetColors.TextPrimary,
            unfocusedTextColor = ClosetColors.TextPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ClosetColors.Rose,
                unfocusedBorderColor = ClosetColors.CardStroke,
                focusedLabelColor = ClosetColors.Rose,
                focusedTextColor = ClosetColors.TextPrimary,
                unfocusedTextColor = ClosetColors.TextPrimary
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onChange(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
