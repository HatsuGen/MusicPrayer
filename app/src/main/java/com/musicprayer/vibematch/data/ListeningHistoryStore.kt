package com.musicprayer.vibematch.data

import android.content.Context
import com.musicprayer.vibematch.model.ListeningRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ListeningHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("listening_history", Context.MODE_PRIVATE)
    private val _records = MutableStateFlow(read())
    val records = _records.asStateFlow()

    fun recordPlay(trackId: Long, durationMs: Long) = mutate(trackId) {
        copy(lastPlayedAt = System.currentTimeMillis(), durationMs = durationMs, playCount = playCount + 1)
    }
    fun saveProgress(trackId: Long, positionMs: Long, durationMs: Long) = mutate(trackId) {
        copy(lastPositionMs = positionMs, durationMs = durationMs.takeIf { it > 0 } ?: this.durationMs)
    }
    fun recordLoop(trackId: Long) = mutate(trackId) { copy(loopCount = loopCount + 1, lastPlayedAt = System.currentTimeMillis()) }

    private fun mutate(id: Long, block: ListeningRecord.() -> ListeningRecord) {
        val next = _records.value.toMutableMap()
        next[id] = (next[id] ?: ListeningRecord(id)).block()
        _records.value = next
        persist(next.values)
    }

    private fun persist(records: Collection<ListeningRecord>) {
        val array = JSONArray()
        records.forEach { r -> array.put(JSONObject().put("id", r.trackId).put("played", r.lastPlayedAt).put("position", r.lastPositionMs).put("duration", r.durationMs).put("plays", r.playCount).put("loops", r.loopCount)) }
        prefs.edit().putString("records", array.toString()).apply()
    }

    private fun read(): Map<Long, ListeningRecord> = runCatching {
        val array = JSONArray(prefs.getString("records", "[]")); buildMap {
            repeat(array.length()) { index -> val o = array.getJSONObject(index); val id = o.getLong("id"); put(id, ListeningRecord(id, o.optLong("played"), o.optLong("position"), o.optLong("duration"), o.optInt("plays"), o.optInt("loops"))) }
        }
    }.getOrDefault(emptyMap())
}
