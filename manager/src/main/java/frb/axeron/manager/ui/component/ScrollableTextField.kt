package frb.axeron.manager.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/**
 * Multi-line field for long text (setting values, shell snippets). Past [maxLines] it scrolls
 * inside, with a [scrollIndicator] showing where and how much; its scrolling never moves the
 * sheet it sits in ([containScroll]). Pass [scrollState] to read it (e.g. `maxValue > 0`
 * means it scrolls) for a hint outside the field.
 */
@Composable
fun ScrollableTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    minLines: Int = 3,
    maxLines: Int = 8,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    scrollState: ScrollState = rememberScrollState(),
    /** For the field itself rather than its frame, e.g. a focusRequester. */
    fieldModifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        state = state,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, style = textStyle) } },
        enabled = enabled,
        lineLimits = TextFieldLineLimits.MultiLine(minLines, maxLines),
        scrollState = scrollState,
        textStyle = textStyle,
        shape = AxPanelShape,
        trailingIcon = trailingIcon,
        modifier = modifier
            .then(fieldModifier)
            .fillMaxWidth()
            .containScroll()
            .scrollIndicator(scrollState, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    )
}
