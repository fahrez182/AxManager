package com.frb.axmanager.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.frb.axmanager.R
import com.frb.axmanager.ui.navigation.ScreenItem
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(navController: NavHostController, viewModelGlobal: ViewModelGlobal) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(ScreenItem.AddApps.route) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Plugin")
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plugins",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                },
                windowInsets = WindowInsets(top = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
//            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            DummyPlugin()
        }
    }
}

fun formatSize(size: Long): String {
    if (size == 0L) return "null"
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        size >= gb -> String.format("%.2f GB", size.toDouble() / gb)
        size >= mb -> String.format("%.2f MB", size.toDouble() / mb)
        size >= kb -> String.format("%.2f KB", size.toDouble() / kb)
        else -> "$size B"
    }
}

@Preview
@Composable
fun DummyPlugin() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
//            .clickable(
//                onClick = onExpandToggle
//            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            val useBanner = true

            if (useBanner /* && module.banner.isNotEmpty() */) {
                val isDark = isSystemInDarkTheme()
                val colorScheme = MaterialTheme.colorScheme
                val context = LocalContext.current
                val amoledMode = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("amoled_mode", false)
                val isDynamic = colorScheme.primary != colorScheme.secondary

                val fadeColor = when {
                    amoledMode && isDark -> Color.Black
                    isDynamic -> colorScheme.surface
                    isDark -> Color(0xFF222222)
                    else -> Color.White
                }

                val banner = "/banner.png"

                Box(
                    modifier = Modifier
                        .matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (banner.startsWith("https", true) || banner.startsWith("http", true)) {
                        AsyncImage(
                            model = banner,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.18f
                        )
                    } else {
//                        val bannerData = remember(banner) {
//                            try {
//                                val file = SuFile("/data/adb/modules/${module.id}/${module.banner}")
//                                file.newInputStream().use { it.readBytes() }
//                            } catch (e: Exception) {
//                                null
//                            }
//                        }
//                        if (bannerData != null) {
//                            AsyncImage(
//                                model = ImageRequest.Builder(context)
//                                    .data(bannerData)
//                                    .build(),
//                                contentDescription = null,
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .fillMaxHeight(),
//                                contentScale = ContentScale.Crop,
//                                alpha = 0.18f
//                            )
//                        }
                        Image(
                            painter = painterResource(R.drawable.ic_axeron),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.18f
                        )

                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = 0.8f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }
            }

            val remove = false
            val update = false
            val updateUrl = "https"
            val enabled = true
            val hasWebShell = true
            val hasActionScript = true

            Column {
                val interactionSource = remember { MutableInteractionSource() }

                var developerOptionsEnabled by rememberSaveable {
                    mutableStateOf(
                        prefs.getBoolean("enable_developer_options", true)
                    )
                }

//                val filterZygiskModules = Natives.isZygiskEnabled() || !module.zygiskRequired

                LaunchedEffect(Unit) {
                    developerOptionsEnabled = prefs.getBoolean("enable_developer_options", true)
                }

                Column(
                    modifier = Modifier
                        .padding(22.dp, 18.dp, 22.dp, 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
//                        val moduleName = "Test Function"
//                        val moduleDesc = "A Test Function"
                        val moduleVersion = "Version: "
                        val moduleAuthor = "Author: "
                        val moduleId = "ID "
                        val moduleVersionCode = "VersionCode: "
                        val moduleUpdateJson = "Update: "
                        val moduleUpdateJsonEmpty = "No Update"
                        val updateJson = "{}"

                        Column(
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LabelItem(
                                    text = formatSize(200000),
                                    style = LabelItemDefaults.style.copy(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                                if (remove) {
                                    LabelItem(
                                        text = "Uninstalled",
                                        style = LabelItemDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                                if (updateUrl.isNotEmpty() && !remove && !update) {
                                    LabelItem(
                                        text = "Update",
                                        style = LabelItemDefaults.style.copy(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                                if (!remove) {
                                    if (update) {
                                        LabelItem(
                                            text = "Update2",
                                            style = LabelItemDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        )
                                    }
                                }
                                if (enabled && !remove) {
                                    if (hasWebShell) {
                                        LabelItem(
                                            text = "WebShell",
                                            style = LabelItemDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                    if (hasActionScript) {
                                        LabelItem(
                                            text = "Action",
                                            style = LabelItemDefaults.style.copy(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Test Funtion",
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.titleMedium.fontFamily
                            )

                            Text(
                                text = "$moduleVersion: Test",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                            )

                            Text(
                                text = "$moduleAuthor: FahrezONE",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                            )

                            if (developerOptionsEnabled) {

                                Text(
                                    text = "$moduleId: Test-Function",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )

                                Text(
                                    text = "$moduleVersionCode: 100",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )

                                Text(
                                    text = if (updateJson.isNotEmpty()) "$moduleUpdateJson: $updateJson" else "$moduleUpdateJson: $moduleUpdateJsonEmpty",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Switch(
                                enabled = !update,
                                checked = enabled,
                                onCheckedChange = {
                                    Toast.makeText(context, "Switch: $it", Toast.LENGTH_SHORT).show()
                                },
                                interactionSource = if (!hasWebShell) interactionSource else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Just A Test Function",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val expanded = true

                    if (expanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasActionScript) {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = !remove && enabled,
                                    onClick = {
                                        Toast.makeText(context, "Action", Toast.LENGTH_SHORT).show()
//                                        navigator.navigate(
//                                            ExecuteModuleActionScreenDestination(
//                                                module.dirId
//                                            )
//                                        )
//                                        viewModel.markNeedRefresh()
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Terminal,
                                        contentDescription = null
                                    )
                                    if (!hasWebShell && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            text = "Action",
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(0.1f, true))
                            }

                            if (hasWebShell) {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = !remove && enabled,
                                    onClick = {
                                        Toast.makeText(context, "WebShell", Toast.LENGTH_SHORT).show()
                                    },
                                    interactionSource = interactionSource,
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.AutoMirrored.Outlined.Wysiwyg,
                                        contentDescription = null
                                    )
                                    if (!hasActionScript && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Exec"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f, true))

                            if (updateUrl.isNotEmpty() && !remove && !update) {
                                Button(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = !remove,
                                    onClick = {
                                        Toast.makeText(context, "Update", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = ButtonDefaults.textShape,
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null
                                    )
                                    if (!hasActionScript || !hasWebShell) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Module Update"
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(0.1f, true))
                            }

                            if (remove) {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    onClick = {
                                        Toast.makeText(context, "Restore", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = null
                                    )
                                    if (!hasActionScript && !hasWebShell && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Restore"
                                        )
                                    }
                                }
                            } else {
                                FilledTonalButton(
                                    modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                    enabled = true,
                                    onClick = {
                                        Toast.makeText(context, "Uninstall", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                    if (!hasActionScript && !hasWebShell && updateUrl.isEmpty()) {
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = "Uninstall"
                                        )
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