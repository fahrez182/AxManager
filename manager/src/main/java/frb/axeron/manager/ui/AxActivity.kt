package frb.axeron.manager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.navigation.dependency
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal

class AxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val settingsViewModel: SettingsViewModel = viewModel()
                    val appsViewModel: AppsViewModel = viewModel()
                    val privilegeViewModel: PrivilegeViewModel = viewModel()
                    val activateViewModel: ActivateViewModel = viewModel()
                    val pluginViewModel: PluginViewModel = viewModel()

                    val viewModelGlobal = ViewModelGlobal(
                        settingsViewModel = settingsViewModel,
                        appsViewModel = appsViewModel,
                        privilegeViewModel = privilegeViewModel,
                        activateViewModel = activateViewModel,
                        pluginViewModel = pluginViewModel
                    )

                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        dependenciesContainerBuilder = {
                            dependency(viewModelGlobal)
                        }
                    )
                }
            }
        }
    }
}
