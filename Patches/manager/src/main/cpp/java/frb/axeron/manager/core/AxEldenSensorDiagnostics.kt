package frb.axeron.manager.core

import android.content.Context
import java.io.Closeable

/** Small facade for UI/lifecycle code to start, inspect, and stop device sensor telemetry. */
class AxEldenSensorDiagnostics(context: Context) : Closeable {
    private val monitor = AxEldenSensorMonitor(context)

    fun start(): AxEldenSensorSnapshot = monitor.start()
    fun current(): AxEldenSensorSnapshot = monitor.snapshot()
    fun stop(): AxEldenSensorSnapshot = monitor.stop()
    fun available(): List<AxEldenSensorSnapshot.SensorInfo> = monitor.availableSensors()

    override fun close() = monitor.close()
}
