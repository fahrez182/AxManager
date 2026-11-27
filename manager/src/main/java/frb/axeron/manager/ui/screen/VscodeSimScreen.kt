package frb.axeron.manager.ui.screen

import android.content.Context
import android.os.BatteryManager
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.AxeronCommandSession
import frb.axeron.manager.ui.component.AxTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ServerSocket

fun isDangerSim(s: String): String? {
    val b = listOf("rm -rf /", "rm -rf /*", "dd if=", "mkfs", "reboot", "shutdown", ":(){:|:&};:")
    val l = s.lowercase()
    for (x in b) if (l.contains(x)) return x
    return null
}

fun freePort(def: Int): Int {
    return try { ServerSocket(0).use { it.localPort } } catch (_: Exception) { def }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun VscodeSimScreen(navigator: DestinationsNavigator) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("echo hello from sim\nid\ngetprop ro.product.model") }
    var out by remember { mutableStateOf("آماده... اول ⚡ را بزن") }
    var lang by remember { mutableStateOf("sh") }
    var sysMode by remember { mutableStateOf(false) } // false=داخل شبیه‌ساز امن، true=روی سیستم
    var portTxt by remember { mutableStateOf("8080") }
    var pathTxt by remember { mutableStateOf("") }
    var fa by remember { mutableStateOf(true) }
    var aiQ by remember { mutableStateOf("") }

    val simDir = remember { File(ctx.filesDir, "sim/workspace").apply { mkdirs() }.absolutePath + "/" + "www".let { File(ctx.filesDir, "sim/workspace/www").apply { mkdirs() }; "" } }
    // sim root:
    val simRoot = remember { File(ctx.filesDir, "sim/workspace").absolutePath }

    val session = remember {
        AxeronCommandSession().apply {
            setResultListener(object : AxeronCommandSession.ResultListener {
                override fun output(o: CharSequence?) { o?.let { out += "\n$it" } }
                override fun onError(e: CharSequence?) { e?.let { out += "\nERR:$it" } }
            })
            setProcessListener(object : AxeronCommandSession.ProcessListener {
                override fun onProcessCreated(pid: Int, command: String) { out += "\n[start] pid=$pid" }
                override fun onProcessRunning(input: String) {}
                override fun onProcessBackground(pids: IntArray) { out += "\n[bg] ${pids.joinToString()} " }
                override fun onProcessFinished(exitCode: Int, lastOutput: String) { out += "\n[exit] $exitCode" }
            })
        }
    }

    fun runServerCmd(raw: String) {
        val block = if (!sysMode) isDangerSim(raw) else null
        if (block != null) { out = "⛔ در شبیه‌ساز بسته شد: $block"; return }
        val full = if (sysMode) raw else "mkdir -p '$simRoot' '$simRoot/www' && cd '$simRoot' && timeout 20s sh -c \"$raw\" 2>&1"
        out = "$ " + (if (sysMode) "[SYSTEM] " else "[SIM] ") + raw + "\n"
        session.runCommand(full, true)
    }

    fun runEditor() {
        val b64 = Base64.encodeToString(code.toByteArray(), Base64.NO_WRAP)
        val ext = mapOf("sh" to "main.sh", "py" to "main.py", "html" to "index.html", "c" to "main.c", "cpp" to "main.cpp", "java" to "Main.java")[lang] ?: "main.txt"
        val fp = "$simRoot/$ext"
        scope.launch {
            withContext(Dispatchers.IO) {
                // ذخیره فایل با base64 تا فارسی و علائم خراب نشود
                File(fp).parentFile?.mkdirs()
            }
            // ذخیره از طریق سرور تا permission درست باشد
            out = "save $fp\n"
            session.runCommand("echo '$b64' | base64 -d > '$fp' && chmod 644 '$fp' && ls -lh '$fp' 2>&1", true)
        }
        // اجرا بعد از 800ms
        scope.launch {
            kotlinx.coroutines.delay(900)
            val cmd = when (lang) {
                "sh" -> if (sysMode) "sh '$fp' 2>&1" else "cd '$simRoot' && timeout 20s sh './$ext' 2>&1"
                "py" -> if (sysMode) "python3 '$fp' 2>&1" else "cd '$simRoot' && timeout 20s python3 './$ext' 2>&1"
                "html" -> { out += "\nHTML ذخیره شد، پیش‌نمایش در WebView جدا باز می‌شود: $fp"; return@launch }
                "c", "cpp" -> { out = "فایل ذخیره شد:$fp\nکامپایلر را تست کن: which clang tcc gcc"; session.runCommand("which clang tcc gcc busybox 2>&1", true); return@launch }
                "java" -> { out = "فایل ذخیره شد:$fp\nجاوا روی گوشی مستقیم اجرا نمی‌شود، Termux + ecj لازم است"; return@launch }
                else -> "sh '$fp' 2>&1"
            }
            runServerCmd(cmd)
        }
    }

    Scaffold(topBar = {
        AxTopAppBar(title = { Text(if (fa) "★ شبیه‌ساز VSCode 4K آفلاین" else "★ VSCode Sim 4K Offline") },
            navigationIcon = { IconButton(onClick = { navigator.popBackStack() }) { Icon(Icons.Outlined.Code, null) } })
    }) { pv ->
        Column(Modifier.padding(pv).verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { fa = true }) { Text("فارسی") }
                Button(onClick = { fa = false }) { Text("EN") }
                AssistChip(onClick = {}, label = { Text(if (sysMode) (if (fa) "روی سیستم: روشن" else "SYSTEM: ON") else (if (fa) "داخل شبیه‌ساز" else "SIM")) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var exp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                    TextField(value = lang, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().weight(1f), label = { Text("lang") })
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        listOf("sh", "py", "html", "c", "cpp", "java").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { lang = it; exp = false })
                        }
                    }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = sysMode, onCheckedChange = { sysMode = it })
                    Text(if (fa) " سیستم" else " sys")
                }
            }
            TextField(value = code, onValueChange = { code = it }, modifier = Modifier.fillMaxWidth().height(220.dp), label = { Text("code") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { runEditor() }) { Text("▶ اجرا") }
                FilledTonalButton(onClick = {
                    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val lvl = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    out = "⚡ level=$lvl%\n"
                    runServerCmd("echo ===BATTERY===; dumpsys battery | head -n 20; echo ===CPU===; cat /proc/cpuinfo | grep processor | wc -l; echo ===MEM===; cat /proc/meminfo | head -n 5; echo ===DISK===; df -h /data | head -n 5; echo ===ID===; id")
                }) { Text("⚡ قدرت") }
            }
            // پورت آفلاین بدون وای‌فای
            Card { Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (fa) "🚀 پورت آفلاین بدون وای‌فای (127.0.0.1) - هر پورت غیر از 5555" else "offline port 127.0.0.1 any except 5555")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextField(value = portTxt, onValueChange = { portTxt = it.filter { c -> c.isDigit() }.take(5) }, modifier = Modifier.weight(1f), label = { Text("port e.g. 8080") })
                    Button(onClick = {
                        val p = portTxt.ifBlank { "8080" }
                        if (p == "5555") { out = "⛔ 5555 مال TCP خود AxManager است، 8080 بگذار"; return@Button }
                        out = "[LOCAL $p] starting...\n"
                        session.runCommand("mkdir -p '$simRoot/www' && echo '<h1>SIM OK $p</h1>' > '$simRoot/www/index.html' && cd '$simRoot/www' && (which python3 >/dev/null 2>&1 && nohup python3 -m http.server $p >/dev/null 2>&1 & || busybox httpd -p $p -h . 2>&1 &) ; sleep 1; curl -I 127.0.0.1:$p 2>&1 | head -n 20", true)
                    }) { Text(if (fa) "روشن" else "ON") }
                    FilledTonalButton(onClick = {
                        val p = portTxt.ifBlank { "8080" }
                        session.runCommand("pkill -f 'http.server $p' 2>&1; fuser -k $p/tcp 2>&1; echo stopped:$p", true)
                    }) { Text("OFF") }
                }
                Text("تست: curl -I 127.0.0.1:$portTxt", style = MaterialTheme.typography.bodySmall)
            }}
            TextField(value = pathTxt, onValueChange = { pathTxt = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (fa) "مسیر فایل /sdcard/Download یا $simRoot" else "path") })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(onClick = { runServerCmd("ls -lh '${if (pathTxt.isBlank()) simRoot else pathTxt}' 2>&1") }) { Text("📁") }
                FilledTonalButton(onClick = { if (pathTxt.isNotBlank()) runServerCmd("cat '$pathTxt' 2>&1 | head -n 200") }) { Text(if (fa) "باز" else "open") }
                FilledTonalButton(onClick = {
                    val p = portTxt.ifBlank { "8080" }
                    runServerCmd("ss -tln 2>&1 || netstat -tln 2>&1")
                }) { Text("ports") }
            }
            TextField(value = aiQ, onValueChange = { aiQ = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (fa) "از دستیار بپرس: باتری را نشان بده" else "ask: show battery") })
            Button(onClick = {
                val q = aiQ
                val sug = when {
                    q.contains("باتری") -> "dumpsys battery | head -n 20"
                    q.contains("مدل") || q.contains("گوشی") -> "getprop ro.product.model; getprop ro.build.version.release; id"
                    q.contains("حافظه") -> "df -h /data; cat /proc/meminfo | head"
                    q.contains("برنامه") || q.contains("apk") -> "pm list packages | head -n 30"
                    else -> "ls -lh '$simRoot'; id"
                }
                out = "🤖 $q\n> $sug\n"
                runServerCmd(sug)
            }) { Text("دستیار") }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(out, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            }
            TextField(value = "", onValueChange = { if (it.endsWith("\n")) { runServerCmd(it.trim()); } }, label = { Text("terminal: ls -la | id") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(40.dp))
        }
    }
    DisposableEffect(Unit) { onDispose { try { session.killSession() } catch (_: Exception) {} } }
}
