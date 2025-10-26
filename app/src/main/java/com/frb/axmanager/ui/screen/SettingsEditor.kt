package com.frb.axmanager.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frb.axmanager.data.SettingsRepository
import com.frb.axmanager.ui.component.ConfirmResult
import com.frb.axmanager.ui.component.SearchAppBar
import com.frb.axmanager.ui.component.rememberConfirmDialog
import com.frb.axmanager.ui.util.ClipboardUtil
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsEditorScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val contentResolver = LocalContext.current.contentResolver
    var selectedType by remember { mutableStateOf(SettingsRepository.SettingType.GLOBAL) }
    var query by remember { mutableStateOf("") }
    var settingsMap by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settingsRepository = SettingsRepository(contentResolver)
    val context = LocalContext.current

    val scrollStates =
        remember { mutableStateMapOf<SettingsRepository.SettingType, LazyListState>() }

    var showFab by remember { mutableStateOf(true) }

    fun loadData() {
        scope.launch {
            isRefreshing = true
            settingsMap = settingsRepository.getAll(selectedType)
            isRefreshing = false
        }
    }

    LaunchedEffect(selectedType) {
        showFab = true
        loadData()
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = "Settings Editor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchLabel = "Search Settings",
                searchText = query,
                onSearchTextChange = { query = it },
                onClearClick = { query = "" },
                windowInsets = WindowInsets(top = 0),
                action = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                settingsMap = settingsRepository.getAll(selectedType)
                                delay(100)
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Refresh, null)
                    }
                }
            )
        },
        floatingActionButton = {
            var isAdding by remember { mutableStateOf(false) }

            TableEditor(
                addTable = true,
                context = context,
                showDialog = isAdding,
                selectedType = selectedType,
                settingsRepository = settingsRepository,
                onDismissRequest = {
                    isAdding = false
                },
                onRefresh = {
                    loadData()
                }
            )

            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                FloatingActionButton(
                    onClick = {
                        isAdding = true
                    }
                ) {
                    Icon(Icons.Filled.Add, null)
                }
            }
        },
        contentWindowInsets = WindowInsets(top = 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            RoundedTabRow(
                selectedType = selectedType,
                onSelect = {
                    selectedType = it
                }
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        settingsMap = settingsRepository.getAll(selectedType)
                        delay(100)
                        isRefreshing = false
                    }
                }
            ) {

                Column {
                    if (isRefreshing) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val filtered = settingsMap.filter {
                            it.key.contains(query, ignoreCase = true) || it.value?.contains(query, ignoreCase = true) == true
                        }

                        AnimatedContent(
                            targetState = selectedType,
                            transitionSpec = {
                                // Dari kiri ke kanan saat maju, kanan ke kiri saat mundur
                                if (targetState.ordinal > initialState.ordinal) {
                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(250)
                                    ) + fadeIn() togetherWith
                                            slideOutHorizontally(
                                                targetOffsetX = { -it / 2 },
                                                animationSpec = tween(250)
                                            ) + fadeOut()
                                } else {
                                    slideInHorizontally(
                                        initialOffsetX = { -it },
                                        animationSpec = tween(250)
                                    ) + fadeIn() togetherWith
                                            slideOutHorizontally(
                                                targetOffsetX = { it / 2 },
                                                animationSpec = tween(250)
                                            ) + fadeOut()
                                }
                            },
                            label = "SettingsSlideAnim"
                        ) { type ->

                            val listState = scrollStates.getOrPut(type) { LazyListState() }

                            LaunchedEffect(listState) {
                                var lastIndex = listState.firstVisibleItemIndex
                                var lastOffset = listState.firstVisibleItemScrollOffset

                                snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                                    .collect { (currIndex, currOffset) ->
                                        val isScrollingDown = currIndex > lastIndex ||
                                                (currIndex == lastIndex && currOffset > lastOffset + 4)
                                        val isScrollingUp = currIndex < lastIndex ||
                                                (currIndex == lastIndex && currOffset < lastOffset - 4)

                                        when {
                                            isScrollingDown && showFab -> showFab = false
                                            isScrollingUp && !showFab -> showFab = true
                                        }

                                        lastIndex = currIndex
                                        lastOffset = currOffset
                                    }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState
                            ) {
                                items(filtered.entries.toList(), key = { it.key }) { entry ->
                                    TableItem(
                                        context = context,
                                        key = entry.key,
                                        value = entry.value ?: "",
                                        selectedType = selectedType,
                                        settingsRepository = settingsRepository
                                    ) {
                                        loadData()
                                    }
                                }
                            }
                        }
                    }
                }

            }

        }
    }
}

@Composable
fun RoundedTabRow(
    selectedType: SettingsRepository.SettingType,
    onSelect: (SettingsRepository.SettingType) -> Unit
) {
    val selectedIndex = SettingsRepository.SettingType.entries.indexOf(selectedType)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(50)), // biar seluruh tabrow bulat
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        indicator = { null },
        edgePadding = 0.dp,
        divider = {}
    ) {
        SettingsRepository.SettingType.entries.forEachIndexed { index, type ->
            val isSelected = index == selectedIndex
            val textColor by animateColorAsState(
                if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Tab(
                selected = isSelected,
                onClick = { onSelect(type) },
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 8.dp),
                text = {
                    Text(
                        text = type.toString(),
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}


@Composable
fun TableItem(
    context: Context,
    key: String,
    value: String,
    selectedType: SettingsRepository.SettingType,
    settingsRepository: SettingsRepository,
    onRefresh: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    TableEditor(
        context = context,
        showDialog = isEditing,
        key = key,
        value = value,
        selectedType = selectedType,
        settingsRepository = settingsRepository,
        onDismissRequest = {
            isEditing = false
        },
        onRefresh = onRefresh
    )

    ElevatedCard(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        onClick = {
            isEditing = true
        },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SmartWrappedText(
                text = key,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
            )
            Spacer(Modifier.height(4.dp))
            SmartWrappedText(
                text = value.ifBlank { "(null)" },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditor(
    addTable: Boolean = false,
    context: Context,
    showDialog: Boolean,
    key: String = "",
    value: String = "",
    selectedType: SettingsRepository.SettingType,
    settingsRepository: SettingsRepository,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    var newKey by remember { mutableStateOf(key) }
    var newValue by remember { mutableStateOf(value) }

    LaunchedEffect(key, value, showDialog) {
        newKey = key
        newValue = value
    }

    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = {
                onDismissRequest()
            },
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (!addTable) {
                        val confirmDialog = rememberConfirmDialog()
                        val scope = rememberCoroutineScope()

                        FilledTonalButton(
                            modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                            onClick = {
                                scope.launch {
                                    val confirmResult = confirmDialog.awaitConfirm(
                                        "Remove Now?",
                                        content = "This action will remove this setting permanently",
                                        confirm = "Remove",
                                        dismiss = "Cancel"
                                    )
                                    if (confirmResult == ConfirmResult.Confirmed) {
                                        if (settingsRepository.deleteValue(selectedType, newKey)) {
                                            onRefresh()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed To Remove",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                        onDismissRequest()
                                    }
                                }

                            },
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null
                            )
                            Text(
                                modifier = Modifier.padding(start = 7.dp),
                                text = "Remove",
                                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                fontSize = MaterialTheme.typography.labelMedium.fontSize
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    FilledTonalButton(
                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                        enabled = newKey.isNotEmpty(),
                        onClick = {
                            if (ClipboardUtil.put(context, newKey)) {
                                Toast.makeText(context, "Key Copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 7.dp),
                            text = "Key",
                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    FilledTonalButton(
                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                        enabled = newValue.isNotEmpty(),
                        onClick = {
                            if (ClipboardUtil.put(context, newValue)) {
                                Toast.makeText(context, "Value Copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 7.dp),
                            text = "Value",
                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    FilledTonalButton(
                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                        enabled = newValue != value && newKey.isNotEmpty(),
                        onClick = {
                            if (settingsRepository.putValue(selectedType, newKey, newValue)) {
                                onRefresh()
                            } else {
                                Toast.makeText(context, "Failed to Save", Toast.LENGTH_SHORT).show()
                            }
                            onDismissRequest()
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 7.dp),
                            text = "Save",
                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        )
                    }
                }

                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    DividerDefaults.Thickness,
                    MaterialTheme.colorScheme.surfaceContainerHighest
                )

                if (addTable) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newKey.ifBlank { "" },
                        onValueChange = {
                            newKey = it
                        },
                        label = {
                            Text("Add Key")
                        },
                        textStyle = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,   // garis saat fokus
                            unfocusedIndicatorColor = Color.Transparent, // garis saat tidak fokus
                            disabledIndicatorColor = Color.Transparent,   // garis saat disabled
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    )
                } else {
                    SmartWrappedText(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = newKey,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newValue.ifBlank { "" },
                    onValueChange = {
                        newValue = it
                    },
                    label = {
                        Text("Edit Value")
                    },
                    textStyle = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    maxLines = 20,
                    singleLine = false,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,   // garis saat fokus
                        unfocusedIndicatorColor = Color.Transparent, // garis saat tidak fokus
                        disabledIndicatorColor = Color.Transparent,   // garis saat disabled
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SmartWrappedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    wrapSymbols: String = ".,=_()[]{}<>:;+-*/|\\"
) {
    // Buat regex untuk simbol wrap
    val regex = remember(wrapSymbols) {
        Regex("(?<=[${Regex.escape(wrapSymbols)}])")
    }

    // Sisipkan zero-width space setelah simbol
    val formatted = remember(text) {
        text.replace(regex, "\u200B")
    }

    Text(
        text = formatted,
        color = color,
        style = style.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier,
        softWrap = true,
        overflow = TextOverflow.Clip
    )
}

