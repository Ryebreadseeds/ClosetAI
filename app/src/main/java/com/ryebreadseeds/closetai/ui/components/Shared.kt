package com.ryebreadseeds.closetai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.theme.closetCard
import java.io.File

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = ClosetColors.Cream)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ItemThumb(
    item: ClosetItemEntity,
    modifier: Modifier = Modifier,
    aspect: Float = 1f
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ClosetColors.Plum)
            .aspectRatio(aspect),
        contentAlignment = Alignment.Center
    ) {
        val file = File(item.photoPath)
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Outlined.Checkroom,
                contentDescription = null,
                tint = ClosetColors.Rose,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun OutfitItemRow(items: List<ClosetItemEntity>) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.take(5).forEach { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                ItemThumb(item, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClosetColors.TextPrimary,
                    maxLines = 1
                )
                Text(item.category, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}

@Composable
fun LoadingCard(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .closetCard()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = ClosetColors.Rose,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
            Text(text, color = ClosetColors.TextPrimary)
        }
    }
}

/** Simple wrap without ExperimentalLayoutApi dependency issues — use AssistChips in a flow-like Column of Rows. */
@Composable
fun FlowRowHack(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val chunked = options.chunked(3)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { opt ->
                    val isSel = opt.equals(selected, true)
                    AssistChip(
                        onClick = { onSelect(opt) },
                        label = { Text(opt) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSel) ClosetColors.Rose.copy(alpha = 0.25f) else ClosetColors.Plum,
                            labelColor = if (isSel) ClosetColors.Cream else ClosetColors.TextSecondary
                        )
                    )
                }
            }
        }
    }
}
