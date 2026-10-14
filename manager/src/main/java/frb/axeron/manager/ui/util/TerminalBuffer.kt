package frb.axeron.manager.ui.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import frb.axeron.api.utils.AnsiFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.util.concurrent.Executor

/**
 * Line model for command output, fed with the raw stream exactly as the process wrote it.
 *
 * Chunk boundaries carry no meaning: a chunk can hold several lines, half a line, a lone `\n`
 * or half an escape sequence. Each [kind] (stdout, stderr, …) keeps its own open line so
 * interleaved streams never splice into each other.
 *
 * - `\n` ends the open line (empty lines are kept).
 * - `\r` returns to column 0; the next printable text replaces the line (progress bars).
 * - `ESC[2J`, `ESC[J`, `ESC[H`, `ESC c` clear the screen back to [markScreenStart].
 * - SGR colour sequences stay in the text for ANSI rendering; other control sequences are dropped.
 *
 * Built for floods (e.g. `dumpsys`): callers only enqueue, parsing runs serially off the main
 * thread, and the UI gets a small [Snapshot] at most once per [publishIntervalMs] and reads
 * the visible lines on demand with [lineAt].
 *
 * Storage: the last [maxLines] in memory by default. [setUnlimited] keeps every line instead,
 * spooled to a file in [spoolDir] with only a ~13 byte index entry per line in memory, so
 * millions of lines stay openable and scrollable.
 */
@Stable
class TerminalBuffer<K : Any>(
    private val maxLines: Int = 20_000,
    private val spoolDir: File? = null,
    unlimited: Boolean = false,
    private val publishIntervalMs: Long = 32,
    private val worker: Executor = Dispatchers.Default.asExecutor(),
    private val publisher: ((Runnable) -> Unit)? = null,
) {

    /**
     * @param number 1-based line number in the output. Stays put when old lines are trimmed
     * (the first kept line may be 380001); restarts after [clear].
     */
    data class Line<K>(val number: Long, val kind: K, val text: String)

    /**
     * What the UI renders: lines at absolute positions [head, tail) (line number = position + 1).
     * [version] bumps on any change, [generation] when existing positions were dropped or
     * reused (screen clear, [clear], storage switch).
     */
    @Immutable
    data class Snapshot(
        val head: Long = 0,
        val tail: Long = 0,
        val version: Long = 0,
        val generation: Int = 0,
        val unlimited: Boolean = false,
    ) {
        val size: Int get() = (tail - head).toInt()
        val isEmpty: Boolean get() = tail == head
    }

    var snapshot by mutableStateOf(Snapshot())
        private set

    val isEmpty: Boolean get() = snapshot.isEmpty

    fun write(kind: K, chunk: String) {
        if (chunk.isNotEmpty()) enqueue(Op.Write(kind, chunk))
    }

    /** Whole line(s) produced by the app itself. Ends any open stream line first. */
    fun appendLine(kind: K, text: String) = enqueue(Op.AppendLine(kind, text))

    /** Ends open stream lines, e.g. when a command finishes, so the next one starts fresh. */
    fun endStreams() = enqueue(Op.EndStreams)

    /** Screen clears only wipe lines added after this point (keeps earlier commands). */
    fun markScreenStart() = enqueue(Op.MarkScreenStart)

    fun clear() = enqueue(Op.Clear)

    /**
     * Keep every line on disk (true) or only the last [maxLines] in memory (false). Applies to
     * the current output too: switching off keeps its last [maxLines]. Needs [spoolDir].
     */
    fun setUnlimited(enabled: Boolean) = enqueue(Op.SetUnlimited(enabled))

    /** Releases the spool file. Later writes are ignored. */
    fun close() = synchronized(stateLock) {
        closed = true
        store.close()
    }

    // ---- reading ----

    /** The line at absolute [position], or null when it is gone. Cheap; safe on the main thread. */
    fun lineAt(position: Long): Line<K>? = synchronized(stateLock) { safeLine(position) }

    /** A failed spool read (closed, I/O error) shows as a missing line, never a crash. */
    private fun safeLine(position: Long): Line<K>? =
        if (closed) null else try {
            store.line(position)
        } catch (e: IOException) {
            null
        }

    /** Kinds of the lines in [from, to), in one lock acquisition. */
    fun forEachKind(from: Long, to: Long, block: (position: Long, kind: K) -> Unit) =
        synchronized(stateLock) {
            var p = maxOf(from, store.head)
            val end = minOf(to, store.tail)
            while (p < end) {
                store.kind(p)?.let { block(p, it) }
                p++
            }
        }

    /** All current lines. Fine for tests and small output; use [writeTo] for big output. */
    fun toList(): List<Line<K>> = synchronized(stateLock) {
        (store.head until store.tail).mapNotNull { safeLine(it) }
    }

    /** Plain text (ANSI stripped) of up to the last [lastLines] lines passing [filter]. */
    fun text(filter: (K) -> Boolean = { true }, lastLines: Int = Int.MAX_VALUE): String =
        buildString {
            val from = synchronized(stateLock) {
                maxOf(store.head, store.tail - lastLines.toLong())
            }
            forEachLine(from, filter) {
                if (isNotEmpty()) append('\n')
                append(AnsiFilter.stripAnsi(it.text))
            }
        }

    /**
     * Streams plain text (ANSI stripped) of every line passing [filter] into [out] without
     * holding it all in memory. [out] is flushed, not closed.
     */
    fun writeTo(out: OutputStream, filter: (K) -> Boolean = { true }) {
        val writer = BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8), 64 * 1024)
        val from = synchronized(stateLock) { store.head }
        forEachLine(from, filter) {
            writer.write(AnsiFilter.stripAnsi(it.text))
            writer.write('\n'.code)
        }
        writer.flush()
    }

    fun sink(kind: K): (String) -> Unit = { write(kind, it) }

    /** Walks lines in small locked batches so parsing and the UI never stall behind it. */
    private inline fun forEachLine(from: Long, filter: (K) -> Boolean, action: (Line<K>) -> Unit) {
        var position = from
        while (true) {
            val batch = synchronized(stateLock) {
                if (position < store.head) position = store.head
                val end = minOf(store.tail, position + 2_000)
                val lines = ArrayList<Line<K>>((end - position).toInt().coerceAtLeast(0))
                while (position < end) safeLine(position++)?.let(lines::add)
                lines
            }
            if (batch.isEmpty()) break
            batch.forEach { if (filter(it.kind)) action(it) }
        }
    }

    // ---- queue: callers only append here ----

    private sealed interface Op {
        data class Write<K>(val kind: K, val chunk: String) : Op
        data class AppendLine<K>(val kind: K, val text: String) : Op
        data class SetUnlimited(val enabled: Boolean) : Op
        data object EndStreams : Op
        data object MarkScreenStart : Op
        data object Clear : Op
    }

    private val queueLock = Any()
    private var queue = ArrayList<Op>()
    private var draining = false

    private fun enqueue(op: Op) {
        val start: Boolean
        synchronized(queueLock) {
            queue.add(op)
            start = !draining
            if (start) draining = true
        }
        if (start) worker.execute(::drain)
    }

    /** Only one drain runs at a time (the [draining] flag), so ops apply in order. */
    private fun drain() {
        while (true) {
            val batch = synchronized(queueLock) {
                if (queue.isEmpty()) {
                    draining = false
                    return
                }
                queue.also { queue = ArrayList() }
            }
            // Lock per op, not per batch: a flood batch can take a while to parse and the
            // main thread must never wait on it for more than one chunk.
            for (op in batch) {
                synchronized(stateLock) {
                    if (closed) return@synchronized
                    try {
                        apply(op)
                    } catch (e: IOException) {
                        // Spool failed (e.g. storage full): keep going in memory from here.
                        Log.w(TAG, "spool failed, falling back to memory", e)
                        store.close()
                        store = RingStore(maxLines, store.tail)
                        streams.values.forEach { it.position = -1 }
                        generation++
                    }
                    version++
                }
                schedulePublish()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun apply(op: Op) {
        when (op) {
            is Op.Write<*> -> applyWrite(op.kind as K, op.chunk)
            is Op.AppendLine<*> -> {
                closeStreams()
                op.text.split('\n').forEach { store.add(op.kind as K, it.trimEnd('\r'), open = false) }
            }

            is Op.SetUnlimited -> switchStore(op.enabled)
            Op.EndStreams -> closeStreams()
            Op.MarkScreenStart -> {
                closeStreams()
                screenStart = store.tail
            }

            Op.Clear -> {
                store.clear()
                streams.clear()
                screenStart = 0
                generation++
            }
        }
    }

    // ---- parsed state: touched under stateLock only ----

    private class StreamState {
        /** Absolute position of the open line, or -1. */
        var position = -1L
        val text = StringBuilder()
        var carriageReturn = false
        val escape = StringBuilder()
    }

    private val stateLock = Any()
    private var store: Store<K> =
        if (unlimited && spoolDir != null) SpoolStore(spoolDir, 0) else RingStore(maxLines, 0)
    private val streams = HashMap<K, StreamState>()
    private var closed = false
    private var screenStart = 0L
    private var version = 0L
    private var generation = 0

    private fun switchStore(unlimited: Boolean) {
        val current = store
        if (unlimited == (current is SpoolStore)) return
        val next: Store<K> = if (unlimited) {
            SpoolStore(spoolDir ?: return, current.head)
        } else {
            RingStore(maxLines, maxOf(current.head, current.tail - maxLines))
        }
        var p = next.head
        while (p < current.tail) {
            current.line(p)?.let { next.add(it.kind, it.text, open = false) }
            p++
        }
        current.close()
        store = next
        generation++
    }

    private fun applyWrite(kind: K, chunk: String) {
        val s = streams.getOrPut(kind) { StreamState() }
        var dirty = false

        for (c in chunk) {
            if (s.escape.isNotEmpty()) {
                s.escape.append(c)
                if (isEscapeComplete(s.escape)) {
                    val seq = s.escape.toString()
                    s.escape.setLength(0)
                    if (handleEscape(s, seq)) dirty = true
                    // A screen clear dropped the open line; nothing left to commit.
                    else if (s.position < 0 && s.text.isEmpty()) dirty = false
                }
                continue
            }
            when (c) {
                ESC -> s.escape.append(c)
                '\n' -> {
                    commit(kind, s, close = true)
                    dirty = false
                }

                '\r' -> s.carriageReturn = true
                '\b' -> if (s.text.isNotEmpty()) {
                    s.text.setLength(s.text.length - 1)
                    dirty = true
                }

                else -> if (c >= ' ' || c == '\t') {
                    applyCarriageReturn(s)
                    s.text.append(c)
                    dirty = true
                }
            }
        }
        if (dirty) commit(kind, s, close = false)
    }

    private fun applyCarriageReturn(s: StreamState) {
        if (s.carriageReturn) {
            s.text.setLength(0)
            s.carriageReturn = false
        }
    }

    private fun commit(kind: K, s: StreamState, close: Boolean) {
        val text = s.text.toString()
        if (s.position >= 0 && s.position in store.head until store.tail) {
            store.update(s.position, text, open = !close)
        } else {
            s.position = store.add(kind, text, open = !close)
        }
        if (close) {
            s.position = -1
            s.text.setLength(0)
            s.carriageReturn = false
        }
    }

    private fun closeStreams() {
        streams.values.forEach {
            if (it.position >= 0) store.finish(it.position)
            it.position = -1
            it.text.setLength(0)
            it.carriageReturn = false
        }
    }

    /** @return whether the open line changed. */
    private fun handleEscape(s: StreamState, seq: String): Boolean {
        if (seq == "${ESC}c") {
            clearScreen()
            return false
        }
        if (seq.length < 3 || seq[1] != '[') return false // non-CSI (OSC, charset, …): drop

        val params = seq.substring(2, seq.length - 1)
        return when (seq.last()) {
            'm' -> {
                applyCarriageReturn(s)
                s.text.append(seq)
                true
            }

            'J' -> {
                clearScreen()
                false
            }

            'H', 'f' -> {
                // Cursor home is how full-screen tools (top, watch) start a new frame.
                if (params.isEmpty() || params == "1;1" || params == "0;0") clearScreen()
                false
            }

            'K' -> {
                // Erase whole line / to start: same as starting the line over.
                if (params == "2" || params == "1") {
                    s.text.setLength(0)
                    true
                } else false
            }

            else -> false
        }
    }

    private fun clearScreen() {
        val keep = screenStart.coerceIn(store.head, store.tail)
        if (keep < store.tail) {
            store.truncate(keep)
            generation++
        }
        closeStreams()
    }

    // ---- publishing to Compose ----

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val publishLock = Any()
    private var publishScheduled = false

    private val publishTask = Runnable {
        val next = synchronized(stateLock) {
            synchronized(publishLock) { publishScheduled = false }
            Snapshot(store.head, store.tail, version, generation, store is SpoolStore)
        }
        snapshot = next
    }

    private fun schedulePublish() {
        synchronized(publishLock) {
            if (publishScheduled) return
            publishScheduled = true
        }
        publisher?.invoke(publishTask) ?: mainHandler.postDelayed(publishTask, publishIntervalMs)
    }

    // ---- storage ----

    /** Lines by absolute position in [head, tail). Called under stateLock only. */
    private interface Store<K : Any> {
        val head: Long
        val tail: Long

        /** Appends a line and returns its position. An open line may still change. */
        fun add(kind: K, text: String, open: Boolean): Long
        fun update(position: Long, text: String, open: Boolean)

        /** The open line at [position] won't change anymore. */
        fun finish(position: Long)

        /** Drops every line at or after [keep]. */
        fun truncate(keep: Long)
        fun line(position: Long): Line<K>?
        fun kind(position: Long): K?
        fun clear()
        fun close()
    }

    /** The last [max] lines in memory. */
    private class RingStore<K : Any>(private val max: Int, base: Long) : Store<K> {
        private val rows = ArrayDeque<Line<K>>()
        override var head = base
            private set
        override val tail get() = head + rows.size

        override fun add(kind: K, text: String, open: Boolean): Long {
            val position = tail
            rows.addLast(Line(position + 1, kind, text))
            while (rows.size > max) {
                rows.removeFirst()
                head++
            }
            return position
        }

        override fun update(position: Long, text: String, open: Boolean) {
            val i = (position - head).toInt()
            rows[i] = rows[i].copy(text = text)
        }

        override fun finish(position: Long) = Unit

        override fun truncate(keep: Long) {
            while (tail > keep && rows.isNotEmpty()) rows.removeLast()
        }

        override fun line(position: Long): Line<K>? =
            if (position in head until tail) rows[(position - head).toInt()] else null

        override fun kind(position: Long): K? = line(position)?.kind

        override fun clear() {
            rows.clear()
            head = 0
        }

        override fun close() = Unit
    }

    /**
     * Every line, text in an append-only spool file. Memory per line: offset + length + kind
     * id in paged arrays. Open lines stay in memory until finished. Recently read lines are
     * cached for scrolling.
     */
    private class SpoolStore<K : Any>(dir: File, base: Long) : Store<K> {
        private val file: File
        private val raf: RandomAccessFile

        init {
            dir.mkdirs()
            // Spools of a crashed session are never closed; sweep the old ones.
            val stale = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            dir.listFiles { f -> f.name.startsWith(PREFIX) && f.lastModified() < stale }
                ?.forEach { it.delete() }
            file = File.createTempFile(PREFIX, ".spool", dir)
            raf = RandomAccessFile(file, "rw")
        }

        override var head = base
            private set
        private var count = 0
        override val tail get() = head + count

        private val offsets = ArrayList<LongArray>()
        private val lengths = ArrayList<IntArray>()
        private val kindIds = ArrayList<ByteArray>()
        private val kindTable = ArrayList<K>()
        private val kindIndex = HashMap<K, Int>()

        private val pending = HashMap<Long, String>()
        private val cache = object : LinkedHashMap<Long, Line<K>>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Line<K>>?) =
                size > CACHE_LINES
        }

        private val buffer = ByteArray(64 * 1024)
        private var buffered = 0
        private var flushed = 0L

        override fun add(kind: K, text: String, open: Boolean): Long {
            val i = count
            if (i ushr PAGE_BITS >= offsets.size) {
                offsets.add(LongArray(PAGE))
                lengths.add(IntArray(PAGE))
                kindIds.add(ByteArray(PAGE))
            }
            val id = kindIndex.getOrPut(kind) { kindTable.add(kind); kindTable.lastIndex }
            kindIds[i ushr PAGE_BITS][i and PAGE_MASK] = id.toByte()
            count++
            val position = head + i
            if (open) pending[position] = text else store(i, text)
            return position
        }

        override fun update(position: Long, text: String, open: Boolean) {
            cache.remove(position)
            if (open) {
                pending[position] = text
            } else {
                pending.remove(position)
                store((position - head).toInt(), text)
            }
        }

        override fun finish(position: Long) {
            pending.remove(position)?.let { store((position - head).toInt(), it) }
        }

        override fun truncate(keep: Long) {
            val n = (keep - head).coerceIn(0, count.toLong()).toInt()
            pending.keys.removeAll { it >= keep }
            cache.keys.removeAll { it >= keep }
            count = n
            // The dropped text stays in the file as garbage; positions are reused.
        }

        override fun line(position: Long): Line<K>? {
            if (position !in head until tail) return null
            val i = (position - head).toInt()
            val kind = kindTable[kindIds[i ushr PAGE_BITS][i and PAGE_MASK].toInt()]
            pending[position]?.let { return Line(position + 1, kind, it) }
            cache[position]?.let { return it }
            return Line(position + 1, kind, read(i)).also { cache[position] = it }
        }

        override fun kind(position: Long): K? {
            if (position !in head until tail) return null
            val i = (position - head).toInt()
            return kindTable[kindIds[i ushr PAGE_BITS][i and PAGE_MASK].toInt()]
        }

        override fun clear() {
            pending.clear()
            cache.clear()
            count = 0
            head = 0
            buffered = 0
            flushed = 0
            raf.setLength(0)
        }

        override fun close() {
            runCatching { raf.close() }
            file.delete()
        }

        private fun store(i: Int, text: String) {
            val bytes = text.encodeToByteArray()
            offsets[i ushr PAGE_BITS][i and PAGE_MASK] = append(bytes)
            lengths[i ushr PAGE_BITS][i and PAGE_MASK] = bytes.size
        }

        private fun append(bytes: ByteArray): Long {
            if (bytes.size > buffer.size - buffered) flush()
            if (bytes.size > buffer.size) {
                val offset = flushed
                raf.seek(offset)
                raf.write(bytes)
                flushed += bytes.size
                return offset
            }
            val offset = flushed + buffered
            System.arraycopy(bytes, 0, buffer, buffered, bytes.size)
            buffered += bytes.size
            return offset
        }

        private fun flush() {
            if (buffered == 0) return
            raf.seek(flushed)
            raf.write(buffer, 0, buffered)
            flushed += buffered
            buffered = 0
        }

        private fun read(i: Int): String {
            val offset = offsets[i ushr PAGE_BITS][i and PAGE_MASK]
            val length = lengths[i ushr PAGE_BITS][i and PAGE_MASK]
            if (length == 0) return ""
            // The newest lines are usually still in the write buffer.
            if (offset >= flushed) {
                return String(buffer, (offset - flushed).toInt(), length, Charsets.UTF_8)
            }
            val bytes = ByteArray(length)
            raf.seek(offset)
            raf.readFully(bytes)
            return String(bytes, Charsets.UTF_8)
        }

        private companion object {
            const val PREFIX = "terminal-"
            const val PAGE_BITS = 14
            const val PAGE = 1 shl PAGE_BITS
            const val PAGE_MASK = PAGE - 1
            const val CACHE_LINES = 1024
        }
    }

    private companion object {
        const val TAG = "TerminalBuffer"
        const val ESC = '\u001B'
        const val MAX_ESCAPE = 64

        fun isEscapeComplete(seq: StringBuilder): Boolean {
            if (seq.length > MAX_ESCAPE) return true
            if (seq.length < 2) return false
            return when (seq[1]) {
                // CSI: parameters, then a final byte in @..~
                '[' -> seq.length > 2 && seq.last() in '@'..'~'
                // OSC: terminated by BEL or ST (ESC \)
                ']' -> seq.last() == '\u0007' || seq.endsWith("$ESC\\")
                // Charset selection etc. take one more byte.
                '(', ')', '#' -> seq.length >= 3
                else -> true
            }
        }
    }
}
