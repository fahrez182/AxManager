package frb.axeron.manager.ui.webui.fgtssl1

import android.util.LruCache

object Fgtssl1NanoCache {
 private val m = object : LruCache<String, ByteArray>(18874368) { override fun sizeOf(k: String, v: ByteArray) = v.size.coerceAtLeast(1) }
 fun get(k: String) = runCatching { m.get(k) }.getOrNull()
 fun put(k: String, v: ByteArray) { runCatching { if (v.size < 524288) m.put(k, v) } }
 fun trim() { runCatching { m.trimToSize(m.maxSize() * 9 / 10) } }
}
