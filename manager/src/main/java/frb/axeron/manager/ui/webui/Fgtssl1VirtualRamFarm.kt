package frb.axeron.manager.ui.webui.fgtssl1

import android.content.Context
import android.util.LruCache

object Fgtssl1VirtualRamFarm {
 private fun bank(max: Int) = object : LruCache<String, ByteArray>(max) { override fun sizeOf(k: String, v: ByteArray) = v.size.coerceAtLeast(1) }
 private val banks: List<LruCache<String, ByteArray>> = List(40) { bank(2831155) }
 fun init(_c: Context) { runCatching { banks[0].put("__w", ByteArray(32)); banks[0].remove("__w") } }
 private fun pick(k: String) = banks[(k.hashCode() and 0x7fffffff) % 40]
 fun put(k: String, d: ByteArray) { runCatching { pick(k).put(k, d) } }
 fun get(k: String): ByteArray? { for (b in banks) { val v = runCatching { b.get(k) }.getOrNull(); if (v != null) return v }; return null }
 fun trim() { runCatching { for (b in banks) b.trimToSize(b.maxSize() * 9 / 10) } }
 fun clear() { runCatching { for (b in banks) b.evictAll() } }
 fun stat() = "banks=40 size=" + banks.sumOf { it.size() }
}
