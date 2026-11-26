package frb.axeron.manager.ui.screen

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import frb.axeron.manager.R
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.GameGraphicsModule
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var injector: AxBladeInjector
    private lateinit var graphicsModule: GameGraphicsModule
    private val COD_PACKAGE = "com.activision.callofduty.shooter"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // مقداردهی اولیه
        injector = AxBladeInjector()
        graphicsModule = GameGraphicsModule(injector)

        val btnActivate = findViewById<Button>(R.id.btnActivateFPS)
        val btnReset = findViewById<Button>(R.id.btnResetSettings)

        // دکمه فعال‌سازی
        btnActivate.setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "در حال فعال‌سازی...", Toast.LENGTH_SHORT).show()
                val result = graphicsModule.enableUltraPerformance(COD_PACKAGE)
                
                when (result) {
                    is InjectionResult.Success -> {
                        Toast.makeText(this@MainActivity, "آماده! ۱۲۰ فریم فعال شد.", Toast.LENGTH_LONG).show()
                    }
                    is InjectionResult.Failure -> {
                        Toast.makeText(this@MainActivity, "خطا: ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // دکمه ریست (بسیار مهم)
        btnReset.setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "در حال بازگشت به حالت عادی...", Toast.LENGTH_SHORT).show()
                val result = graphicsModule.resetToDefault(COD_PACKAGE)
                
                when (result) {
                    is InjectionResult.Success -> {
                        Toast.makeText(this@MainActivity, "گوشی به حالت عادی برگشت.", Toast.LENGTH_LONG).show()
                    }
                    is InjectionResult.Failure -> {
                        Toast.makeText(this@MainActivity, "خطا در ریست: ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
