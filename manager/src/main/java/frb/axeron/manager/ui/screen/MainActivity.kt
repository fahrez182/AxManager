package frb.axeron.manager.ui.screen

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import frb.axeron.manager.R
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.InjectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var btnBoost: Button
    private lateinit var btnReset: Button
    private lateinit var tvStatus: TextView
    private lateinit var rgModes: RadioGroup
    private lateinit var etCustomDpi: TextInputEditText

    private lateinit var injector: AxBladeInjector
    private lateinit var resolutionManager: AutoResolutionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // مقداردهی ویجت‌ها از لایوت
        btnBoost = findViewById(R.id.btnBoost)
        btnReset = findViewById(R.id.btnReset)
        tvStatus = findViewById(R.id.tvStatus)
        rgModes = findViewById(R.id.rgModes)
        etCustomDpi = findViewById(R.id.etCustomDpi)

        // مقداردهی موتورهای اجرایی (بدون آرگومان اضافی برای جلوگیری از خطای Too many arguments)
        injector = AxBladeInjector()
        resolutionManager = AutoResolutionManager(this, injector)

        // مدیریت دکمه اعمال بوست / ابعاد
        btnBoost.setOnClickListener {
            tvStatus.text = "Status: Injecting Boost..."
            
            val customText = etCustomDpi.text.toString()
            
            lifecycleScope.launch {
                val result = if (customText.isNotEmpty()) {
                    // اگر کاربر عدد DPI دستی وارد کرده بود
                    val userDpi = customText.toIntOrNull() ?: 320
                    resolutionManager.applyCustomBoost(userDpi)
                } else {
                    // در غیر این صورت استفاده از حالت‌های پیش‌فرض گزینه‌ها
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

        // مدیریت دکمه ریست و بازنشانی سیستم
        btnReset.setOnClickListener {
            tvStatus.text = "Status: Resetting System..."
            lifecycleScope.launch {
                val result = resolutionManager.reset()
                withContext(Dispatchers.Main) {
                    tvStatus.text = if (result is InjectionResult.Success) {
                        "Status: Restored to Default"
                    } else {
                        "Status: Reset Failed"
                    }
                }
            }
        }
    }
}
