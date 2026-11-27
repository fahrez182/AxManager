package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>(start = true) // مشخص کردن به عنوان صفحه اول
@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "AX MANAGER HOME", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(text = "Welcome, Amirhossein!")
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // دکمه انتقال به بخش‌های دیگر
        Button(onClick = { /* Navigation logic will go here */ }) {
            Text("Go to Activation")
        }
    }
}
