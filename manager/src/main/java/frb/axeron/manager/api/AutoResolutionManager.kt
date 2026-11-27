package frb.axeron.manager.api

import android.content.Context

class AutoResolutionManager(
  private val context: Context,
  private val injector: AxBladeInjector
) {
  private val pkg = "com.activision.callofduty.shooter"

  suspend fun applyCustomBoost(dpi: Int): InjectionResult {
    val safe = dpi.coerceIn(200, 800)
    val r1 = injector.exec("wm density $safe")
    if (r1 is InjectionResult.Failure) return r1
    val r2 = injector.exec("cmd game set --perf true $pkg")
    return if (r2 is InjectionResult.Success) InjectionResult.Success("DPI $safe applied")
    else r2
  }

  suspend fun applyProBoost(mode: String): InjectionResult {
    return when (mode) {
      "SMOOTH" -> {
        injector.exec("settings put global game_driver_all_apps 0")
        injector.exec("cmd game set --perf true $pkg")
      }
      "HIGH_FPS" -> {
        injector.exec("settings put global game_driver_all_apps 1")
        injector.exec("cmd game set --perf true $pkg")
      }
      else -> {
        injector.exec("cmd power set-fixed-performance-mode-enabled true")
        injector.exec("cmd game set --perf true $pkg")
        injector.exec("dumpsys gfxinfo $pkg reset")
      }
    }
  }

  suspend fun reset(): InjectionResult {
    injector.exec("wm density reset")
    injector.exec("cmd power set-fixed-performance-mode-enabled false")
    return InjectionResult.Success("reset done")
  }

  suspend fun currentDpi(): String {
    val r = injector.exec("wm density")
    return if (r is InjectionResult.Success) {
      Regex("(\\d+)").findAll(r.data).lastOrNull()?.value ?: "..."
    } else "..."
  }
}
