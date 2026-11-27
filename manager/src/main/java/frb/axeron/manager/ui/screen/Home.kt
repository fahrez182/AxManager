package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "AX MANAGER HOME", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(20.dp))
    Text(text = "Welcome, Amirhossein!")
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = { navigator.navigate(ActivateScreenDestination) }) {
      Text("Go to Activation")
    }
  }
}
