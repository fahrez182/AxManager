package frb.axeron.manager.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import kotlin.math.ceil

/**
 * Confirmation for an action that is hard to undo.
 *
 * Top: a tinted panel with the action's icon inside a ring that runs down for [seconds], the
 * title and the [subject]. Then the [message] as a note (a warning when [destructive]), any
 * [extra] options, the countdown and the buttons. With no answer in time the sheet cancels
 * itself, so one left open (or opened by mistake) never confirms anything.
 *
 * @param subject what the action targets (e.g. the app's icon and name).
 * @param extra options for the action, e.g. a [ConfirmOption] to keep data on uninstall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    destructive: Boolean = true,
    seconds: Int = 10,
    subject: (@Composable () -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    val left = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        left.animateTo(0f, tween(seconds * 1000, easing = LinearEasing))
        dismiss()
    }
    val remaining = ceil(left.value * seconds).toInt().coerceAtLeast(0)
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero: what is about to happen, to what, and how long is left to decide.
            Surface(
                shape = AxCardShape,
                color = accent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                        CircularProgressIndicator(
                            progress = { left.value },
                            modifier = Modifier.size(80.dp),
                            color = accent,
                            trackColor = accent.copy(alpha = 0.15f),
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Surface(shape = CircleShape, color = accent, modifier = Modifier.size(58.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (destructive) MaterialTheme.colorScheme.onError
                                    else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    subject?.invoke()
                }
            }

            // The consequence, as a note: a warning for destructive actions.
            Surface(
                shape = AxPanelShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        if (destructive) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (extra != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    content = extra
                )
            }

            // Countdown pill: says plainly that doing nothing cancels.
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.countdown_cancels_in, remaining),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Default,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    onClick = onConfirm,
                    colors = if (destructive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else ButtonDefaults.buttonColors()
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(confirmLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * A checkable option inside a confirmation: icon, label, a line on what it does, and a
 * checkbox. The whole card toggles, and it lights up when checked.
 */
@Composable
fun ConfirmOption(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val border by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "confirmOptionBorder"
    )
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = AxPanelShape,
        color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
        ) {
            IconBadge(imageVector = icon, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
