package frb.axeron.manager.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import frb.axeron.api.Axeron
import frb.axeron.ktx.workerHandler
import frb.axeron.manager.R
import frb.axeron.manager.ui.theme.AxManagerTheme
import frb.axeron.server.util.Logger
import frb.axeron.shared.ShizukuApiConstant.REQUEST_PERMISSION_REPLY_ALLOWED
import frb.axeron.shared.ShizukuApiConstant.REQUEST_PERMISSION_REPLY_IS_ONETIME
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            Axeron.dispatchPermissionConfirmationResult(uid, pid, code, data)
        } catch (e: Throwable) {
            LOGGER.e("dispatchPermissionConfirmationResult")
        }
    }

    private fun waitForBinder(): Boolean {
        val latch = CountDownLatch(1)
        val listener = object : Axeron.OnBinderReceivedListener {
            override fun onBinderReceived() {
                latch.countDown()
                Axeron.removeBinderReceivedListener(this)
            }
        }
        Axeron.addBinderReceivedListenerSticky(listener, workerHandler)
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
        val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("applicationInfo", ApplicationInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("applicationInfo")
        }

        if (uid == -1 || pid == -1 || ai == null || !waitForBinder()) {
            finish()
            return
        }

        val permission =
            Axeron.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED

        if (!permission) {
            setResult(uid, pid, requestCode, allowed = false, onetime = true)
            return
        }

        setContent {
            AxManagerTheme {
                RequestPermissionDialog(
                    applicationInfo = ai,
                    onAllow = { once ->
                        setResult(uid, pid, requestCode, allowed = true, onetime = once)
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
    onAllow: (once: Boolean) -> Unit,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                AppIcon(
                    applicationInfo = applicationInfo,
                    pm = pm,
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
                    text = stringResource(R.string.uid_value, uid),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_axeron),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.permission_request),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permission_request_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            // Tombol aksi + countdown
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onAllow(false) }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.permission_allow_all_time))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onAllow(true) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.permission_allow_one_time))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDeny() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.permission_dont_allow_countdown, remaining))
                }

            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AppIcon(
    applicationInfo: ApplicationInfo,
    pm: PackageManager,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageDrawable(pm.getApplicationIcon(applicationInfo))
                clipToOutline = true
                layoutParams = android.widget.LinearLayout.LayoutParams(120, 120)
            }
        },
        modifier = modifier
    )
}