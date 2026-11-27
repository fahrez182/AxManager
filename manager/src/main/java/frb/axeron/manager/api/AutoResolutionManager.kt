package frb.axeron.manager.api
import android.content.Context
class AutoResolutionManager(context: Context, private val injector: AxBladeInjector) {
  private val pkg = "com.activision.callofduty.shooter"
  suspend fun applyCustomBoost(dpi: Int): InjectionResult {
    val safe = dpi.coerceIn(200, 800)
    val r1 = injector.exec("wm density $safe")
    if (r1 is InjectionResult.Failure) return r1
    return injector.exec("cmd game set --perf true $pkg")
  }
  suspend fun applyProBoost(mode: String): InjectionResult {
    if (mode == "ULTRA_FPS") injector.exec("cmd power set-fixed-performance-mode-enabled true")
    return injector.exec("cmd game set --perf true $pkg")
  }
  suspend fun reset(): InjectionResult {
    injector.exec("wm density reset")
    injector.exec("cmd power set-fixed-performance-mode-enabled false")
    return InjectionResult.Success("reset done")
  }
  suspend fun currentDpi(): String {
    val r = injector.exec("wm density")
    return if (r is InjectionResult.Success) Regex("\\d+").findAll(r.data).lastOrNull()?.value ?: "..." else "..."
  }
}
