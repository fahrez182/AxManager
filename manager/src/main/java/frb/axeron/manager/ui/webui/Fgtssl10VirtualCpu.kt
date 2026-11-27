package frb.axeron.manager.ui.webui.fgtssl10

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object Fgtssl10VirtualCpu {
 private val id = AtomicInteger(0)
 val pool = ThreadPoolExecutor(16, 32, 30L, TimeUnit.SECONDS, LinkedBlockingQueue(8192)) { r -> Thread(r, "fgt10-cpu-" + id.incrementAndGet()).apply { isDaemon = true } }.apply { allowCoreThreadTimeOut(true) }
 fun run(t: () -> Unit) { runCatching { pool.execute(t) } }
 fun infoJs() = "window.__VCPU=540;window.__VCPU_PHYS=32;window.__VCPU_MAX=108;window.__VSOC='AxSilicon-FGT10-SPACE-2000';window.__QUEUE=46080;"
}
