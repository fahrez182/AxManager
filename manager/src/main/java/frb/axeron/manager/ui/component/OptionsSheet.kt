package frb.axeron.manager.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R

/**
 * Bottom sheet frame shared by the filter and shortcut sheets: title, optional description,
 * an optional header action (e.g. Reset), then [content] on a 16dp gutter. [overline] is a
 * small line above the title, e.g. a technical name the title explains.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxOptionsSheet(
    title: String,
    onDismissRequest: () -> Unit,
    description: String? = null,
    headerAction: (@Composable () -> Unit)? = null,
    overline: (@Composable () -> Unit)? = null,
    /** False for sheets whose content is its own lazy list (it can't sit in a scrolling parent). */
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onDismissRequest = onDismissRequest
    ) {
        // Scrolls when the content is taller than the screen, instead of cutting the bottom off.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    overline?.invoke()
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                headerAction?.invoke()
            }

            content()
        }
    }
}

/** Section title inside an [AxOptionsSheet]. */
@Composable
fun OptionsSectionHeader(text: String) {
    SectionHeader(text = text, modifier = Modifier.padding(top = 4.dp))
}

/** Selectable card: icon badge, label and an optional subtitle (e.g. a count). */
@Composable
fun OptionTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "optionTileContainer"
    )

    Surface(
        modifier = modifier.alpha(if (enabled) 1f else 0.45f),
        onClick = onClick,
        enabled = enabled,
        shape = AxCardShape,
        color = containerColor,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(imageVector = icon, size = 32.dp)
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Lays [items] out as [OptionTile]s, [columns] per row; a short last row keeps tile widths.
 * Tiles in a row share one height even when only some of them show a subtitle.
 */
@Composable
fun <T> OptionTileGrid(
    items: List<T>,
    columns: Int = 3,
    tile: @Composable (item: T, modifier: Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item -> tile(item, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Two-way pill toggle for ascending / descending. */
@Composable
fun OrderToggle(
    ascending: Boolean,
    onChange: (Boolean) -> Unit
) {
    ChoicePill(
        options = listOf(
            Triple(true, Icons.Outlined.ArrowUpward, R.string.ascending),
            Triple(false, Icons.Outlined.ArrowDownward, R.string.descending)
        ),
        selected = ascending,
        onSelect = onChange
    )
}

/**
 * One pill, one segment per option (value, icon, label); the selected one is filled with
 * [selectedColor] of it, primary unless given.
 */
@Composable
fun <T> ChoicePill(
    options: List<Triple<T, ImageVector, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
    selectedColor: @Composable (T) -> Color = { MaterialTheme.colorScheme.primary },
    selectedContentColor: @Composable (T) -> Color = { MaterialTheme.colorScheme.onPrimary },
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEach { (value, icon, labelId) ->
                val isSelected = value == selected
                val background by animateColorAsState(
                    targetValue = if (isSelected) selectedColor(value) else Color.Transparent,
                    label = "choiceOption"
                )
                val content = if (isSelected) selectedContentColor(value)
                else MaterialTheme.colorScheme.onSurfaceVariant
                val onClick = { if (!isSelected) onSelect(value) }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(background)
                        .clickable(enabled = enabled, onClick = onClick)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = icon,
                        contentDescription = null,
                        tint = content
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(labelId),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = content
                    )
                }
            }
        }
    }
}
