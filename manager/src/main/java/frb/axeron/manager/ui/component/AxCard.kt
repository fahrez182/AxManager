package frb.axeron.manager.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// One card language for the whole app: a flat tonal surface, tinted panels inside instead of nested cards.
val AxCardShape = RoundedCornerShape(16.dp)
val AxPanelShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AxCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** A long press; the card then handles both itself (Surface's onClick has no long press). */
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = AxCardShape,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    if (onLongClick != null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick ?: {},
                    onLongClick = onLongClick
                ),
            shape = shape,
            color = color,
            tonalElevation = 2.dp
        ) {
            Column(content = content)
        }
    } else if (onClick != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = color,
            tonalElevation = 2.dp
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            tonalElevation = 2.dp
        ) {
            Column(content = content)
        }
    }
}

/** Tinted panel used inside an [AxCard] to group a row of values. */
fun Modifier.axPanel(color: Color): Modifier = this
    .clip(AxPanelShape)
    .background(color.copy(alpha = 0.6f))

@Composable
fun IconBadge(
    painter: Painter,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(size * 0.55f),
            painter = painter,
            contentDescription = null,
            tint = tint
        )
    }
}

@Composable
fun IconBadge(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 36.dp
) = IconBadge(rememberVectorPainter(imageVector), modifier, tint, size)

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InlineDivider(height: Dp) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(height)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 4.dp),
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}
