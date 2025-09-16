package com.frb.axmanager.ui.screen

import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import com.frb.axmanager.R
import com.frb.axmanager.ui.component.AxSnackBarHost
import com.frb.axmanager.ui.util.LocalSnackbarHost
import com.frb.axmanager.ui.viewmodel.QuickShellViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal
import com.frb.engine.client.Axeron
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun QuickShellScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    LocalContext.current
    val viewModel: QuickShellViewModel = viewModel()
    val running = viewModel.isRunning

    val listState = rememberLazyListState()
    val logs = remember { mutableStateListOf<QuickShellViewModel.Output>() }

    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    // FAB visibility
    var fabVisible by remember { mutableStateOf(true) }

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(listState, viewModel.isRunning) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (!viewModel.isRunning) {  // hanya update FAB kalau sedang tidak running
                    if (index > previousIndex || (index == previousIndex && offset > previousScrollOffset)) {
                        fabVisible = false
                    } else if (index < previousIndex || offset < previousScrollOffset) {
                        fabVisible = true
                    }
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }

    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QuickShell",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.stop()
                        },
                        enabled = running,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
                    IconButton(onClick = {
                        viewModel.clear()
                    }) {
                        Icon(Icons.Filled.ClearAll, contentDescription = "Clear")
                    }
                },
                windowInsets = WindowInsets(top = 0)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
            ) {
                FloatingActionButton(onClick = {
                    scope.launch {
                        saveLogsToDownload(logs, snackBarHost)
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }
        },
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            TextField(
                value = viewModel.commandText,
                onValueChange = {
                    viewModel.setCommand(it)
                },
                label = {
                    Text(viewModel.execMode)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            viewModel.runShell()
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_exec),
                            contentDescription = "Send",
                            modifier = Modifier.width(50.dp)
                        )
                    }
                },
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,   // garis saat fokus
                    unfocusedIndicatorColor = Color.Transparent, // garis saat tidak fokus
                    disabledIndicatorColor = Color.Transparent   // garis saat disabled
                ),
                shape = RoundedCornerShape(12.dp), // ubah bentuk.
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            LaunchedEffect(viewModel.clear) {
                logs.clear()
            }

            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.lastIndex)
                }
            }

            // collect flow
            LaunchedEffect(Unit) {
                viewModel.output.collect { line ->
//                    Log.d("QuickShellViewModel", line.output)
                    logs.add(line)
                }
            }

            val hScroll = rememberScrollState()

            SelectionContainer(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .horizontalScroll(hScroll)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(logs) { line ->
                            if (line.type == QuickShellViewModel.OutputType.TYPE_COMMAND) return@items
                            Text(
                                text = line.output.parseAsAnsiAnnotatedString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = MaterialTheme.typography.labelSmall.fontSize, // samain dengan fontSize
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    )
                                ),
                                softWrap = false,   // MATIIN WRAP
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


suspend fun saveLogsToDownload(logs: List<QuickShellViewModel.Output>, snackbar: SnackbarHostState) {
    val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    val date = format.format(Date())

    val baseDir = File(Environment.getExternalStorageDirectory(), "AxManager/logs")
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }

    val file = File(baseDir, "QuickShell_log_${date}.log")

    try {
        val fos = Axeron.newFileService().getStreamSession(file.absolutePath, true, false).outputStream
        logs.forEach { line ->
            fos.write("${line.output}\n".toByteArray())
        }
        fos.flush()

        snackbar.showSnackbar("Log saved to ${file.absolutePath}")
    } catch (e: Exception) {
        snackbar.showSnackbar("Failed to save logs: ${e.message}")
    }
}



