package frb.axeron.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(
    title: @Composable () -> Unit,
    searchLabel: String = stringResource(R.string.search_label),
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onConfirm: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }

    // The search field is always shown under the title, so the list never hides it.
    val background = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier.background(
            Brush.verticalGradient(
                0f to background,
                0.85f to background,
                1f to background.copy(alpha = 0f)
            )
        )
    ) {
        AxTopAppBar(
            title = title,
            navigationIcon = onBackClick?.let { onBack ->
                {
                    IconButton(
                        onClick = onBack,
                        content = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                    )
                }
            },
            actions = { action?.invoke() },
            scrollBehavior = scrollBehavior
        )
        SearchPill(
            text = searchText,
            onTextChange = onSearchTextChange,
            placeholder = searchLabel,
            onClear = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onClearClick()
            },
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onConfirm?.invoke()
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
    }
}

/**
 * Compact search field. Built on BasicTextField: a Material TextField can't go below 56dp,
 * which looks oversized under a title bar.
 */
@Composable
private fun SearchPill(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 4.dp)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        inner()
                    }
                }
            )
            if (text.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SearchAppBarPreview() {
    var searchText by remember { mutableStateOf("") }
    SearchAppBar(
        title = { Text("Search text") },
        searchText = searchText,
        onSearchTextChange = { searchText = it },
        onClearClick = { searchText = "" }
    )
}
