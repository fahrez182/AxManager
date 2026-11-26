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
                val btnBoost = findViewById<Button>(R.id.btnBoost)
val btnReset = findViewById<Button>(R.id.btnReset)
val tvStatus = findViewById<TextView>(R.id.tvStatus)
val rgModes = findViewById<RadioGroup>(R.id.rgModes)
val etCustomDpi = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCustomDpi)

val injector = AxBladeInjector(this)
val resolutionManager = AutoResolutionManager(this, injector)

btnBoost.setOnClickListener {
    tvStatus.text = "Status: Injecting Boost..."
    
    val customText = etCustomDpi.text.toString()
    
    lifecycleScope.launch {
        val result = if (customText.isNotEmpty()) {
            // اگر عدد دلخواه وارد کرده بود، از آن استفاده کن
            val userDpi = customText.toIntOrNull() ?: 320
            resolutionManager.applyCustomBoost(userDpi)
        } else {
            // در غیر این صورت از حالت انتخابی RadioGroup استفاده کن
            val selectedMode = when (rgModes.checkedRadioButtonId) {
                R.id.rbUltra -> "ULTRA_FPS"
                R.id.rbHigh -> "HIGH_FPS"
                else -> "SMOOTH"
            }
            resolutionManager.applyProBoost(selectedMode)
        }

        withContext(Dispatchers.Main) {
            when (result) {
                is InjectionResult.Success -> {
                    tvStatus.text = "Status: Success! (Active)"
                }
                is InjectionResult.Failure -> {
                    tvStatus.text = "Status: Failed - ${result.reason}"
                }
            }
        }
    }
}

btnReset.setOnClickListener {
    tvStatus.text = "Status: Resetting System..."
    lifecycleScope.launch {
        val result = resolutionManager.reset()
        withContext(Dispatchers.Main) {
            tvStatus.text = if (result is InjectionResult.Success) "Status: Restored to Default" else "Status: Reset Failed"
        }
    }
}
