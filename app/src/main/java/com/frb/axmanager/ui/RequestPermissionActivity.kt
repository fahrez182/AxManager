package com.frb.axmanager.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.frb.axmanager.ui.theme.AxManagerTheme
import com.frb.engine.utils.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME
import rikka.shizuku.ktx.workerHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RequestPermissionActivity : ComponentActivity() {

    private val LOGGER = Logger("RequestPermissionActivity")

    private fun setResult(uid: Int, pid: Int, code: Int, allowed: Boolean, onetime: Boolean) {
        val data = Bundle().apply {
            putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed)
            putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime)
        }
        try {
            Shizuku.dispatchPermissionConfirmationResult(uid, pid, code, data)
        } catch (e: Throwable) {
            LOGGER.e("dispatchPermissionConfirmationResult")
        }
    }

    private fun waitForBinder(): Boolean {
        val latch = CountDownLatch(1)
        val listener = object : Shizuku.OnBinderReceivedListener {
            override fun onBinderReceived() {
                latch.countDown()
                Shizuku.removeBinderReceivedListener(this)
            }
        }
        Shizuku.addBinderReceivedListenerSticky(listener, workerHandler)
        return try {
            latch.await(5, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            LOGGER.e(e, "Binder not received in 5s")
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = intent.getIntExtra("uid", -1)
        val pid = intent.getIntExtra("pid", -1)
        val requestCode = intent.getIntExtra("requestCode", -1)
        val ai = intent.getParcelableExtra<ApplicationInfo>("applicationInfo")

        if (uid == -1 || pid == -1 || ai == null || !waitForBinder()) {
            finish()
            return
        }

        val permission =
            Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED

        if (!permission) {
            setResult(uid, pid, requestCode, allowed = false, onetime = true)
            return
        }

        setContent {
            AxManagerTheme {
                RequestPermissionDialog(
                    applicationInfo = ai,
                    onAllow = {
                        setResult(uid, pid, requestCode, allowed = true, onetime = false)
                        finish()
                    },
                    onDeny = {
                        setResult(uid, pid, requestCode, allowed = false, onetime = true)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPermissionDialog(
    applicationInfo: ApplicationInfo,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appName = remember { pm.getApplicationLabel(applicationInfo).toString() }
    val uid = applicationInfo.uid

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var remaining by remember { mutableIntStateOf(10) }

    // Auto deny countdown
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        if (remaining == 0) scope.launch { sheetState.hide() }.invokeOnCompletion { onDeny() }
    }

    ModalBottomSheet(
        onDismissRequest = { onDeny() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Info Aplikasi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                AndroidView(
                    factory = {
                        ImageView(it).apply {
                            setImageDrawable(pm.getApplicationIcon(applicationInfo))
                            clipToOutline = true
                            layoutParams = android.widget.LinearLayout.LayoutParams(120, 120)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(applicationInfo.packageName, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "UID: $uid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 🔹 Konten utama
            Icon(
                painter = painterResource(id = com.frb.engine.R.drawable.ic_axeron),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Permission Request",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "The app requires permission to continue operating.\nGrant permission so the feature can run properly.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            // 🔹 Tombol aksi + countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDeny() }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Deny (${remaining}s)")
                }
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onAllow() }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}