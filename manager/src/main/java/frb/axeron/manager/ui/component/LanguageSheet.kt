package frb.axeron.manager.ui.component

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.screen.findActivity
import frb.axeron.manager.ui.util.LocaleHelper
import java.util.Locale

/**
 * Picks the app language: "System default" plus every language in locales_config. A tap applies
 * it at once; the activity is recreated in the new language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locales = remember { LocaleHelper.getSupportedLocales(context) }
    val current = remember { LocaleHelper.getCurrentAppLocale(context) }
    // Compared as tags: "in" and "id" are the same language, and so are their Locale objects' tags.
    val currentTag = current?.toLanguageTag()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = AxPanelShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    item {
                        LanguageRow(
                            title = stringResource(R.string.system_default),
                            subtitle = null,
                            selected = currentTag == null,
                            onClick = { select(context, null, currentTag, onDismiss) }
                        )
                    }
                    items(locales, key = { it.toLanguageTag() }) { locale ->
                        // The name in its own language, so anyone can find theirs; and in the
                        // current language below it.
                        val native = locale.getDisplayName(locale)
                            .replaceFirstChar { it.titlecase(locale) }
                        val inCurrent = locale.getDisplayName(Locale.getDefault())
                            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                        LanguageRow(
                            title = native,
                            subtitle = inCurrent.takeIf { it != native },
                            selected = locale.toLanguageTag() == currentTag,
                            onClick = { select(context, locale, currentTag, onDismiss) }
                        )
                    }
                }
            }
        }
    }
}

private fun select(
    context: Context,
    locale: Locale?,
    currentTag: String?,
    onDismiss: () -> Unit
) {
    onDismiss()
    if (locale?.toLanguageTag() == currentTag) return
    val activity = context.findActivity() ?: return
    LocaleHelper.setAppLocale(activity, locale)
}

@Composable
private fun LanguageRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else null
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
