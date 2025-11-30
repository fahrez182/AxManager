package frb.axeron.manager.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.ExtraLabel
import frb.axeron.manager.ui.component.ExtraLabelDefaults

@Composable
@Preview
fun StatusCard() {
    val isDark = isSystemInDarkTheme()
    val colorScheme = colorScheme
    val fadeColor = when {
        isDark -> colorScheme.surfaceVariant
        else -> colorScheme.surfaceVariant
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(10.dp, 30.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier
                        .size(145.dp),
                    painter = painterResource(R.drawable.ic_axeron),
                    contentDescription = null
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                fadeColor.copy(alpha = 0.0f),
                                fadeColor.copy(alpha = 0.55f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_running),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ExtraLabel(
                        text = "Shell",
                        style = ExtraLabelDefaults.style.copy(
                            allCaps = false
                        )
                    )
                }

                Text(
                    text = "Version: 20 | Pid: 20",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.weight(1f))

                Text("Hello")
            }
        }
    }
}

@Composable
@Preview
fun PreviewCard() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PluginInfo(Modifier.weight(1f))
        PrivilegeInfo(Modifier.weight(1f))
    }
}

@Composable
@Preview
fun PluginInfo(
    modifier: Modifier = Modifier,
    countTotal: Int = 0,
) {
    val containerColor = colorScheme.surfaceVariant

    ElevatedCard(
        colors = CardDefaults.cardColors(),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(20.dp, 15.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier.size(90.dp),
                    imageVector = Icons.Filled.Extension,
                    tint = colorScheme.outline,
                    contentDescription = null
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                containerColor.copy(alpha = 0.0f),
                                containerColor.copy(alpha = 0.55f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (countTotal <= 1) {
                        "Plugin"
                    } else {
                        "Plugins"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$countTotal",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

    }
}

@Composable
@Preview
fun PrivilegeInfo(
    modifier: Modifier = Modifier,
    countTotal: Int = 0,
) {
    val containerColor = colorScheme.surfaceVariant

    ElevatedCard(
        colors = CardDefaults.cardColors(),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(15.dp, 10.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier.size(90.dp),
                    imageVector = Icons.Filled.AdminPanelSettings,
                    tint = colorScheme.outline,
                    contentDescription = null
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                containerColor.copy(alpha = 0.0f),
                                containerColor.copy(alpha = 0.55f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (countTotal <= 1) {
                        "Privilege"
                    } else {
                        "Privileges"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$countTotal",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

    }
}