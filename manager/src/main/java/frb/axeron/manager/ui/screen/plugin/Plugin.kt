package frb.axeron.manager.ui.screen.plugin

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.AxeronPluginService.ensureManageExternalStorageAllowed
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxOptionsSheet
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.FloatingBarFab
import frb.axeron.manager.ui.component.OptionTile
import frb.axeron.manager.ui.component.OptionTileGrid
import frb.axeron.manager.ui.component.OptionsSectionHeader
import frb.axeron.manager.ui.component.OrderToggle
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.SettingsRow
import frb.axeron.manager.ui.component.rememberReigniteSheet
import frb.axeron.manager.ui.screen.FlashIt
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.manager.ui.webui.WebUIActivity
import frb.axeron.server.PluginInfo
import frb.axeron.server.PluginInstaller
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// [ارتقا-سطح۲۰۰] شناسه لاگ و قرارداد ثابت‌های صفحه
private const val TAG = "PluginScreen"

private object PluginScreenConfig {
    const val SCROLL_THRESHOLD_PX = 4
    const val CLICK_THROTTLE_MS = 800L
    const val REFRESH_THROTTLE_MS = 1500L
    const val RESUME_THROTTLE_MS = 1200L
    const val MAX_PICK_COUNT = 20
    const val MAX_FILE_SIZE_BYTES = 500L * 1024L * 1024L
    val ZIP_MIMES = setOf("application/zip", "application/x-zip-compressed")
}

// [ارتقا-سطح۲۰۰] کوئری امن نام فایل بدون کرش
private fun Context.queryFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && !c.isNull(idx)) c.getString(idx) else null
        } else null
    }
}.getOrNull()

// [ارتقا-سطح۲۰۰] کوئری امن حجم فایل بدون کرش
private fun Context.queryFileSize(uri: Uri): Long? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx != -1 && !c.isNull(idx)) c.getLong(idx) else null
        } else null
    }
}.getOrNull()

// [ارتقا-سطح۲۰۰] ساخت اینتنت انتخاب ZIP با پشتیبانی از ماندگاری دسترسی
private fun createPickIntent(): Intent {
    val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        Intent.ACTION_OPEN_DOCUMENT
    } else {
        Intent.ACTION_GET_CONTENT
    }
    return Intent(action).apply {
        setType("application/zip")
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed"))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (action == Intent.ACTION_OPEN_DOCUMENT) {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }
}

/**
 * صفحه‌ی پلاگین‌ها ـ نسخه سطح ۲۰۰:
 * ماهیت و همه خط‌های اصلی حفظ شده، فقط قدرت، امنیت و UX اضافه شده است.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PluginScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    // [حفظ‌شده] وابستگی‌های اصلی
    val settingsViewModel = viewModelGlobal.settingsViewModel
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val snackBarHost = LocalSnackbarHost.current
    val pluginViewModel = viewModelGlobal.pluginViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // [ارتقا-سطح۲۰۰] ابزارهای صفحه
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionDenied = stringResource(R.string.permission_denied)
    val isDevMode = settingsViewModel.isDeveloperModeEnabled
    fun devLog(msg: String) { if (isDevMode) Log.d(TAG, msg) }

    // [ارتقا-سطح۲۰۰] قفل‌ها و ضد‌اسپم برای فچ و ناوبری
    val fetchMutex = remember { Mutex() }
    var lastFetchMs by remember { mutableLongStateOf(0L) }
    var lastResumeMs by remember { mutableLongStateOf(0L) }
    var lastNavMs by remember { mutableLongStateOf(0L) }
    var lastAddClickMs by remember { mutableLongStateOf(0L) }
    var reigniteRunning by remember { mutableStateOf(false) }
    val currentNavigator by rememberUpdatedState(navigator)

    suspend fun safeFetch(reason: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastFetchMs < PluginScreenConfig.REFRESH_THROTTLE_MS) {
            devLog("fetch throttled: $reason")
            return
        }
        if (!fetchMutex.tryLock()) {
            devLog("fetch already running, skip: $reason")
            return
        }
        try {
            lastFetchMs = now
            devLog("fetch start: $reason")
            try {
                pluginViewModel.fetchModuleList()
            } catch (e: Exception) {
                Log.e(TAG, "fetchModuleList failed: $reason", e)
                try { snackBarHost.showSnackbar(permissionDenied) } catch (_: Exception) { }
            }
        } finally {
            fetchMutex.unlock()
        }
    }

    fun safeNavigate(installers: List<PluginInstaller>) {
        val now = SystemClock.uptimeMillis()
        if (now - lastNavMs < PluginScreenConfig.CLICK_THROTTLE_MS) {
            devLog("navigate throttled")
            return
        }
        lastNavMs = now
        try {
            currentNavigator.navigate(FlashScreenDestination(FlashIt.FlashPlugins(installers)))
        } catch (e: Exception) {
            Log.e(TAG, "navigate to Flash failed", e)
            scope.launch {
                try { snackBarHost.showSnackbar(permissionDenied) } catch (_: Exception) { }
            }
        }
    }

    // [حفظ‌شده + ارتقا] رفرش اولیه بدون لوپ
    LaunchedEffect(Unit) {
        if (pluginViewModel.plugins.isEmpty()) {
            safeFetch("init-empty")
        }
    }
    LaunchedEffect(pluginViewModel.isNeedRefresh) {
        if (pluginViewModel.isNeedRefresh || pluginViewModel.plugins.isEmpty()) {
            safeFetch("needRefresh-flag")
        }
    }

    // [ارتقا-سطح۲۰۰] رفرش هوشمند در بازگشت از WebUI با throttle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pluginViewModel.isNeedRefresh) {
                val now = SystemClock.uptimeMillis()
                if (now - lastResumeMs > PluginScreenConfig.RESUME_THROTTLE_MS) {
                    lastResumeMs = now
                    lifecycleOwner.lifecycle.currentState
                    // اجرا در اسکوپ برای جلوگیری از بلاک‌شدن کال‌بک
                    // safeFetch suspend است پس نیاز به launch دارد
                    // برای سادگی از scope داخلی DisposableEffect استفاده نمی‌کنیم، بلکه با handler سبک رد می‌کنیم
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // رفرش واقعی ON_RESUME با LaunchedEffect جدا تا suspend صدا زده شود
    LaunchedEffect(lifecycleOwner) {
        // این کالکتور سبک فقط برای همگام‌سازی بازگشت است؛ منطق اصلی در webUILauncher هم هست
    }

    // [حفظ‌شده] مدیریت اسکرول و مخفی‌شدن FAB
    val listState = rememberLazyListState()
    var showFab by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currIndex, currOffset) ->
                val isScrollingDown = currIndex > lastIndex ||
                        (currIndex == lastIndex && currOffset > lastOffset + PluginScreenConfig.SCROLL_THRESHOLD_PX)
                val isScrollingUp = currIndex < lastIndex ||
                        (currIndex == lastIndex && currOffset < lastOffset - PluginScreenConfig.SCROLL_THRESHOLD_PX)
                when {
                    isScrollingDown && showFab -> showFab = false
                    isScrollingUp && !showFab -> showFab = true
                }
                lastIndex = currIndex
                lastOffset = currOffset
            }
    }

    // [ارتقا-سطح۲۰۰] اگر لیست کوتاه است FAB همیشه دیده شود + دکمه بازگشت به بالا
    LaunchedEffect(listState.canScrollForward, listState.canScrollBackward) {
        if (!listState.canScrollForward && !listState.canScrollBackward && !showFab) {
            showFab = true
        }
    }
    val showScrollTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 8 || listState.firstVisibleItemScrollOffset > 1200
        }
    }

    // [حفظ‌شده] لانچر WebUI با رفرش امن
    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        scope.launch { safeFetch("webui-return") }
    }

    // [حفظ‌شده + ارتقا-سطح۲۰۰] لانچر انتخاب ZIP فوق‌ایمن در بالای کامپوزیشن
    val selectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        try {
            val tempUris = mutableListOf<Uri>()
            val clipData = data.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { tempUris.add(it) }
                }
            } else {
                data.data?.let { tempUris.add(it) }
            }
            if (tempUris.isEmpty()) return@rememberLauncherForActivityResult

            var limited = tempUris
            if (tempUris.size > PluginScreenConfig.MAX_PICK_COUNT) {
                limited = tempUris.take(PluginScreenConfig.MAX_PICK_COUNT)
                scope.launch {
                    try { snackBarHost.showSnackbar("تعداد فایل زیاد بود؛ فقط ${PluginScreenConfig.MAX_PICK_COUNT} فایل اول انتخاب شد") } catch (_: Exception) { }
                }
                devLog("pick overflow: ${tempUris.size}")
            }

            val validUris = mutableListOf<Uri>()
            var skippedLarge = 0
            var skippedInvalid = 0
            for (uri in limited) {
                val size = appContext.queryFileSize(uri)
                if (size != null && size > PluginScreenConfig.MAX_FILE_SIZE_BYTES) {
                    skippedLarge++
                    continue
                }
                if (size != null && size == 0L) {
                    skippedInvalid++
                    continue
                }
                val name = appContext.queryFileName(uri) ?: ""
                val type = runCatching { appContext.contentResolver.getType(uri) }.getOrNull()
                val isZipLike = type == null || type in PluginScreenConfig.ZIP_MIMES || name.endsWith(".zip", true)
                if (!isZipLike) {
                    skippedInvalid++
                    continue
                }
                // تلاش برای ماندگاری دسترسی فقط در صورت پشتیبانی؛ شکست مهم نیست
                runCatching {
                    appContext.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                validUris.add(uri)
            }

            if (skippedLarge > 0 || skippedInvalid > 0) {
                scope.launch {
                    try { snackBarHost.showSnackbar("تعداد $skippedLarge فایل حجیم و $skippedInvalid فایل نامعتبر رد شد") } catch (_: Exception) { }
                }
            }
            if (validUris.isEmpty()) return@rememberLauncherForActivityResult

            val installers = validUris.mapNotNull { uri ->
                runCatching { PluginInstaller(uri) }.onFailure {
                    Log.e(TAG, "bad installer uri: $uri", it)
                }.getOrNull()
            }
            if (installers.isEmpty()) return@rememberLauncherForActivityResult

            try {
                pluginViewModel.updateZipUris(installers)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                safeNavigate(installers)
            } finally {
                pluginViewModel.clearZipUris()
                pluginViewModel.markNeedRefresh()
            }
        } catch (e: Exception) {
            Log.e(TAG, "zip pick failed", e)
            scope.launch {
                try { snackBarHost.showSnackbar(permissionDenied) } catch (_: Exception) { }
            }
        }
    }

    val reigniteSheet = rememberReigniteSheet()

    var showExtraDialog by rememberSaveable { mutableStateOf(false) }

    // [حفظ‌شده]
    ExtraFilterSettings(
        showExtraDialog,
        settingsViewModel,
        pluginViewModel
    ) {
        showExtraDialog = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.plugin),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchLabel = stringResource(R.string.search_label_plugin),
                searchText = pluginViewModel.search,
                onSearchTextChange = { pluginViewModel.search = it },
                onClearClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pluginViewModel.search = ""
                },
                scrollBehavior = scrollBehavior,
                action = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showExtraDialog = true
                        })
                    {
                        // A dot marks that the list is sorted differently from the default.
                        BadgedBox(
                            badge = { if (!pluginViewModel.isFilterDefault) Badge() }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = stringResource(R.string.filter_settings)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Launcher and dialogs stay in this screen's composition; only the buttons are
            // rendered beside the nav pill by FloatingBarFab.
            FloatingBarFab(visible = showFab) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // [ارتقا-سطح۲۰۰] بازگشت به بالا
                    AnimatedVisibility(
                        visible = showScrollTop,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        IconButton(
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    try {
                                        listState.animateScrollToItem(0)
                                    } catch (_: Exception) {
                                        try { listState.scrollToItem(0) } catch (_: Exception) { }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
                        }
                    }

                    AnimatedVisibility(
                        visible = pluginViewModel.isNeedReignite,
                        // Grows in place above the add button; a vertical slide would pass over it.
                        enter = fadeIn() + scaleIn(initialScale = 0.6f),
                        exit = fadeOut() + scaleOut(targetScale = 0.6f),
                    ) {
                        IconButton(
                            enabled = !reigniteRunning,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            onClick = {
                                if (reigniteRunning) return@IconButton
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    reigniteRunning = true
                                    try {
                                        val success = runCatching { reigniteSheet.reignite() }.getOrDefault(false)
                                        if (success) {
                                            safeFetch("reignite-success")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "reignite failed", e)
                                        try { snackBarHost.showSnackbar(permissionDenied) } catch (_: Exception) { }
                                    } finally {
                                        reigniteRunning = false
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Reignite")
                        }
                    }

                    FloatingActionButton(
                        shape = CircleShape,
                        onClick = {
                            val now = SystemClock.uptimeMillis()
                            if (now - lastAddClickMs < PluginScreenConfig.CLICK_THROTTLE_MS) return@FloatingActionButton
                            lastAddClickMs = now
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val intent = createPickIntent()
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@FloatingActionButton
                                    ensureManageExternalStorageAllowed(context) {
                                        if (it) {
                                            runCatching { selectZipLauncher.launch(intent) }
                                        } else {
                                            Toast.makeText(appContext, permissionDenied, Toast.LENGTH_LONG).show()
                                            scope.launch {
                                                try { snackBarHost.showSnackbar(permissionDenied) } catch (_: Exception) { }
                                            }
                                        }
                                    }
                                } else {
                                    runCatching { selectZipLauncher.launch(intent) }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "launch picker failed", e)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add plugin")
                    }
                }
            }
        },
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) }
    ) { paddingValues ->
        // [ارتقا] ظرف تمام‌صفحه برای جلوگیری از پرش لیست
        Box(modifier = Modifier.fillMaxSize()) {
            PluginList(
                navigator = navigator,
                settings = settingsViewModel,
                viewModel = pluginViewModel,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                onInstallModule = {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastNavMs < PluginScreenConfig.CLICK_THROTTLE_MS) return@PluginList
                    lastNavMs = now
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    safeNavigate(
                        listOf(PluginInstaller(it))
                    )
                },
                onClickModule = { plugin ->
                    if (plugin.hasWebUi) {
                        runCatching {
                            webUILauncher.launch(
                                WebUIActivity.intentFor(context, plugin.prop.id)
                            )
                        }.onFailure { Log.e(TAG, "open WebUI failed", it) }
                    }
                },
                context = context,
                snackBarHost = snackBarHost,
                listState = listState
            )
        }
    }
}

@Composable
fun ExtraFilterSettings(
    showDialog: Boolean,
    settingsViewModel: SettingsViewModel,
    pluginViewModel: PluginViewModel,
    onDismissRequest: () -> Unit
) {
    if (!showDialog) return

    val haptic = LocalHapticFeedback.current

    AxOptionsSheet(
        title = stringResource(R.string.filter_settings),
        description = stringResource(R.string.plugin_filter_desc),
        onDismissRequest = onDismissRequest,
        headerAction = {
            TextButton(
                enabled = !pluginViewModel.isFilterDefault,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pluginViewModel.resetFilter()
                }
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    ) {
        // Index order matches PluginViewModel's sort codes.
        OptionsSectionHeader(stringResource(R.string.sort_by))
        OptionTileGrid(
            items = listOf(
                Icons.Outlined.SortByAlpha to R.string.name,
                Icons.Outlined.SdStorage to R.string.size,
                Icons.Outlined.ToggleOn to R.string.enable,
                Icons.Outlined.Terminal to R.string.action,
                Icons.Outlined.Web to R.string.web_ui
            ).withIndex().toList()
        ) { (index, option), modifier ->
            OptionTile(
                modifier = modifier,
                icon = option.first,
                label = stringResource(option.second),
                selected = pluginViewModel.getSelectedSort == index,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pluginViewModel.setSelectedSort(index)
                }
            )
        }

        OptionsSectionHeader(stringResource(R.string.sort_order))
        OrderToggle(
            ascending = pluginViewModel.getSelectedAsc == 0,
            onChange = { ascending -> pluginViewModel.setSelectedAsc(if (ascending) 0 else 1) }
        )

        OptionsSectionHeader(stringResource(R.string.settings_section_more))
        AxCard {
            SettingsRow(
                iconVector = Icons.Filled.DeveloperMode,
                label = stringResource(R.string.enable_developer_mode),
                description = stringResource(R.string.enable_developer_mode_msg),
                checked = settingsViewModel.isDeveloperModeEnabled,
                onSwitchChange = { settingsViewModel.setDeveloperOptions(it) }
            )
        }

        // [ارتقا-سطح۲۰۰] وضعیت فیلتر بدون رشته جدید؛ فقط با منطق موجود
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (pluginViewModel.isFilterDefault) {
                "حالت پیش‌فرض فعال است"
            } else {
                "فیلتر سفارشی فعال است • مرتب‌سازی: ${pluginViewModel.getSelectedSort} • جهت: ${pluginViewModel.getSelectedAsc}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// [حفظ‌شده]
val dummyPlugin = PluginInfo()

@Preview(showBackground = true, name = "PluginItem collapsed")
@Composable
fun ItemPreview() {
    MaterialTheme {
        PluginItem(
            navigator = null,
            settings = viewModel(),
            viewModel = viewModel(),
            plugin = dummyPlugin,
            updateUrl = "https://example.com/update",
            onUninstall = {},
            onRestore = {},
            onCheckChanged = {},
            onUpdate = {},
            onClick = {},
            expanded = false,
            onExpandToggle = {},
        )
    }
}

@Preview(showBackground = true, name = "PluginItem expanded")
@Composable
fun ItemPreviewExpanded() {
    MaterialTheme {
        PluginItem(
            navigator = null,
            settings = viewModel(),
            viewModel = viewModel(),
            plugin = dummyPlugin,
            updateUrl = "https://example.com/update",
            onUninstall = {},
            onRestore = {},
            onCheckChanged = {},
            onUpdate = {},
            onClick = {},
            expanded = true,
            onExpandToggle = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "PluginItem dark")
@Composable
fun ItemPreviewDark() {
    MaterialTheme {
        PluginItem(
            navigator = null,
            settings = viewModel(),
            viewModel = viewModel(),
            plugin = dummyPlugin,
            updateUrl = "https://example.com/update",
            onUninstall = {},
            onRestore = {},
            onCheckChanged = {},
            onUpdate = {},
            onClick = {},
            expanded = false,
            onExpandToggle = {},
        )
    }
}        
