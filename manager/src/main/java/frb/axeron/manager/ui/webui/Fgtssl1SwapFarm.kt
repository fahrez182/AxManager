package frb.axeron.manager.ui.webui.fgtssl1

import android.content.Context
import java.io.File
import java.io.RandomAccessFile

object Fgtssl1SwapFarm {
 private lateinit var dir: File
 fun init(c: Context) { dir = File(c.cacheDir, "fgt-swap").apply { mkdirs() }; for (i in 0 until 26) runCatching { val f = File(dir, "swap$i.bin"); if (!f.exists()) RandomAccessFile(f, "rw").apply { setLength(8L * 1024 * 1024); close() } } }
 fun page(src: File): ByteArray { val hit = Fgtssl1VirtualRamFarm.get("swap:" + src.absolutePath); if (hit != null) return hit; val b = src.inputStream().use { it.readNBytes(2 * 1024 * 1024) }; Fgtssl1VirtualRamFarm.put("swap:" + src.absolutePath, b.take(98304).toByteArray()); return b }
 fun stat() = "swaps=26 dir=" + runCatching { dir.absolutePath }.getOrDefault("?")
}
