package com.frb.axmanager.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import com.frb.axmanager.ui.viewmodel.QuickShellViewModel
import com.frb.axmanager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickShellScreen(navController: NavHostController, viewModelGlobal: ViewModelGlobal) {
    val vm: QuickShellViewModel = viewModel()
//    val output by vm.output.collectAsState()
    val running by vm.isRunning.collectAsState()
    rememberScrollState()

//    LaunchedEffect(output) {
//        scroll.animateScrollTo(scroll.maxValue.coerceAtLeast(0))
//    }

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
                            fontWeight = FontWeight.Black,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(top = 0)
            )
        },
        contentWindowInsets = WindowInsets(top = 0, bottom = 0)
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = vm.commandText.collectAsState().value,
                onValueChange = { vm.setCommand(it) },
                label = {
                    Text("Commands")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { vm.runShell() }
                ) { Text("Run Shell") }

                OutlinedButton(
                    onClick = { vm.clear() }
                ) { Text("Clear") }
                Spacer(Modifier.weight(1f))

                OutlinedButton(
                    enabled = running,
                    onClick = { vm.stop() }
                ) {
                    Text("Stop")
                }
            }

            Spacer(Modifier.height(12.dp))

            val listState = rememberLazyListState()
            val logs = remember { mutableStateListOf<String>() }

            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.lastIndex)
                }
            }

            // collect flow
            LaunchedEffect(Unit) {
                vm.output.collect { line ->
                    if (line == "__CLEAR__") {
                        logs.clear()
                    } else {
                        logs.add(line)
                    }
                }
            }

            val hScroll = rememberScrollState()

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .horizontalScroll(hScroll)
                ) {
                    LazyColumn(
                        state = listState
                    ) {
                        items(logs) { line ->
                            Text(
                                text = line.parseAsAnsiAnnotatedString(),
                                fontSize = 14.sp,  // bisa fix fontSize (zoom di graphicsLayer)
                                softWrap = false,   // MATIIN WRAP
                                modifier = Modifier.fillMaxWidth(),
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

