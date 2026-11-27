package com.example.data.repository

import com.example.data.dao.ScanItemDao
import com.example.data.model.ScanItem
import com.example.security.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanRepository(
    private val dao: ScanItemDao,
    private val cryptoEngine: CryptoEngine? = null
) {

    val allScans: Flow<List<ScanItem>> = dao.getAll()
        .map { list -> list.map(::decryptItem) }
        .flowOn(Dispatchers.Default)

    val bookmarkedScans: Flow<List<ScanItem>> = dao.getBookmarked()
        .map { list -> list.map(::decryptItem) }
        .flowOn(Dispatchers.Default)

    fun getScanById(id: Long): Flow<ScanItem?> = dao.getById(id)
        .map { item -> item?.let(::decryptItem) }
        .flowOn(Dispatchers.Default)

    fun searchScans(query: String): Flow<List<ScanItem>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return allScans
        return allScans.map { items ->
            items.filter { item ->
                item.rawContent.contains(trimmed, ignoreCase = true) ||
                    item.title.contains(trimmed, ignoreCase = true) ||
                    item.format.contains(trimmed, ignoreCase = true) ||
                    item.parsedType.contains(trimmed, ignoreCase = true)
            }
        }
    }

    suspend fun recordScan(
        rawContent: String,
        format: String,
        parsedType: String,
        securityRating: String,
        title: String = "",
        metadataJson: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val storedRaw = cryptoEngine?.encryptString(rawContent) ?: rawContent
        val storedTitle = cryptoEngine?.encryptString(title.ifBlank { rawContent.take(40) })
            ?: title.ifBlank { rawContent.take(40) }
        val item = ScanItem(
            rawContent = storedRaw,
            format = format,
            parsedType = parsedType,
            securityRating = securityRating,
            timestamp = timestamp,
            title = storedTitle,
            metadataJson = metadataJson
        )
        dao.insert(item)
    }

    suspend fun toggleBookmark(item: ScanItem) = withContext(Dispatchers.IO) {
        val encrypted = encryptItem(item.copy(isBookmarked = !item.isBookmarked))
        dao.update(encrypted)
    }

    suspend fun delete(item: ScanItem) = withContext(Dispatchers.IO) {
        dao.deleteById(item.id)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    private fun encryptItem(item: ScanItem): ScanItem {
        val engine = cryptoEngine ?: return item
        return item.copy(
            rawContent = if (item.rawContent.startsWith("AESGCM:")) item.rawContent else engine.encryptString(item.rawContent),
            title = if (item.title.startsWith("AESGCM:")) item.title else engine.encryptString(item.title)
        )
    }

    private fun decryptItem(item: ScanItem): ScanItem {
        val engine = cryptoEngine ?: return item
        return item.copy(
            rawContent = engine.decryptString(item.rawContent),
            title = engine.decryptString(item.title)
        )
    }

    fun exportToJson(items: List<ScanItem>): String {
        val root = JSONObject()
        root.put("app", "Anamol Scanner")
        root.put("version", "3.0")
        root.put("exportTime", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("rawContent", item.rawContent)
            obj.put("format", item.format)
            obj.put("parsedType", item.parsedType)
            obj.put("securityRating", item.securityRating)
            obj.put("timestamp", item.timestamp)
            obj.put("isBookmarked", item.isBookmarked)
            obj.put("title", item.title)
            array.put(obj)
        }
        root.put("records", array)
        return root.toString(2)
    }

    fun exportToCsv(items: List<ScanItem>): String {
        val sb = StringBuilder()
        sb.append("ID,Timestamp,Format,Type,Security,Bookmarked,Title,RawContent\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (item in items) {
            val dateStr = dateFormat.format(Date(item.timestamp))
            val escapedRaw = "\"" + item.rawContent.replace("\"", "\"\"") + "\""
            val escapedTitle = "\"" + item.title.replace("\"", "\"\"") + "\""
            sb.append("${item.id},$dateStr,${item.format},${item.parsedType},${item.securityRating},${item.isBookmarked},$escapedTitle,$escapedRaw\n")
        }
        return sb.toString()
    }
}
