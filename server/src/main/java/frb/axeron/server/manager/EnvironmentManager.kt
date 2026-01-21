package frb.axeron.server.manager

import frb.axeron.shared.AxeronConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EnvironmentManager() {

    private val fileName = "axeron_env.json"
    private val file: File
        get() = File(
            PathHelper.getShellPath(AxeronConstant.folder.PARENT),
            fileName
        )
    private val lock = ReentrantLock()
    private var data: HashMap<String, String> = hashMapOf()

    init {
        load()
    }

    /** Memuat file JSON ke HashMap */
    private fun load() {
        if (file.exists()) {
            val jsonText = file.readText()
            if (jsonText.isNotEmpty()) {
                val jsonObject = JSONObject(jsonText)
                data = jsonObject.keys().asSequence()
                    .associateWith { jsonObject.getString(it) }
                    .toMap(HashMap())
            }
        }
    }

    /** Menyimpan HashMap ke environment.json */
    private suspend fun save() = withContext(Dispatchers.IO) {
        lock.withLock {
            val jsonObject = JSONObject(data as Map<*, *>)
            file.writeText(jsonObject.toString(4))
        }
    }

    /** PUT - Tambah atau update satu key */
    suspend fun put(key: String, value: String) {
        lock.withLock {
            data[key] = value
        }
        save()
    }

    /** PUT ALL - Tambahkan atau update banyak key-value */
    suspend fun putAll(map: MutableMap<String, String>) {
        lock.withLock {
            data.putAll(map)
        }
        save()
    }

    suspend fun replaceAll(map: MutableMap<String, String>) {
        lock.withLock {
            data.clear()
            data.putAll(map)
        }
        save()
    }

    /** READ - Ambil value berdasarkan key */
    fun read(key: String): String? = data[key]

    /** DELETE - Hapus key */
    suspend fun delete(key: String) {
        lock.withLock {
            data.remove(key)
        }
        save()
    }

    /** GET ALL - Ambil semua data */
    fun getAll(): Map<String, String> {
        load()
        return data.toMap()
    }


    fun putBlocking(key: String, value: String) = runBlocking {
        put(key, value)
    }

    fun putAllBlocking(map: MutableMap<String, String>) = runBlocking {
        putAll(map)
    }

    fun replaceAllBlocking(map: MutableMap<String, String>) = runBlocking {
        replaceAll(map)
    }
}