package frb.axeron.manager.ui.webui.fgtssl10

import android.content.Context
import java.io.File

object Fgtssl10SwapFarm {
 private lateinit var dir: File
 fun init(c: Context) { dir = File(c.cacheDir, "fgt10-swap").apply { mkdirs() }; for (i in 0 until 263) runCatching { val f = File(dir, "swap$i.bin"); if (!f.exists()) f.writeBytes(ByteArray(0)) } }
 fun page(src: File): ByteArray { val hit = Fgtssl10VirtualRamFarm.get("swap:" + src.absolutePath); if (hit != null) return hit; val b = src.inputStream().use { it.readNBytes(2 * 1024 * 1024) }; Fgtssl10VirtualRamFarm.put("swap:" + src.absolutePath, b.take(98304).toByteArray()); return b }
 fun stat() = "swaps=263 dir=" + runCatching { dir.absolutePath }.getOrDefault("?")
}
