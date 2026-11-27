package frb.axeron.manager.util

object GfxTurbo {
    init {
        try { System.loadLibrary("gfx_turbo") } catch(e: Exception) { e.printStackTrace() }
    }
    @JvmStatic external fun info(): String
    @JvmStatic external fun targetFps(want: Int): Int
    @JvmStatic external fun applyAmoled(px: IntArray, w: Int, h: Int)
}
