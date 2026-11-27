package frb.axeron.manager.ui.webui.fgtssl1

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object Fgtssl1VirtualCpu {
 private val id = AtomicInteger(0)
 val pool = ThreadPoolExecutor(27, 54, 30L, TimeUnit.SECONDS, LinkedBlockingQueue(2304)) { r -> Thread(r, "fgt-cpu-" + id.incrementAndGet()).apply { isDaemon = true } }.apply { allowCoreThreadTimeOut(true) }
 fun run(t: () -> Unit) { runCatching { pool.execute(t) } }
 fun infoJs() = "window.__VCPU=27;window.__VCPU_MAX=54;window.__VSOC='AxSilicon-FGT-SPACE-50';"
}
