package frb.axeron.manager.core

import org.json.JSONArray
import org.json.JSONObject

/** Read-only snapshot of sensors exposed by Android SensorManager. */
data class AxEldenSensorSnapshot(
    val capturedAtMs: Long,
    val availableSensors: List<SensorInfo>,
    val readings: List<SensorReading>,
    val running: Boolean,
    val errors: List<String> = emptyList()
) {
    data class SensorInfo(
        val type: Int,
        val typeName: String,
        val name: String,
        val vendor: String,
        val resolution: Float,
        val maximumRange: Float,
        val minDelayUs: Int,
        val wakeUp: Boolean
    )

    data class SensorReading(
        val type: Int,
        val typeName: String,
        val sensorName: String,
        val values: List<Float>,
        val accuracy: Int,
        val sensorTimestampNs: Long,
        val receivedAtMs: Long
    )

    fun toJson(): String = JSONObject().apply {
        put("captured_at_ms", capturedAtMs)
        put("running", running)
        put("available_sensors", JSONArray().apply {
            availableSensors.forEach { sensor ->
                put(JSONObject().apply {
                    put("type", sensor.type)
                    put("type_name", sensor.typeName)
                    put("name", sensor.name)
                    put("vendor", sensor.vendor)
                    put("resolution", sensor.resolution.toDouble())
                    put("maximum_range", sensor.maximumRange.toDouble())
                    put("min_delay_us", sensor.minDelayUs)
                    put("wake_up", sensor.wakeUp)
                })
            }
        })
        put("readings", JSONArray().apply {
            readings.forEach { reading ->
                put(JSONObject().apply {
                    put("type", reading.type)
                    put("type_name", reading.typeName)
                    put("sensor_name", reading.sensorName)
                    put("values", JSONArray(reading.values))
                    put("accuracy", reading.accuracy)
                    put("sensor_timestamp_ns", reading.sensorTimestampNs)
                    put("received_at_ms", reading.receivedAtMs)
                })
            }
        })
        put("errors", JSONArray(errors))
    }.toString()
}
