package frb.axeron.manager.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class TerminalBufferTest {

    private enum class K { OUT, ERR, INFO }

    // Synchronous worker and publisher: every call is parsed and published before it returns.
    private fun buffer(maxLines: Int = 20_000) = TerminalBuffer<K>(
        maxLines = maxLines,
        worker = { it.run() },
        publisher = { it.run() }
    )

    private fun TerminalBuffer<K>.texts() = toList().map { it.text }

    @Test
    fun searchFindsEveryOccurrenceIgnoringCaseAndAnsi() {
        val b = buffer()
        b.write(K.OUT, "Error one\n")
        b.write(K.ERR, "\u001B[31merror\u001B[0m and ERROR\n")
        b.write(K.OUT, "fine\n")
        val all = b.search("error")
        assertEquals(
            listOf(TerminalBuffer.Match(0, 0), TerminalBuffer.Match(1, 0), TerminalBuffer.Match(1, 10)),
            all
        )
        // The filter hides stderr, and the limit caps the result.
        assertEquals(listOf(TerminalBuffer.Match(0, 0)), b.search("error", filter = { it == K.OUT }))
        assertEquals(2, b.search("error", limit = 2).size)
        assertEquals(emptyList<TerminalBuffer.Match>(), b.search(""))
    }

    @Test
    fun chunkBoundariesDoNotSplitLines() {
        val b = buffer()
        b.write(K.OUT, "hel")
        b.write(K.OUT, "lo\nwor")
        b.write(K.OUT, "ld\n")
        assertEquals(listOf("hello", "world"), b.texts())
    }

    @Test
    fun loneNewlineEndsLineAndBlankLinesAreKept() {
        val b = buffer()
        b.write(K.OUT, "a")
        b.write(K.OUT, "\n")
        b.write(K.OUT, "b\n\n\nc\n")
        assertEquals(listOf("a", "b", "", "", "c"), b.texts())
    }

    @Test
    fun carriageReturnOverwritesAcrossChunks() {
        val b = buffer()
        b.write(K.OUT, "10%\r")
        b.write(K.OUT, "50%\r")
        b.write(K.OUT, "100%\r\ndone\n")
        assertEquals(listOf("100%", "done"), b.texts())
    }

    @Test
    fun crlfIsOneNewline() {
        val b = buffer()
        b.write(K.OUT, "a\r\nb\r\n")
        assertEquals(listOf("a", "b"), b.texts())
    }

    @Test
    fun interleavedStreamsDoNotSplice() {
        val b = buffer()
        b.write(K.OUT, "out-")
        b.write(K.ERR, "err\n")
        b.write(K.OUT, "line\n")
        assertEquals(listOf("out-line", "err"), b.texts())
        assertEquals(listOf(K.OUT, K.ERR), b.toList().map { it.kind })
    }

    @Test
    fun sgrKeptAndSplitEscapeHandled() {
        val b = buffer()
        b.write(K.OUT, "\u001B[3")
        b.write(K.OUT, "1mred\u001B[0m\n")
        assertEquals(listOf("\u001B[31mred\u001B[0m"), b.texts())
    }

    @Test
    fun nonSgrSequencesDropped() {
        val b = buffer()
        b.write(K.OUT, "\u001B[?25lhi\u001B[K\u001B]0;title\u0007\n")
        assertEquals(listOf("hi"), b.texts())
    }

    @Test
    fun clearScreenOnlyWipesSinceScreenStart() {
        val b = buffer()
        b.appendLine(K.INFO, "[command] top")
        b.markScreenStart()
        b.write(K.OUT, "frame1\nrow\n")
        b.write(K.OUT, "\u001B[H\u001B[2Jframe2\n")
        assertEquals(listOf("[command] top", "frame2"), b.texts())
    }

    @Test
    fun appendLineClosesOpenStreamLine() {
        val b = buffer()
        b.write(K.OUT, "Name: ")
        b.appendLine(K.INFO, "[input] bob")
        b.write(K.OUT, "Hello bob\n")
        assertEquals(listOf("Name: ", "[input] bob", "Hello bob"), b.texts())
    }

    /** dumpsys-sized output (~40 MB, 400k lines) cut into 8 KB chunks like a pipe read. */
    private fun floodChunks(): List<String> {
        val all = buildString {
            for (i in 0 until 400_000) {
                append("  mService[").append(i).append("]: ComponentInfo{com.example/.Svc} state=RUNNING pid=1234 uid=10123 \u001B[32mok\u001B[0m\n")
            }
        }
        return all.chunked(8 * 1024)
    }

    @Test
    fun floodIsParsedFastAndCapped() {
        val chunks = floodChunks()
        val b = buffer(maxLines = 20_000)
        val start = System.nanoTime()
        chunks.forEach { b.write(K.OUT, it) }
        val ms = (System.nanoTime() - start) / 1_000_000
        println("flood: ${chunks.size} chunks, ${chunks.sumOf { it.length } / 1_048_576} MB parsed in $ms ms")

        assertEquals(20_000, b.toList().size)
        assertTrue(b.toList().last().text.startsWith("  mService[399999]"))
        assertTrue(b.toList().first().text.startsWith("  mService[380000]"))
        assertTrue("parsing too slow: $ms ms", ms < 5_000)
    }

    @Test
    fun floodOnBackgroundWorkerKeepsOrderAndThrottlesPublishing() {
        val chunks = floodChunks()
        val publishes = AtomicInteger()
        // Stands in for the main-thread postDelayed(32 ms).
        val ui = Executors.newSingleThreadScheduledExecutor()
        val b = TerminalBuffer<K>(
            maxLines = 20_000,
            publisher = { task ->
                ui.schedule({ publishes.incrementAndGet(); task.run() }, 32, TimeUnit.MILLISECONDS)
            }
        )
        // Producer on its own thread, like the process reader.
        val producer = thread { chunks.forEach { b.write(K.OUT, it) } }
        producer.join()

        val deadline = System.currentTimeMillis() + 10_000
        while (b.toList().lastOrNull()?.text?.startsWith("  mService[399999]") != true) {
            assertTrue("worker never caught up", System.currentTimeMillis() < deadline)
            Thread.sleep(10)
        }
        println("background flood: ${chunks.size} chunks -> ${publishes.get()} publishes")

        assertEquals(20_000, b.toList().size)
        // Contiguous and in order: ids/positions survived the ring-buffer trimming.
        val first = b.toList().first().text.substringAfter('[').substringBefore(']').toInt()
        b.toList().forEachIndexed { i, line ->
            assertTrue(line.text.startsWith("  mService[${first + i}]"))
        }
        assertTrue(publishes.get() < chunks.size / 10)
        ui.shutdown()
    }

    @Test
    fun lineNumbersStayAbsoluteAcrossTrimAndClears() {
        val b = buffer(3)
        b.write(K.OUT, "1\n2\n3\n4\n5\n")
        assertEquals(listOf(3L, 4L, 5L), b.toList().map { it.number })

        val c = buffer()
        c.write(K.OUT, "1\n2\n")
        c.markScreenStart()
        c.write(K.OUT, "a\nb\n\u001B[2Jc\n")
        // Screen clear drops a/b; c takes the next number after the kept lines.
        assertEquals(listOf("1", "2", "c"), c.texts())
        assertEquals(listOf(1L, 2L, 3L), c.toList().map { it.number })

        c.clear()
        c.write(K.OUT, "x\n")
        assertEquals(listOf(1L), c.toList().map { it.number })
    }

    // ---- unlimited (spool) mode ----

    private fun spoolBuffer(dir: File, unlimited: Boolean = true, maxLines: Int = 20_000) =
        TerminalBuffer<K>(
            maxLines = maxLines,
            spoolDir = dir,
            unlimited = unlimited,
            worker = { it.run() },
            publisher = { it.run() }
        )

    private fun tempDir(): File =
        File.createTempFile("spool", "").apply { delete(); mkdirs(); deleteOnExit() }

    @Test
    fun spoolKeepsAMillionLinesOnDisk() {
        val dir = tempDir()
        val b = spoolBuffer(dir)
        val chunk = buildString {
            for (i in 0 until 1_000) append("line ").append(i).append(" of a chunk ±ü\n")
        }
        val runtime = Runtime.getRuntime()
        System.gc()
        val before = runtime.totalMemory() - runtime.freeMemory()
        val start = System.nanoTime()
        repeat(1_000) { b.write(K.OUT, chunk) } // 1,000,000 lines
        val ms = (System.nanoTime() - start) / 1_000_000
        System.gc()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory() - before) / 1_048_576
        println("spool: 1M lines in $ms ms, heap +$usedMb MB, file ${dir.listFiles()!!.sumOf { it.length() } / 1_048_576} MB")

        val snap = b.snapshot
        assertEquals(1_000_000, snap.size)
        assertTrue(snap.unlimited)
        assertEquals("line 0 of a chunk ±ü", b.lineAt(0)!!.text)
        assertEquals("line 999 of a chunk ±ü", b.lineAt(999_999)!!.text)
        assertEquals(500_001L, b.lineAt(500_000)!!.number)
        assertTrue("index too big: $usedMb MB", usedMb < 64)

        b.close()
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test
    fun spoolHandlesOpenLinesAndScreenClear() {
        val b = spoolBuffer(tempDir())
        b.write(K.OUT, "keep\n")
        b.markScreenStart()
        b.write(K.OUT, "10%\r")
        b.write(K.ERR, "warn\n")
        b.write(K.OUT, "100%\n")
        assertEquals(listOf("keep", "100%", "warn"), b.texts())

        b.write(K.OUT, "frame1\n\u001B[H\u001B[2Jframe2\n")
        assertEquals(listOf("keep", "frame2"), b.texts())
        assertEquals(listOf(1L, 2L), b.toList().map { it.number })
        b.close()
    }

    @Test
    fun switchingStorageKeepsOutputAndOpenLine() {
        val b = spoolBuffer(tempDir(), unlimited = false, maxLines = 5)
        b.write(K.OUT, "1\n2\n3\n4\n5\n6\n7\nopen")
        b.setUnlimited(true)
        assertTrue(b.snapshot.unlimited)
        b.write(K.OUT, "-line\n8\n9\n")
        // Lines dropped before switching are gone; everything after is kept.
        assertEquals(listOf("4", "5", "6", "7", "open-line", "8", "9"), b.texts())
        assertEquals(4L, b.toList().first().number)

        b.setUnlimited(false)
        // Back to memory: only the last maxLines (5) survive.
        assertEquals(listOf("6", "7", "open-line", "8", "9"), b.texts())
        assertEquals(5, b.snapshot.size)
        b.close()
    }

    @Test
    fun writeToStreamsPlainText() {
        val b = spoolBuffer(tempDir())
        b.write(K.OUT, "\u001B[31mred\u001B[0m\n")
        b.write(K.ERR, "err\n")
        b.write(K.OUT, "out\n")
        val out = ByteArrayOutputStream()
        b.writeTo(out) { it == K.OUT }
        assertEquals("red\nout\n", out.toString("UTF-8"))
        assertEquals("err\nout", b.text(lastLines = 2))
        b.close()
    }

    @Test
    fun trimKeepsOpenLineIndexValid() {
        val b = buffer(3)
        b.write(K.OUT, "1\n2\n3\n4")
        b.write(K.OUT, "5\n")
        assertEquals(listOf("2", "3", "45"), b.texts())
    }
}
