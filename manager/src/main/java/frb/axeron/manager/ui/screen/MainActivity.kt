package frb.axeron.manager.ui.screen

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import frb.axeron.manager.R
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.AxBladeInjector

class MainActivity : AppCompatActivity() {

    private lateinit var etCustomDpi: TextInputEditText
    private lateinit var rgModes: RadioGroup
    private lateinit var btnBoost: Button
    private lateinit var btnReset: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // اتصال المان‌های ویژوال به متغیرها
        etCustomDpi = findViewById(R.id.etCustomDpi)
        rgModes = findViewById(R.id.rgModes)
        btnBoost = findViewById(R.id.btnBoost)
        btnReset = findViewById(R.id.btnReset)
        tvStatus = findViewById(R.id.tvStatus)

        // دکمه اعمال تنظیمات و بهینه‌سازی
        btnBoost.setOnClickListener {
            val dpiStr = etCustomDpi.text.toString().trim()
            val dpi = if (dpiStr.isNotEmpty()) dpiStr.toInt() else 360

            val selectedModeId = rgModes.checkedRadioButtonId
            val modeName = when (selectedModeId) {
                R.id.rbHigh -> "High"
                R.id.rbUltra -> "Ultra"
                else -> "Smooth"
            }

            try {
                // استفاده از موتورهای Blade و Resolution برای اعمال فرمان بدون نیاز به Root
                AxBladeInjector.injectCommand("wm density $dpi")
                AutoResolutionManager.applyPreset(modeName, dpi)

                tvStatus.text = "Status: Successfully Applied ($modeName, DPI: $dpi)"
                Toast.makeText(this, "Optimization Applied!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                tvStatus.text = "Status: Error -> ${e.message}"
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // دکمه بازنشانی به حالت پیش‌فرض
        btnReset.setOnClickListener {
            try {
                AxBladeInjector.injectCommand("wm density reset")
                AutoResolutionManager.resetDefaults()
                
                etCustomDpi.setText("")
                tvStatus.text = "Status: Reset to Default"
                Toast.makeText(this, "Reset Successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                tvStatus.text = "Status: Reset Error"
            }
        }
    }
}
