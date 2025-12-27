package frb.axeron.manager.ui.screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
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
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.data.SettingsRepository
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.util.ClipboardUtil
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsEditorScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val scope = rememberCoroutineScope()

    /* ================= SOURCE OF TRUTH ================= */

    val settingTypes = remember {
        SettingsRepository.SettingType.entries.toList()
    }

    var selectedType by remember {
        mutableStateOf(SettingsRepository.SettingType.GLOBAL)
    }

    var query by remember { mutableStateOf("") }
    var showFab by remember { mutableStateOf(true) }

    val settingsRepository = remember(contentResolver) {
        SettingsRepository(contentResolver)
    }

    /* ================= PAGER ================= */

    val pagerState = rememberPagerState(
        initialPage = settingTypes.indexOf(selectedType),
        pageCount = { settingTypes.size }
    )

    /* ================= STATE CACHES ================= */

    val dataCache = remember {
        mutableStateMapOf<SettingsRepository.SettingType, Map<String, String?>>()
    }

    val loadingStates = remember {
        mutableStateMapOf<SettingsRepository.SettingType, Boolean>()
    }

    val scrollStates = remember {
        mutableStateMapOf<SettingsRepository.SettingType, LazyListState>()
    }

    /* ================= SYNC PAGER → TAB ================= */

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                selectedType = settingTypes[page]
                showFab = true
            }
    }

    /* ================= REFRESH EVENT ================= */

    fun refresh(type: SettingsRepository.SettingType) {
        scope.launch {
            loadingStates[type] = true
            dataCache[type] = settingsRepository.getAll(type)
            loadingStates[type] = false
        }
    }

    /* ================= UI ================= */

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = "Settings Editor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                searchLabel = "Search Settings",
                searchText = query,
                onSearchTextChange = { query = it },
                onClearClick = { query = "" },
                action = {
                    IconButton(onClick = { refresh(selectedType) }) {
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
                onDismissRequest = { isAdding = false },
                onRefresh = { refresh(selectedType) }
            )

            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                FloatingActionButton(onClick = { isAdding = true }) {
                    Icon(Icons.Filled.Add, null)
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val scope = rememberCoroutineScope()

            RoundedTabRow(
                selectedType = selectedType,
                onSelect = {
                    scope.launch {
                        if (selectedType == it) return@launch
                        val target = settingTypes.indexOf(it)
                        if (pagerState.currentPage != target && target >= 0) {
                            pagerState.animateScrollToPage(target)
                        }
                        showFab = true
                    }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> settingTypes[page].name }
            ) { page ->

                val type = settingTypes[page]

                /* ========== INITIAL LOAD (ONCE PER PAGE) ========== */

                LaunchedEffect(type) {
                    if (!dataCache.containsKey(type)) {
                        loadingStates[type] = true
                        dataCache[type] = settingsRepository.getAll(type)
                        loadingStates[type] = false
                        Log.d("SettingsEditor", "loaded $type")
                    }
                }

                val data = dataCache[type] ?: emptyMap()
                val loading = loadingStates[type] == true

                val filtered by remember(data, query) {
                    derivedStateOf {
                        data.filter {
                            it.key.contains(query, true) ||
                                    it.value?.contains(query, true) == true
                        }
                    }
                }

                val listState = scrollStates.getOrPut(type) {
                    LazyListState()
                }

                /* ========== FAB AUTO HIDE ========== */

                LaunchedEffect(type) {
                    var lastIndex = listState.firstVisibleItemIndex
                    var lastOffset = listState.firstVisibleItemScrollOffset

                    snapshotFlow {
                        listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                    }.collect { (i, o) ->
                        val down = i > lastIndex || (i == lastIndex && o > lastOffset + 4)
                        val up = i < lastIndex || (i == lastIndex && o < lastOffset - 4)

                        when {
                            down && showFab -> showFab = false
                            up && !showFab -> showFab = true
                        }

                        lastIndex = i
                        lastOffset = o
                    }
                }

                /* ========== CONTENT ========== */

                PullToRefreshBox(
                    isRefreshing = loading,
                    onRefresh = { refresh(type) }
                ) {
                    if (loading && data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState
                        ) {
                            items(
                                items = filtered.entries.toList(),
                                key = { it.key }
                            ) { entry ->
                                TableItem(
                                    context = context,
                                    key = entry.key,
                                    value = entry.value ?: "",
                                    selectedType = type,
                                    settingsRepository = settingsRepository
                                ) {
                                    refresh(type)
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

    SecondaryScrollableTabRow(
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
//                            Text(
//                                modifier = Modifier.padding(start = 7.dp),
//                                text = "Remove",
//                                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
//                                fontSize = MaterialTheme.typography.labelMedium.fontSize
//                            )
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
                        VerticalDivider(
                            modifier = Modifier
                                .height(12.dp)
                                .padding(horizontal = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null
                        )
//                        Text(
//                            modifier = Modifier.padding(start = 7.dp),
//                            text = "Key",
//                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
//                            fontSize = MaterialTheme.typography.labelMedium.fontSize
//                        )
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
                        VerticalDivider(
                            modifier = Modifier
                                .height(12.dp)
                                .padding(horizontal = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.AutoMirrored.Outlined.TextSnippet,
                            contentDescription = null
                        )
//                        Text(
//                            modifier = Modifier.padding(start = 7.dp),
//                            text = "Value",
//                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
//                            fontSize = MaterialTheme.typography.labelMedium.fontSize
//                        )
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

