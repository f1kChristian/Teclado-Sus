package com.tecladop2pv2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Small, explicit export/import bridge for the legacy SharedPreferences history.
 * Only completed message records are shared; raw key-event records are never exported.
 */
internal object HistorySyncStore {
    private const val PREFERENCES = "teclado_p2p_historico"
    private const val RECORDS_KEY = "registros"
    private const val MAX_RECORD_TEXT_CHARS = 3_500
    private const val MAX_RECORDS_PER_BATCH = 3
    private const val MAX_RECORDS_PER_SEND = 50
    private const val MAX_STORED_RECORDS = 500

    private val excludedPackageHints = listOf(
        "password", "credential", "authenticator", "bank", "wallet",
        "bitwarden", "keepass", "1password", "dashlane"
    )

    fun prepareBatches(context: Context): List<String> {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val records = readRecords(preferences.getString(RECORDS_KEY, "[]"))
        var changed = false
        val eligible = mutableListOf<JSONObject>()

        for (index in 0 until records.length()) {
            val record = records.optJSONObject(index) ?: continue
            if (record.optString("tipo", "mensagem") != "mensagem") continue
            if (record.optString("origem", "local") == "dispositivo pareado") continue

            val text = record.optString("texto").trim()
            val app = record.optString("aplicativo", "Aplicativo desconhecido")
            val normalizedApp = app.lowercase()
            if (text.isBlank() || text.length > MAX_RECORD_TEXT_CHARS) continue
            if (excludedPackageHints.any(normalizedApp::contains)) continue

            var id = record.optString("syncId")
            if (runCatching { UUID.fromString(id) }.isFailure) {
                id = UUID.randomUUID().toString()
                record.put("syncId", id)
                changed = true
            }

            // Build a strict allow-list instead of sending arbitrary fields from local storage.
            eligible += JSONObject()
                .put("syncId", id)
                .put("texto", text)
                .put("tipo", "mensagem")
                .put("aplicativo", app.take(256))
                .put("data", record.optString("data").take(64))
        }

        if (changed) preferences.edit().putString(RECORDS_KEY, records.toString()).apply()

        val selected = eligible.takeLast(MAX_RECORDS_PER_SEND)
        return selected.chunked(MAX_RECORDS_PER_BATCH).map { chunk ->
            JSONArray().apply { chunk.forEach { record -> put(record) } }.toString()
        }
    }

    fun mergeIncoming(context: Context, recordsJson: String): Int {
        val incoming = runCatching { JSONArray(recordsJson) }.getOrElse { return 0 }
        if (incoming.length() > MAX_RECORDS_PER_BATCH) return 0

        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val stored = readRecords(preferences.getString(RECORDS_KEY, "[]"))
        val knownIds = mutableSetOf<String>()
        for (index in 0 until stored.length()) {
            stored.optJSONObject(index)?.optString("syncId")
                ?.takeIf { runCatching { UUID.fromString(it) }.isSuccess }
                ?.let(knownIds::add)
        }

        var added = 0
        for (index in 0 until incoming.length()) {
            val item = incoming.optJSONObject(index) ?: continue
            val id = item.optString("syncId")
            val text = item.optString("texto").trim()
            val app = item.optString("aplicativo", "Outro dispositivo")
            if (runCatching { UUID.fromString(id) }.isFailure) continue
            if (id in knownIds || item.optString("tipo") != "mensagem") continue
            if (text.isBlank() || text.length > MAX_RECORD_TEXT_CHARS) continue
            if (app.length > 256) continue
            if (excludedPackageHints.any(app.lowercase()::contains)) continue

            stored.put(
                JSONObject()
                    .put("syncId", id)
                    .put("texto", text)
                    .put("tipo", "mensagem")
                    .put("aplicativo", app)
                    .put("data", item.optString("data").take(64))
                    .put("origem", "dispositivo pareado")
            )
            knownIds += id
            added++
        }

        while (stored.length() > MAX_STORED_RECORDS) {
            val trimmed = JSONArray()
            for (index in 1 until stored.length()) trimmed.put(stored.getJSONObject(index))
            stored.replaceWith(trimmed)
        }

        if (added > 0) preferences.edit().putString(RECORDS_KEY, stored.toString()).apply()
        return added
    }

    private fun readRecords(raw: String?): JSONArray =
        runCatching { JSONArray(raw ?: "[]") }.getOrElse { JSONArray() }

    private fun JSONArray.replaceWith(other: JSONArray) {
        while (length() > 0) remove(0)
        for (index in 0 until other.length()) put(other.getJSONObject(index))
    }
}
