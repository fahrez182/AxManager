package frb.axeron.manager.ui.component

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import frb.axeron.manager.R
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedMoreStepDialog(
    applicationInfo: ApplicationInfo? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val uriHandler = LocalUriHandler.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    android.util.Log.d("NeedMoreStepDialog", "Rendering NeedMoreStepDialog for ${applicationInfo?.packageName ?: "System Card"}")

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (applicationInfo != null) {
                val appName = remember { pm.getApplicationLabel(applicationInfo).toString() }
                val uid = applicationInfo.uid
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
                            .clip(AxPanelShape)
                    )

                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(applicationInfo.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = stringResource(R.string.uid_value, uid),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_axeron),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.permission_need_more_step),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permission_grant_runtime_not_allowed),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    uriHandler.openUri("https://fahrez182.github.io/AxManager/guide/faq.html#start-via-wireless-debugging-start-by-connecting-to-a-computer-the-permission-of-adb-is-limited")
                },
                shape = AxPanelShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.learn_more))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                shape = AxPanelShape
            ) {
                Text(stringResource(R.string.close))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
