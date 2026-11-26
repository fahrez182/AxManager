package frb.axeron.manager.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import frb.axeron.manager.R
import frb.axeron.manager.api.AutoResolutionManager
import frb.axeron.manager.api.AxBladeInjector
import frb.axeron.manager.api.GameGraphicsModule
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
    private lateinit var graphicsModule: GameGraphicsModule

    private val COD_PACKAGE = "com.activision.callofduty.shooter"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnBoost = findViewById(R.id.btnBoost)
        btnReset = findViewById(R.id.btnReset)
        tvStatus = findViewById(R.id.tvStatus)
        rgModes = findViewById(R.id.rgModes)
        etCustomDpi = findViewById(R.id.etCustomDpi)

        injector = AxBladeInjector(this)
        resolutionManager = AutoResolutionManager(this, injector)
        graphicsModule = GameGraphicsModule(applicationContext)

        btnBoost.setOnClickListener {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "دسترسی تغییر تنظیمات را بده", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            tvStatus.text = "Status: Injecting..."
            val customText = etCustomDpi.text.toString()

            lifecycleScope.launch {
                val result = if (customText.isNotEmpty()) {
                    val userDpi = customText.toIntOrNull() ?: 320
                    resolutionManager.applyCustomBoost(userDpi)
                } else {
                    val mode = when (rgModes.checkedRadioButtonId) {
                        R.id.rbUltra -> "ULTRA_FPS"
                        R.id.rbHigh -> "HIGH_FPS"
                        else -> "SMOOTH"
                    }
                    resolutionManager.applyProBoost(mode)
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is InjectionResult.Success -> {
                            tvStatus.text = "Status: Success!"
                            updateNumber()
                        }
                        is InjectionResult.Failure -> tvStatus.text = "Status: Failed - ${result.reason}"
                    }
                }
            }
        }

        btnReset.setOnClickListener {
            tvStatus.text = "Status: Resetting..."
            lifecycleScope.launch {
                val result = resolutionManager.reset()
                withContext(Dispatchers.Main) {
                    tvStatus.text = if (result is InjectionResult.Success) {
                        "Status: Reset Done"
                    } else {
                        "Status: Reset Failed"
                    }
                    updateNumber()
                }
            }
        }

        val tvFpsNumber = findViewById<TextView?>(R.id.tvFpsNumber)
        try {
            tvFpsNumber?.text = graphicsModule.getCurrentNumber().toString()
        } catch (_: Exception) {}

        findViewById<Button?>(R.id.btnFps120)?.setOnClickListener {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "دسترسی تغییر تنظیمات را بده", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            val r = graphicsModule.enableUltraPerformance(COD_PACKAGE)
            if (r is InjectionResult.Success) {
                tvStatus.text = "Status: 120 Applied!"
                updateNumber()
                Toast.makeText(this, "عدد شد 120", Toast.LENGTH_SHORT).show()
                checkOverlayAndStart()
            } else {
                tvStatus.text = "Status: Failed - ${(r as InjectionResult.Failure).reason}"
            }
        }

        findViewById<Button?>(R.id.btnRestartGame)?.setOnClickListener {
            tvStatus.text = "Status: Restarting..."
            graphicsModule.resetToDefault(COD_PACKAGE)
            graphicsModule.restartGame(COD_PACKAGE)
            Toast.makeText(this, "بازی ریستارت شد", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button?>(R.id.btnFloating)?.setOnClickListener {
            checkOverlayAndStart()
        }
    }

    override fun onResume() {
        super.onResume()
        updateNumber()
    }

    private fun updateNumber() {
        try {
            val tv = findViewById<TextView?>(R.id.tvFpsNumber) ?: return
            tv.text = graphicsModule.getCurrentNumber().toString()
        } catch (_: Exception) {}
    }

    private fun checkOverlayAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "دسترسی پنجره شناور را بده", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            val i = Intent(this, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            Toast.makeText(this, "پنجره روی لابی فعال شد", Toast.LENGTH_SHORT).show()
        }
    }
}
