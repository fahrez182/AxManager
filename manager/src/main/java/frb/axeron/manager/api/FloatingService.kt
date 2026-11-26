package frb.axeron.manager.ui.screen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import frb.axeron.manager.api.GameGraphicsModule
import frb.axeron.manager.api.InjectionResult

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: LinearLayout? = null
    private val COD_PACKAGE = "com.activision.callofduty.shooter"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        showWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) showWindow()
        return START_STICKY
    }

    private fun startAsForeground() {
        val chId = "axeron_float"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(chId, "پنجره شناور", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
            val n = Notification.Builder(this, chId)
                .setContentTitle("پنجره 120 فعال است")
                .setContentText("روی لابی دکمه را بزن")
                .setSmallIcon(android.R.drawable.btn_star)
                .build()
            startForeground(1, n)
        } else {
            startForeground(1, Notification())
        }
    }

    private fun showWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val module = GameGraphicsModule(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xDD1E1E1E.toInt())
            setPadding(24, 24, 24, 24)
        }

        val btnEnable = Button(this).apply {
            text = "فعال کردن 120 کامل"
            setOnClickListener {
                val r = module.enableUltraPerformance(COD_PACKAGE)
                text = if (r is InjectionResult.Success) "120 فعال شد" else "خطا!"
                Toast.makeText(this@FloatingService, if (r is InjectionResult.Success) r.data else (r as InjectionResult.Failure).reason, Toast.LENGTH_SHORT).show()
            }
        }

        val btnRestart = Button(this).apply {
            text = "ریستارت"
            setOnClickListener {
                module.resetToDefault(COD_PACKAGE)
                module.restartGame(COD_PACKAGE)
            }
        }

        val btnClose = Button(this).apply {
            text = "X بستن"
            setOnClickListener { stopSelf() }
        }

        layout.addView(btnEnable)
        layout.addView(btnRestart)
        layout.addView(btnClose)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        var sx = 0
        var sy = 0
        var px = 0
        var py = 0
        layout.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX.toInt(); sy = e.rawY.toInt(); px = params.x; py = params.y; false }
                MotionEvent.ACTION_MOVE -> { params.x = px + (e.rawX.toInt() - sx); params.y = py + (e.rawY.toInt() - sy); windowManager.updateViewLayout(layout, params); true }
                else -> false
            }
        }

        floatingView = layout
        try { windowManager.addView(layout, params) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { floatingView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        floatingView = null
    }
}
