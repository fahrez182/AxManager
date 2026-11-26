package frb.axeron.manager.ui.screen

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import frb.axeron.manager.R
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etCustomDpi: TextInputEditText
    private lateinit var rgModes: RadioGroup
    private lateinit var btnBoost: Button
    private lateinit var btnReset: Button
    private lateinit var tvStatus: TextView

    // تعریف موتورها بر اساس ساختار کلاس‌های شما
    private lateinit var injector: AxBladeInjector
    private lateinit var resolutionManager: AutoResolutionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // مقداردهی اولیه موتورها (نیاز به Context و Injector دارند)
        injector = AxBladeInjector(this)
        resolutionManager = AutoResolutionManager(this, injector)

        // اتصال المان‌های UI
        etCustomDpi = findViewById(R.id.etCustomDpi)
        rgModes = findViewById(R.id.rgModes)
        btnBoost = findViewById(R.id.btnBoost)
        btnReset = findViewById(R.id.btnReset)
        tvStatus = findViewById(R.id.tvStatus)

        // دکمه اعمال تنظیمات (Boost)
        btnBoost.setOnClickListener {
            val dpiStr = etCustomDpi.text.toString().trim()
            
            lifecycleScope.launch {
                val result = if (dpiStr.isNotEmpty()) {
                    // اگر کاربر DPI دستی وارد کرد
                    val customDpi = dpiStr.toIntOrNull() ?: 360
                    resolutionManager.applyCustomBoost(customDpi)
                } else {
                    // اگر فقط حالت‌های آماده را انتخاب کرد
                    val selectedId = rgModes.checkedRadioButtonId
                    val mode = when (selectedId) {
                        R.id.rbUltra -> "ULTRA_FPS"
                        R.id.rbHigh -> "HIGH_FPS"
                        else -> "SMOOTH"
                    }
                    resolutionManager.applyProBoost(mode)
                }
                handleResult(result)
            }
        }

        // دکمه ریست کردن
        btnReset.setOnClickListener {
            lifecycleScope.launch {
                val result = resolutionManager.reset()
                handleResult(result)
            }
        }
    }

    private fun handleResult(result: InjectionResult) {
        when (result) {
            is InjectionResult.Success -> {
                tvStatus.text = "Status: ${result.data}"
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
            }
            is InjectionResult.Failure -> {
                tvStatus.text = "Status: Error -> ${result.reason}"
                Toast.makeText(this, "Failed: ${result.reason}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
