package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.data.WidgetSpec
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

@Composable
fun WidgetStep(
    selected: List<String>,
    density: GridDensity,
    columns: Int,
    rows: Int,
    iconShape: String,
    onToggle: (String) -> Unit,
    onDensity: (GridDensity) -> Unit,
    onIconShape: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val palette = LocalPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(46.dp))
        Text(
            text = "Build your first page",
            color = palette.onGlass,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Choose what sits on your home screen. You can move or remove anything later.",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("Widgets")
            Widgets.all.forEach { spec ->
                WidgetCard(
                    spec = spec,
                    icon = iconFor(spec.type),
                    selected = spec.type in selected,
                    onClick = { onToggle(spec.type) },
                )
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("Grid")
            Segmented(
                options = GridDensity.entries.map { it.id to it.label },
                selected = density.id,
                onSelect = { onDensity(GridDensity.byId(it)) },
            )
            Text(
                text = "$columns columns · $rows rows per page",
                color = palette.onGlassMuted,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(4.dp))
            SectionLabel("Icon shape")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconShapes.ids.forEach { id ->
                    ShapeChip(id = id, selected = id == iconShape, onClick = { onIconShape(id) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(text = "Back", onClick = onBack)
            GlassButton(
                text = "Set up my home screen",
                onClick = onContinue,
                filled = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WidgetCard(spec: WidgetSpec, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val border by animateColorAsState(
        if (selected) palette.accent.copy(alpha = 0.85f) else Color.Transparent,
        label = "widgetBorder",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .glass(ZenithShapes.card, strength = 0.85f)
            .border(1.5.dp, border, ZenithShapes.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(ZenithShapes.tile)
                .background(
                    if (selected) palette.accent.copy(alpha = 0.22f) else palette.onGlass.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) palette.accent else palette.onGlassMuted, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(spec.title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.onGlass.copy(alpha = 0.10f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("${spec.spanX}x${spec.spanY}", color = palette.onGlassMuted, fontSize = 10.sp)
                }
            }
            Text(spec.description, color = palette.onGlassMuted, fontSize = 12.5.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.size(10.dp))
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) palette.accent else palette.onGlass.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF0A0B12), modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun ShapeChip(id: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(52.dp)
                .clip(ZenithShapes.forIconShape(id))
                .background(
                    if (selected) palette.accent.copy(alpha = 0.85f) else palette.onGlass.copy(alpha = 0.14f)
                )
                .clickable(onClick = onClick)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            IconShapes.label(id),
            color = if (selected) palette.onGlass else palette.onGlassMuted,
            fontSize = 11.sp,
        )
    }
}

private fun iconFor(type: String): ImageVector = when (type) {
    "clock" -> Icons.Rounded.Schedule
    "battery" -> Icons.Rounded.BatteryChargingFull
    "calendar" -> Icons.Rounded.CalendarMonth
    "notes" -> Icons.AutoMirrored.Rounded.StickyNote2
    "search" -> Icons.Rounded.Search
    else -> Icons.Rounded.Tune
}
