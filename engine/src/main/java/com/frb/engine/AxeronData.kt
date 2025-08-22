package com.frb.engine

import android.os.Environment
import android.util.Log
import java.io.File

//object AxeronData {
//
//    private lateinit var application: Application;
//    private var uri: Uri? = null
//    private var json: JSONObject = JSONObject()
//
//    fun init(app: Application) {
//        application = app
//        uri = getOrCreateFile(app)
//        uri?.let { load(app, it) }
//    }
//
//    private fun getOrCreateFile(context: Context): Uri? {
//        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//        } else {
//            MediaStore.Files.getContentUri("external")
//        }
//
//        // Cek dulu apakah file sudah ada
//        val cursor = context.contentResolver.query(
//            collection,
//            arrayOf(MediaStore.MediaColumns._ID),
//            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
//            arrayOf("Documents/AxData/", "data.json"),
//            null
//        )
//
//        cursor?.use {
//            val id
//            val file = File(
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
//                "AxData/data.json"
//            )
//            if (file.exists()) {
//                MediaScannerConnection.scanFile(
//                    context,
//                    arrayOf(file.absolutePath),
//                    arrayOf("application/json")
//                ) { _, uri ->
//                     = uri.lastPathSegment?.toLongOrNull()
//                    Log.d("AxData", "ID file = $id")
//                }
//                return Uri.withAppendedPath(collection, id.toString()) // tunggu scan selesai, panggil lagi fungsi ini
//            }
////            if (it.moveToFirst()) {
////                val id = it.getLong(0)
////                return Uri.withAppendedPath(collection, id.toString())
////            }
//        }
//
//        // Kalau tidak ada, bikin baru
//        val values = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, "data.json")
//            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
//            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/AxData/")
//        }
//        return context.contentResolver.insert(collection, values)
//    }
//
//    private fun load(context: Context, fileUri: Uri) {
//        try {
//            context.contentResolver.openInputStream(fileUri)?.use { input ->
//                val text = input.bufferedReader().readText()
//                if (text.isNotEmpty()) json = JSONObject(text)
//            }
//        } catch (_: Exception) {
//        }
//    }
//
//    private fun save(context: Context) {
//        uri?.let { fileUri ->
//            context.contentResolver.openOutputStream(fileUri, "wt")?.use { output ->
//                output.write(json.toString(2).toByteArray())
//            }
//        }
//    }
//
//    fun put(key: String, value: Any) {
//        json.put(key, value)
//        save(application)
//    }
//
//    fun get(key: String, default: Any): Any? {
//        if (!json.has(key)) return default
//        return json.opt(key)
//    }
//
//    fun contains(key: String): Boolean = json.has(key)
//
//    fun remove(key: String) {
//        json.remove(key)
//        save(application)
//    }
//
//    fun clear() {
//        json = JSONObject()
//        save(application)
//    }
//}

object AxeronData {
    private const val FOLDER_NAME = "AxData"
    private const val FILE_NAME = "keystore.txt"

    private var file: File? = null

    fun init() {
        ensureFile()
    }

    // ---------- Public API ----------
    fun put(content: String) = save(content)

    fun get(): String? = load()

    // ---------- Internal ----------
    private fun ensureFile() {
        // folder di Documents/AxData
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            FOLDER_NAME
        )

        dir.listFiles()?.forEach {
            Log.d("AxeronData", "ensureFile: ${it.absolutePath}")
        }
//        Log.d("AxeronData", "ensureFile: ${dir}")
//        if (!dir.exists()) dir.mkdirs()
//
//        // file data.json
//        file = File(dir, FILE_NAME)
//        if (!file!!.exists()) {
//            file!!.createNewFile()
//            // inisialisasi awal JSON kosong
//            file!!.writeText("")
//        }
    }

    fun load(): String? {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$FOLDER_NAME/$FILE_NAME")
        if (file.exists()) {
            return file.readText()
        }
        return "empty"
    }

    private fun save(content: String) {
        try {
            file?.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
