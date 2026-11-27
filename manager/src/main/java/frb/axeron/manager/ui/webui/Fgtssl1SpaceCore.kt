package frb.axeron.manager.ui.webui.fgtssl1

import android.app.Activity
import android.content.Context
import java.util.concurrent.ConcurrentLinkedQueue

object Fgtssl1SpaceCore {
 const val TARGET = 240
 fun lock(a: Activity) { runCatching { val w = a.window; val at = w.attributes; at.preferredRefreshRate = 240f; w.attributes = at } }
 fun fpsLockJs() = "(function(){try{window.__FPS_TARGET=240;window.__HZ_LOCK=240;window.__4K=1;window.__SPACE=1;window.__FGTSSL1=1;var last=performance.now(),frames=0;function loop(t){frames++;if(t-last>=1000){window.__FPS=frames;frames=0;last=t;}requestAnimationFrame(loop);}requestAnimationFrame(loop);}catch(e){}})();"
 private val q = ConcurrentLinkedQueue<String>()
 fun i(m: String) { if (q.size > Fgtssl1Config.AUDIT_CAP) q.poll(); q.add(m.take(200)) }
 fun dump() = q.joinToString("\n")
}

object Fgtssl1Boot {
 fun prewarm(c: Context) { try { Thread { try { Fgtssl1VirtualCpu.run {}; Fgtssl1VirtualRamFarm.init(c); Fgtssl1SwapFarm.init(c) } catch (_: Exception) {} }.apply { priority = Thread.MIN_PRIORITY; start() } } catch (_: Exception) {} }
}
