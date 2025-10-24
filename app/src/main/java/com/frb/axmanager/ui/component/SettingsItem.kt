package com.frb.axmanager.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class SettingsItemType {
    PARENT,
    CHILD
}


@Composable
fun SettingsItem(
    type: SettingsItemType = SettingsItemType.PARENT,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    label: String? = null,
    description: String? = null,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    onClick: (() -> Unit)? = null,
    onCheckedClick: ((checked: Boolean) -> Unit)? = null,
    checked: Boolean = false,
    onSwitchChange: ((checked: Boolean) -> Unit)? = null,
    content: (@Composable (enabled: Boolean, checked: Boolean) -> Unit)? = null
) {
    ElevatedCard(
        modifier = when (type) {
            SettingsItemType.PARENT -> Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
            SettingsItemType.CHILD -> Modifier.padding(0.dp)
        },
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = when (type) {
            SettingsItemType.PARENT -> CardDefaults.elevatedShape
            SettingsItemType.CHILD -> RoundedCornerShape(0.dp)
        },
        elevation = when (type) {
            SettingsItemType.PARENT -> CardDefaults.elevatedCardElevation()
            SettingsItemType.CHILD -> CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        },
        enabled = enabled,
        onClick = {
            if (onClick != null) {
                onClick()
            }
            if (onCheckedClick != null) {
                onCheckedClick(checked)
            }
        }
    ) {
        Column {
            Row(
                modifier = when {
                    label == null && description == null -> Modifier.height(IntrinsicSize.Min)
                    else -> Modifier
                        .padding(all = 16.dp)
                        .height(IntrinsicSize.Min)
                },
                verticalAlignment = Alignment.CenterVertically
            )
            {
                when {
                    iconVector != null -> {
                        Icon(
                            modifier = Modifier.size(24.scaleDp),
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(16.dp))
                    }

                    iconPainter != null -> {
                        Icon(
                            modifier = Modifier.size(24.scaleDp),
                            painter = iconPainter,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(16.dp))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (label != null) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (onSwitchChange != null) {
                    if (onCheckedClick != null) {
                        Spacer(Modifier.width(12.dp))
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(DividerDefaults.Thickness)
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        enabled = enabled,
                        checked = checked,
                        onCheckedChange = onSwitchChange
                    )
                }

            }

            if (content != null) {
                content(enabled, checked)
            }
        }
    }
}

@Composable
fun SettingsItemExpanded(
    type: SettingsItemType = SettingsItemType.PARENT,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    label: String,
    description: String? = null,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    expanded: Boolean = false,
    content: @Composable (enabled: Boolean, expanded: Boolean) -> Unit = { enabled, expanded -> }
) {
    var expand by remember { mutableStateOf(expanded) }

    val rotation by animateFloatAsState(
        targetValue = if (expand) -180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "rotate"
    )

    ElevatedCard(
        modifier = when (type) {
            SettingsItemType.PARENT -> Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
            SettingsItemType.CHILD -> Modifier.padding(0.dp)
        },
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = when (type) {
            SettingsItemType.PARENT -> CardDefaults.elevatedShape
            SettingsItemType.CHILD -> RoundedCornerShape(0.dp)
        },
        elevation = when (type) {
            SettingsItemType.PARENT -> CardDefaults.elevatedCardElevation()
            SettingsItemType.CHILD -> CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        },
        enabled = enabled,
        onClick = {
            expand = !expand
        }
    ) {
        Column {
            Row(
                modifier = when (type) {
                    SettingsItemType.PARENT -> Modifier.padding(all = 16.dp)
                    SettingsItemType.CHILD -> Modifier.padding(horizontal = 16.dp)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    iconVector != null -> {
                        Icon(
                            modifier = Modifier.size(24.scaleDp),
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(16.dp))
                    }

                    iconPainter != null -> {
                        Icon(
                            modifier = Modifier.size(24.scaleDp),
                            painter = iconPainter,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(16.dp))
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            content(enabled, expand)
        }
    }
}

@Composable
fun CheckBoxText(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (checked) {
                true -> MaterialTheme.colorScheme.surfaceContainerHighest
                false -> MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(5.dp),
        onClick = {
            onCheckedChange(!checked)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                modifier = Modifier.size(32.dp),
                checked = checked,
                onCheckedChange = { newValue ->
                    onCheckedChange(newValue)
                }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.fontSize
            )
        }
    }
}
