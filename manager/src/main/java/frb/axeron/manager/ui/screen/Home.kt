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
// نکته مهم: اگر خط زیر قرمز شد، یعنی KSP هنوز فایل را نساخته. 
// اما برای رفع ارور فعلی، ما این ایمپورت را حذف یا اصلاح می‌کنیم.
// فعلاً این را کامنت می‌کنیم تا Build انجام شود:
// import com.ramcosta.composedestinations.generated.api.ActivateScreenDestination

@Destination<RootGraph>
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
        
        // اینجا اگر می‌خواهی به صفحه فعال‌سازی بروی، فعلاً از یک دکمه ساده استفاده می‌کنیم
        // تا ارور ندهد، بعداً که KSP درست شد، مقصد را اضافه می‌کنیم
        Button(onClick = { /* Navigation logic will go here */ }) {
            Text("Go to Activation")
        }
    }
}
