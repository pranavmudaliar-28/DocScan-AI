package com.example.docscanai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ScanRecord(
    val id: String,
    val name: String,
    val imageUri: String,
    val timestamp: Long,
)

object ScanHistoryRepository {
    private const val PREFS_NAME = "docscan_prefs"
    private const val KEY_HISTORY = "scan_history_v2"
    private const val SEP_FIELD = "|||"
    private const val SEP_ENTRY = "~~~"
    private const val MAX_SCANS = 30

    private val _scans = MutableStateFlow<List<ScanRecord>>(emptyList())
    val scans: StateFlow<List<ScanRecord>> = _scans

    fun init(context: Context) {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        _scans.value = deserialize(raw)
    }

    fun addScan(context: Context, record: ScanRecord) {
        val updated = buildList {
            add(record)
            addAll(_scans.value.filter { it.id != record.id })
        }.take(MAX_SCANS)
        _scans.value = updated
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_HISTORY, serialize(updated)).apply()
    }

    fun findById(id: String): ScanRecord? = _scans.value.firstOrNull { it.id == id }

    private fun serialize(scans: List<ScanRecord>) =
        scans.joinToString(SEP_ENTRY) { "${it.id}${SEP_FIELD}${it.name}${SEP_FIELD}${it.imageUri}${SEP_FIELD}${it.timestamp}" }

    private fun deserialize(raw: String): List<ScanRecord> {
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP_ENTRY).mapNotNull { entry ->
            val p = entry.split(SEP_FIELD)
            if (p.size >= 4) ScanRecord(p[0], p[1], p[2], p[3].toLongOrNull() ?: 0L) else null
        }
    }
}
