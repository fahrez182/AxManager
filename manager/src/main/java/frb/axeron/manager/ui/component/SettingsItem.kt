package frb.axeron.manager.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Rows inside a settings card: 16dp gutter + 36dp badge + 12dp gap.
private val RowTextInset = 64.dp

/**
 * One card that groups several [SettingsRow]s, separated by inset dividers.
 * Like [SettingsItem], it carries its own 16dp horizontal gutter.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AxCard(
        modifier = modifier.padding(horizontal = 16.dp),
        content = content
    )
}

@Composable
fun SettingsRow(
    label: String,
    description: String? = null,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    checked: Boolean = false,
    showDivider: Boolean = false,
    onSwitchChange: ((checked: Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    /** Extra actions (e.g. an icon button) placed before the switch or chevron. */
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable (enabled: Boolean) -> Unit)? = null
) {
    // A switch row toggles from anywhere on the row, not only from the switch itself.
    val rowClick = onClick ?: onSwitchChange?.let { { it(!checked) } }

    Column {
        if (showDivider) {
            SettingsDivider()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (rowClick != null) Modifier.clickable(enabled = enabled) { rowClick() }
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRowBody(
                label = label,
                description = description,
                icon = iconPainter ?: iconVector?.let { rememberVectorPainter(it) },
                enabled = enabled
            )

            trailing?.invoke(this)

            when {
                onSwitchChange != null -> {
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        enabled = enabled,
                        checked = checked,
                        onCheckedChange = onSwitchChange
                    )
                }

                onClick != null -> RowChevron()
            }
        }

        if (content != null) {
            content(enabled)
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = RowTextInset, end = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

@Composable
private fun RowScope.SettingsRowBody(
    label: String?,
    description: String?,
    icon: Painter?,
    enabled: Boolean
) {
    if (icon != null) {
        IconBadge(
            painter = icon,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(12.dp))
    }

    Column(modifier = Modifier.weight(1f)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun RowChevron() {
    Spacer(Modifier.width(8.dp))
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

enum class SettingsItemType {
    PARENT,
    CHILD
}

/** Card frame shared by the standalone settings items: an [AxCard] for PARENT, flat for CHILD. */
@Composable
private fun SettingsItemFrame(
    type: SettingsItemType,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = when (type) {
            SettingsItemType.PARENT -> Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
            SettingsItemType.CHILD -> Modifier
        },
        onClick = onClick,
        enabled = enabled,
        shape = when (type) {
            SettingsItemType.PARENT -> AxCardShape
            SettingsItemType.CHILD -> RectangleShape
        },
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = when (type) {
            SettingsItemType.PARENT -> 2.dp
            SettingsItemType.CHILD -> 0.dp
        }
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    type: SettingsItemType = SettingsItemType.PARENT,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
    SettingsItemFrame(
        type = type,
        containerColor = containerColor,
        contentColor = contentColor,
        enabled = enabled,
        onClick = {
            when {
                onClick != null || onCheckedClick != null -> {
                    onClick?.invoke()
                    onCheckedClick?.invoke(checked)
                }
                // A plain switch item toggles from anywhere on the card.
                onSwitchChange != null -> onSwitchChange(!checked)
            }
        }
    ) {
        Row(
            modifier = when {
                label == null && description == null -> Modifier.height(IntrinsicSize.Min)
                else -> Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .height(IntrinsicSize.Min)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRowBody(
                label = label,
                description = description,
                icon = iconPainter ?: iconVector?.let { rememberVectorPainter(it) },
                enabled = enabled
            )

            when {
                onSwitchChange != null -> {
                    if (onCheckedClick != null) {
                        Spacer(Modifier.width(12.dp))
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(DividerDefaults.Thickness)
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        enabled = enabled,
                        checked = checked,
                        onCheckedChange = onSwitchChange
                    )
                }

                onClick != null && content == null -> RowChevron()
            }
        }

        if (content != null) {
            content(enabled, checked)
        }
    }
}

@Composable
fun SettingsItemExpanded(
    type: SettingsItemType = SettingsItemType.PARENT,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
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

    SettingsItemFrame(
        type = type,
        containerColor = containerColor,
        contentColor = contentColor,
        enabled = enabled,
        onClick = { expand = !expand }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRowBody(
                label = label,
                description = description,
                icon = iconPainter ?: iconVector?.let { rememberVectorPainter(it) },
                enabled = enabled
            )

            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation)
            )
        }

        content(enabled, expand)
    }
}

@Composable
fun CheckBoxText(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(AxPanelShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable { onCheckedChange(!checked) }
            .height(40.dp)
            .padding(horizontal = 4.dp),
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
