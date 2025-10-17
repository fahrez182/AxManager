package com.frb.axmanager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun LabelPreview() {
    ExtraLabel()
}

@Composable
fun ExtraLabel() {
    Card(
        shape = RoundedCornerShape(3.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(10.dp),
                imageVector = Icons.Outlined.Tune,
                contentDescription = null
            )

            Spacer(Modifier.size(4.dp))

            Text(
                fontWeight = FontWeight.Medium,
                lineHeight = 8.sp,
                fontSize = 8.sp,
                text = "TEST")
        }
    }
}

@Immutable
class ExtraLabelStyle(
    val containerColor: Color,
    val contentColor: Color,
    val shape: Shape,
    val textStyle: TextStyle,
    val allCaps: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ExtraLabelStyle) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (shape != other.shape) return false
        if (textStyle != other.textStyle) return false

        return true
    }

    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        shape: Shape = this.shape,
        allCaps: Boolean = this.allCaps,
        textStyle: TextStyle = this.textStyle
    ): ExtraLabelStyle = ExtraLabelStyle(
        containerColor,
        contentColor,
        shape,
        textStyle,
        allCaps
    )

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + textStyle.hashCode()
        return result
    }
}

object ExtraLabelDefaults {
    val style
        @Composable get() = ExtraLabelStyle(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(3.dp),
            textStyle = TextStyle(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 8.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            allCaps = true
        )
}

@Composable
fun ExtraLabel(
    text: String,
    style: ExtraLabelStyle = ExtraLabelDefaults.style,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = RoundedCornerShape(3.dp),
        colors = CardDefaults.cardColors().copy(
            containerColor = style.containerColor,
            contentColor = style.contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                iconVector != null -> {
                    Icon(
                        modifier = Modifier.size(10.scaleDp),
                        imageVector = iconVector,
                        contentDescription = null
                    )
                    Spacer(Modifier.size(4.dp))
                }
                iconPainter != null -> {
                    Icon(
                        modifier = Modifier.size(10.scaleDp),
                        painter = iconPainter,
                        contentDescription = null
                    )
                    Spacer(Modifier.size(4.dp))
                }
            }

            Text(
                text = if (style.allCaps) text.uppercase() else text,
                style = style.textStyle.copy(
                    color = style.contentColor
                )
            )
        }
    }
}

