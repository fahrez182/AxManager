package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import frb.axeron.manager.ui.component.AxTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxCard
import frb.axeron.manager.ui.component.IconBadge
import frb.axeron.manager.ui.component.StatusPill
import frb.axeron.manager.ui.util.LocalFloatingBarInset
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PrivilegeDetailScreen(
    appInfo: AppsViewModel.AppInfo,
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal,
) {
    val privilegeViewModel = viewModelGlobal.privilegeViewModel
    val context = LocalContext.current

    // Prefer the live entry so grant/revoke reflects immediately.
    val live = privilegeViewModel.privileges[appInfo.uid] ?: appInfo
    val allowed = live.isAdded
    val setAllowed: (Boolean) -> Unit = { checked ->
        if (checked) privilegeViewModel.grant(appInfo.uid)
        else privilegeViewModel.revoke(appInfo.uid)
    }

    val iconRequest = remember(appInfo.packageInfo) {
        ImageRequest.Builder(context)
            .data(appInfo.packageInfo)
            .crossfade(true)
            .build()
    }

    Scaffold(
        topBar = {
            AxTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privilege),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                // Padding inside the scroll lets content pass under the fading top bar.
                .padding(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + LocalFloatingBarInset.current + 16.dp
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Identity: icon, name, package, UID and current status in one card
            AxCard {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = iconRequest,
                        contentDescription = appInfo.label,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appInfo.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = appInfo.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${stringResource(R.string.uid_label)} ${appInfo.uid}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusPill(
                        text = stringResource(
                            if (allowed) R.string.privilege_allowed else R.string.privilege_not_allowed
                        ),
                        color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            // The one action on this screen
            AxCard(onClick = { setAllowed(!allowed) }) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(imageVector = Icons.Outlined.AdminPanelSettings)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.privilege_grant),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.privilege_grant_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = allowed,
                        onCheckedChange = setAllowed
                    )
                }
            }
        }
    }
}
