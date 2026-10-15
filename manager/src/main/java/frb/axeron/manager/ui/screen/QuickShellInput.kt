package frb.axeron.manager.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.data.CommandBookmarks
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.AxDropdownDivider
import frb.axeron.manager.ui.component.AxDropdownHeader
import frb.axeron.manager.ui.component.AxDropdownItem
import frb.axeron.manager.ui.component.AxDropdownMenu
import androidx.compose.material.icons.outlined.MoreVert
import android.widget.Toast
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.input.TextFieldState
import frb.axeron.manager.ui.component.ScrollableTextField
import frb.axeron.manager.ui.component.scrollIndicator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.EmptyState

/** Keys of the items QuickShell appends to the command field's text menu. */
private object BookmarkMenuKey
private object BookmarksMenuKey

/**
 * QuickShell's command bar: a `$` / `#` prompt, the input and a run button.
 *
 * The input shows where it is scrolled to (bar on the right): collapsed it is one line high,
 * expanded it grows to [EXPANDED_LINES] lines; past that it scrolls. Newlines are always kept.
 *
 * Snippets live in the text menus (next to Copy / Paste), not in extra buttons: with text
 * selected, "Save snippet" saves the selection; with only a cursor, "Save snippet" saves the
 * whole command and "Snippets" opens the saved list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandInput(
    state: TextFieldState,
    /** `#` for a root shell, `$` otherwise, like a terminal prompt. */
    prompt: String,
    expanded: Boolean,
    onRun: () -> Unit,
    onSaveBookmark: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarkLabel = stringResource(R.string.quickshell_bookmark)
    val bookmarksLabel = stringResource(R.string.quickshell_bookmarks)
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface
    )
    val scroll = rememberScrollState()
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

    Surface(
        shape = AxCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        // One line: input centred on the exec button. Expanded (keyboard up, several lines):
        // the button stays at the bottom, next to the last line, as before.
        Row(
            verticalAlignment = if (expanded) Alignment.Bottom else Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            BasicTextField(
                state = state,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                // Never SingleLine: that one strips newlines from typed and pasted text, which
                // flattened multi-line snippets. One line high is MultiLine(1, 1) instead.
                lineLimits = TextFieldLineLimits.MultiLine(1, if (expanded) EXPANDED_LINES else 1),
                scrollState = scroll,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = if (expanded) 12.dp else 8.dp)
                    .animateContentSize(tween(250, easing = LinearOutSlowInEasing))
                    .scrollIndicator(scroll, barColor, inset = 0.dp, edge = 0.dp)
                    // With a selection, "Save snippet" comes from SaveSnippetActivity (the
                    // system's process-text entries), here as in the output. These cover the
                    // cursor-only menu, which process-text entries don't appear in.
                    .appendTextContextMenuComponents {
                        if (!state.selection.collapsed) return@appendTextContextMenuComponents
                        val text = state.text.toString()
                        separator()
                        if (text.isNotBlank()) {
                            item(BookmarkMenuKey, bookmarkLabel) {
                                close()
                                onSaveBookmark(text)
                            }
                        }
                        item(BookmarksMenuKey, bookmarksLabel) {
                            close()
                            onOpenBookmarks()
                        }
                    },
                decorator = { inner ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "$prompt ",
                            style = textStyle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(Modifier.weight(1f)) {
                            if (state.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.quickshell_input_hint),
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        }
                    }
                }
            )
            // The original exec button: the plain ic_exec glyph in primary.
            IconButton(onClick = onRun, modifier = Modifier.padding(end = 6.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_exec),
                    contentDescription = stringResource(R.string.exec),
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Lines the expanded command input shows before it scrolls. */
private const val EXPANDED_LINES = 6

/**
 * One saved command, cut at 2 lines. A short one inserts on tap; a longer one shows how long
 * it is and opens [onView] instead, so a command is never inserted or run unread. ⋮ has
 * View / edit, Insert, Run and Delete in the app's dropdown (the one Quick Settings uses).
 */
@Composable
private fun SnippetRow(
    command: String,
    onView: () -> Unit,
    onInsert: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    // Cut by the 2-line limit (newlines, or one long line wrapping), seen at layout.
    var clipped by remember(command) { mutableStateOf(false) }
    Surface(
        shape = AxPanelShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = if (clipped) onView else onInsert
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (it.hasVisualOverflow) clipped = true }
                )
                if (clipped) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.UnfoldMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.quickshell_snippet_long_hint,
                                command.lineCount(),
                                command.length
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                AxDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    AxDropdownHeader(command.lineSequence().first(), monospace = true)
                    AxDropdownItem(
                        text = stringResource(R.string.quickshell_view_edit),
                        icon = Icons.Outlined.Edit,
                        onClick = {
                            menu = false
                            onView()
                        }
                    )
                    AxDropdownItem(
                        text = stringResource(R.string.quickshell_insert),
                        icon = Icons.AutoMirrored.Outlined.Input,
                        onClick = {
                            menu = false
                            onInsert()
                        }
                    )
                    AxDropdownItem(
                        text = stringResource(R.string.quickshell_run),
                        icon = Icons.Outlined.PlayArrow,
                        onClick = {
                            menu = false
                            onRun()
                        }
                    )
                    AxDropdownDivider()
                    AxDropdownItem(
                        text = stringResource(R.string.remove),
                        icon = Icons.Outlined.Delete,
                        destructive = true,
                        onClick = {
                            menu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

private fun String.lineCount(): Int = count { it == '\n' } + 1

/**
 * The whole snippet, editable. Insert and Run use what is in the field, so it can be tweaked for
 * one run without saving; "Save changes" (shown once it differs) writes it back to the list.
 */
@Composable
private fun SnippetSheet(
    command: String,
    onInsert: (String) -> Unit,
    onRun: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val updated = stringResource(R.string.quickshell_snippet_updated)
    val state = remember(command) { TextFieldState(command) }
    val text = state.text.toString()
    val modified = text != command

    AxOptionsSheet(
        title = stringResource(R.string.quickshell_snippet),
        description = stringResource(R.string.quickshell_snippet_stats, text.lineCount(), text.length),
        onDismissRequest = onDismissRequest,
        headerAction = {
            IconButton(onClick = {
                CommandBookmarks.remove(command)
                onDismissRequest()
            }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.remove),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        ScrollableTextField(
            state = state,
            minLines = 3,
            maxLines = 8,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth()
        )
        if (modified) {
            FilledTonalButton(
                onClick = {
                    CommandBookmarks.update(command, text)
                    Toast.makeText(context, updated, Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quickshell_save_changes))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onInsert(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Input, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quickshell_insert))
            }
            Button(
                onClick = { onRun(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quickshell_run))
            }
        }
    }
}

/** Types a new snippet, long ones included (the field scrolls with an indicator). */
@Composable
fun AddSnippetSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val saved = stringResource(R.string.quickshell_bookmark_saved)
    val state = remember { TextFieldState() }
    val text = state.text.toString()

    AxOptionsSheet(
        title = stringResource(R.string.quickshell_add_snippet),
        description = if (text.isEmpty()) stringResource(R.string.quickshell_add_snippet_desc)
        else stringResource(R.string.quickshell_snippet_stats, text.lineCount(), text.length),
        onDismissRequest = onDismissRequest
    ) {
        ScrollableTextField(
            state = state,
            placeholder = stringResource(R.string.quickshell_input_hint),
            minLines = 4,
            maxLines = 8,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismissRequest, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    CommandBookmarks.add(text)
                    Toast.makeText(context, saved, Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save))
            }
        }
    }
}

/** Saved commands: view or edit one, insert it at the cursor, run it, or delete it. */
@Composable
fun BookmarksSheet(
    onInsert: (String) -> Unit,
    onRun: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val bookmarks = CommandBookmarks.items
    var viewing by remember { mutableStateOf<String?>(null) }
    viewing?.let { command ->
        SnippetSheet(
            command = command,
            onInsert = onInsert,
            onRun = onRun,
            onDismissRequest = { viewing = null }
        )
    }

    AxOptionsSheet(
        title = stringResource(R.string.quickshell_bookmarks),
        description = stringResource(R.string.quickshell_bookmarks_desc),
        onDismissRequest = onDismissRequest
    ) {
        if (bookmarks.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.BookmarkBorder,
                title = stringResource(R.string.quickshell_bookmarks_empty)
            )
            return@AxOptionsSheet
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(bookmarks, key = { it }) { command ->
                SnippetRow(
                    command = command,
                    onView = { viewing = command },
                    onInsert = { onInsert(command) },
                    onRun = { onRun(command) },
                    onDelete = { CommandBookmarks.remove(command) }
                )
            }
        }
    }
}
