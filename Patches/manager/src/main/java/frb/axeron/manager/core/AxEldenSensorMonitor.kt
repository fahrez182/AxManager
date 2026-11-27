package frb.axeron.manager.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reads sensors explicitly exposed by Android for AxManager itself.
 * No special permission is required for the standard motion/environment sensors.
 * This class does not access another app or game process.
 */
class AxEldenSensorMonitor(
    context: Context,
    private val onSnapshot: ((AxEldenSensorSnapshot) -> Unit)? = null
) : SensorEventListener, Closeable {

    companion object {
        // Conservative sampling: suitable for status/diagnostic displays, not motion control.
        private const val SAMPLE_PERIOD_US = 200_000 // 5 Hz

        private val supportedTypes = listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_AMBIENT_TEMPERATURE
        )

        fun typeName(type: Int): String = when (type) {
            Sensor.TYPE_ACCELEROMETER -> "accelerometer"
            Sensor.TYPE_GYROSCOPE -> "gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "magnetic_field"
            Sensor.TYPE_LIGHT -> "light"
            Sensor.TYPE_PROXIMITY -> "proximity"
            Sensor.TYPE_PRESSURE -> "pressure"
            Sensor.TYPE_ROTATION_VECTOR -> "rotation_vector"
            Sensor.TYPE_GRAVITY -> "gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "linear_acceleration"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "relative_humidity"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "ambient_temperature"
            else -> "sensor_$type"
        }
    }

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lock = Any()
    private val closed = AtomicBoolean(false)
    private val activeSensors = mutableListOf<Sensor>()
    private val latestReadings = linkedMapOf<Int, AxEldenSensorSnapshot.SensorReading>()
    private val errors = mutableListOf<String>()
    @Volatile private var running = false

    /** Sensors physically/virtually exposed by this Android device. */
    fun availableSensors(): List<AxEldenSensorSnapshot.SensorInfo> =
        sensorManager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            AxEldenSensorSnapshot.SensorInfo(
                type = sensor.type,
                typeName = typeName(sensor.type),
                name = sensor.name,
                vendor = sensor.vendor,
                resolution = sensor.resolution,
                maximumRange = sensor.maximumRange,
                minDelayUs = sensor.minDelay,
                wakeUp = sensor.isWakeUpSensor
            )
        }

    /** Starts listening to one default sensor for each supported type available. */
    @Synchronized
    fun start(): AxEldenSensorSnapshot {
        check(!closed.get()) { "AxEldenSensorMonitor is closed" }
        if (!running) {
            synchronized(lock) {
                latestReadings.clear()
                errors.clear()
            }
            supportedTypes.distinct().forEach { type ->
                val sensor = sensorManager.getDefaultSensor(type) ?: return@forEach
                try {
                    if (sensorManager.registerListener(this, sensor, SAMPLE_PERIOD_US)) {
                        activeSensors += sensor
                    } else {
                        synchronized(lock) { errors += "Registration rejected: ${typeName(type)}" }
                    }
                } catch (e: SecurityException) {
                    synchronized(lock) { errors += "Permission denied: ${typeName(type)}" }
                } catch (e: RuntimeException) {
                    synchronized(lock) { errors += "Unavailable: ${typeName(type)} (${e.javaClass.simpleName})" }
                }
            }
            running = activeSensors.isNotEmpty()
        }
        return snapshot()
    }

    @Synchronized
    fun stop(): AxEldenSensorSnapshot {
        if (running || activeSensors.isNotEmpty()) {
            sensorManager.unregisterListener(this)
            activeSensors.clear()
            running = false
        }
        return snapshot()
    }

    fun snapshot(): AxEldenSensorSnapshot {
        val readingsCopy: List<AxEldenSensorSnapshot.SensorReading>
        val errorsCopy: List<String>
        synchronized(lock) {
            readingsCopy = latestReadings.values.toList()
            errorsCopy = errors.toList()
        }
        return AxEldenSensorSnapshot(
            capturedAtMs = System.currentTimeMillis(),
            availableSensors = availableSensors(),
            readings = readingsCopy,
            running = running,
            errors = errorsCopy
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (closed.get()) return
        val reading = AxEldenSensorSnapshot.SensorReading(
            type = event.sensor.type,
            typeName = typeName(event.sensor.type),
            sensorName = event.sensor.name,
            values = event.values.map { it },
            accuracy = event.accuracy,
            sensorTimestampNs = event.timestamp,
            receivedAtMs = System.currentTimeMillis()
        )
        synchronized(lock) { latestReadings[event.sensor.type] = reading }
        // UI/observer failures must not terminate the Android sensor callback thread.
        try {
            onSnapshot?.invoke(snapshot())
        } catch (failure: RuntimeException) {
            synchronized(lock) {
                errors += "Snapshot callback failed: ${failure.javaClass.simpleName}"
                if (errors.size > 32) errors.removeAt(0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Accuracy is included in the next SensorReading; no extra work is needed here.
    }

    @Synchronized
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            sensorManager.unregisterListener(this)
            activeSensors.clear()
            running = false
        }
    }
}
