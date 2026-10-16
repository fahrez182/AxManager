package frb.axeron.manager.ui.screen

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import frb.axeron.manager.ui.component.AxTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.rememberLifecycleOwner
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppearanceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeveloperScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.AxCardShape
import frb.axeron.manager.ui.component.AxPanelShape
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.SectionHeader
import frb.axeron.manager.ui.component.SettingsDivider
import frb.axeron.manager.ui.component.SettingsGroup
import frb.axeron.manager.ui.component.SettingsRow
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import frb.axeron.server.ServerConstants

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val activateViewModel = viewModelGlobal.activateViewModel
    val settings = viewModelGlobal.settingsViewModel
//    val privilegeViewModel = viewModelGlobal.privilegeViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showDevDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    if (showQuickSettings) {
        QuickSettingsSheet(onDismissRequest = { showQuickSettings = false })
    }

    DeveloperInfo(
        showDevDialog
    ) {
        showDevDialog = false
    }

    if (showResetDialog) {
        ResetBottomSheet(
            onDismissRequest = { showResetDialog = false },
            isRoot = activateViewModel.axeronInfo.isRoot(),
            onReset = { rootMode, both ->
                showResetDialog = false
                navigator.navigate(FlashScreenDestination(FlashIt.FlashUninstall(rootMode, both)))
            }
        )
    }

    Scaffold(
        topBar = {
            AxTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 5.dp),
                        onClick = {
                            showDevDialog = true
                        })
                    {
                        Icon(Icons.Outlined.Info, null)
                    }
                }
            )
        }
    ) { paddingValues ->

        val axeronRunning = activateViewModel.axeronInfo.isRunning()

        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                // Padding inside the scroll lets content pass under the fading top bar.
                .padding(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            SettingsSectionHeader(stringResource(R.string.settings_section_environment))
            SettingsGroup {
                if (axeronRunning) {
                    val context = LocalContext.current
                    // Older Shizuku clients (e.g. old ZArchiver) refuse to connect unless the real
                    // Shizuku app is installed. Re-checked on resume: the user may install it meanwhile.
                    var shizukuAppInstalled by remember { mutableStateOf(isShizukuAppInstalled(context)) }
                    val lifecycleOwner = rememberLifecycleOwner()
                    DisposableEffect(Unit) {
                        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
                            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                                activateViewModel.checkShizukuIntercept()
                                activateViewModel.checkStopPluginsWithServer()
                                shizukuAppInstalled = isShizukuAppInstalled(context)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                    SettingsRow(
                        iconPainter = painterResource(R.drawable.ic_axeron),
                        label = stringResource(R.string.axeron_permission),
                        description = if (activateViewModel.isShizukuActive && !shizukuAppInstalled) {
                            stringResource(R.string.axeron_permission_desc) + "\n\n" +
                                    stringResource(R.string.axeron_permission_shizuku_missing)
                        } else {
                            stringResource(R.string.axeron_permission_desc)
                        },
                        checked = activateViewModel.isShizukuActive,
                        onSwitchChange = { activateViewModel.setShizukuIntercept(it) }
                    )
                    SettingsRow(
                        iconVector = Icons.Outlined.PowerSettingsNew,
                        label = stringResource(R.string.stop_plugins_with_server),
                        description = stringResource(R.string.stop_plugins_with_server_desc),
                        checked = activateViewModel.isStopPluginsWithServer,
                        onSwitchChange = { activateViewModel.updateStopPluginsWithServer(it) },
                        showDivider = true
                    )
                }

                SettingsRow(
                    iconVector = Icons.Filled.Terminal,
                    label = stringResource(R.string.prefer_system_binaries),
                    description = stringResource(R.string.prefer_system_binaries_desc),
                    checked = settings.isPreferSystemBinariesEnabled,
                    onSwitchChange = { settings.setPreferSystemBinaries(it) },
                    showDivider = axeronRunning
                )
            }

            SettingsSectionHeader(stringResource(R.string.settings_section_general))
            SettingsGroup {
                SettingsRow(
                    iconVector = Icons.Filled.RestartAlt,
                    label = stringResource(R.string.active_on_boot),
                    description = stringResource(R.string.active_on_boot_desc),
                    checked = settings.isActivateOnBootEnabled,
                    onSwitchChange = { settings.setActivateOnBoot(it) }
                )

                SettingsRow(
                    iconVector = Icons.Filled.Refresh,
                    label = stringResource(R.string.ignite_when_relog),
                    description = stringResource(R.string.ignite_when_relog_desc),
                    checked = settings.isIgniteWhenRelogEnabled,
                    onSwitchChange = { settings.setIgniteWhenRelog(it) },
                    showDivider = true
                )
            }

            if (axeronRunning) {
                SettingsSectionHeader(stringResource(R.string.settings_section_maintenance))
                SettingsGroup {
                    SettingsRow(
                        iconVector = Icons.Filled.FolderDelete,
                        label = stringResource(R.string.reset_path),
                        description = stringResource(R.string.reset_path_desc),
                        onClick = { showResetDialog = true }
                    )
                }
            }

            SettingsSectionHeader(stringResource(R.string.settings_section_more))
            SettingsGroup {
                if (axeronRunning) {
                    SettingsRow(
                        iconVector = Icons.Filled.Edit,
                        label = stringResource(R.string.settings_editor),
                        onClick = { navigator.navigate(SettingsEditorScreenDestination) },
                        trailing = {
                            IconButton(onClick = { showQuickSettings = true }) {
                                Icon(
                                    Icons.Outlined.Bolt,
                                    contentDescription = stringResource(R.string.quick_settings),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }

                SettingsRow(
                    iconVector = Icons.Filled.Palette,
                    label = stringResource(R.string.appearance),
                    onClick = { navigator.navigate(AppearanceScreenDestination) },
                    showDivider = axeronRunning
                )

                SettingsRow(
                    iconVector = Icons.Filled.BugReport,
                    label = stringResource(R.string.developer),
                    onClick = { navigator.navigate(DeveloperScreenDestination) },
                    showDivider = true
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    SectionHeader(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperInfo(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/fahrez182"
    val telegramUrl = "https://t.me/fahrezone"
    val sociabuzzUrl = "https://sociabuzz.com/fahrezone/tribe"

    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF303030)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fahrez182),
                        contentDescription = "Developer Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF303030))
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "fahrez182 (FahrezONE)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.developer_and_maintainer),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "“Everything is an Idea”",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tombol GitHub
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(githubUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.github))
                    }

                    // Tombol Telegram
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(telegramUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = "Telegram",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.telegram))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = { uriHandler.openUri(sociabuzzUrl) },
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Coffee,
                        contentDescription = "Support / Donate",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.support_or_donate))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun isShizukuAppInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo(ServerConstants.SHIZUKU_MANAGER_APPLICATION_ID, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetBottomSheet(
    onDismissRequest: () -> Unit,
    isRoot: Boolean,
    onReset: (rootMode: Boolean, both: Boolean) -> Unit
) {
    val error = MaterialTheme.colorScheme.error
    val rootPath = PathHelper.getWorkingPath(true, AxeronApiConstant.folder.PARENT).absolutePath
    val shellPath = PathHelper.getWorkingPath(false, AxeronApiConstant.folder.PARENT).absolutePath

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero, in the same style as the other destructive sheets: what this does.
            Surface(
                shape = AxCardShape,
                color = error.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, error.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp)
                ) {
                    Surface(shape = CircleShape, color = error, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.reset_path),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.reset_path_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Root and shell keep separate folders, so each can be reset on its own.
            // Only a root server can reach the root folder; shell mode resets its own.
            if (isRoot) {
                ResetOptionItem(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = stringResource(R.string.reset_root),
                    paths = listOf(rootPath),
                    current = true,
                    onClick = { onReset(true, false) }
                )
            }
            ResetOptionItem(
                icon = Icons.Default.Terminal,
                title = stringResource(if (isRoot) R.string.reset_shell else R.string.reset),
                paths = listOf(shellPath),
                current = !isRoot,
                onClick = { onReset(false, false) }
            )
            if (isRoot) {
                ResetOptionItem(
                    icon = Icons.Outlined.Layers,
                    title = stringResource(R.string.reset_both),
                    paths = listOf(rootPath, shellPath),
                    onClick = { onReset(true, true) }
                )
            }
        }
    }
}

/** One reset target: its icon, name, the folder(s) it wipes, and a mark on the current mode. */
@Composable
fun ResetOptionItem(
    icon: ImageVector,
    title: String,
    paths: List<String>,
    onClick: () -> Unit,
    current: Boolean = false
) {
    val error = MaterialTheme.colorScheme.error
    Surface(
        onClick = onClick,
        shape = AxPanelShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (current) error.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
        ) {
            IconBadge(imageVector = icon, tint = error)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (current) {
                        Spacer(Modifier.width(8.dp))
                        StatusPill(text = stringResource(R.string.reset_current_mode), color = error)
                    }
                }
                paths.forEach { path ->
                    Text(
                        text = path,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
