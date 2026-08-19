package com.musicprayer.vibematch.data

import android.content.Context
import com.musicprayer.vibematch.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences("playlists", Context.MODE_PRIVATE)
    private val _playlists = MutableStateFlow(read())
    val playlists = _playlists.asStateFlow()

    fun create(name: String) {
        if (name.isBlank()) return
        save(_playlists.value + Playlist(System.currentTimeMillis(), name.trim()))
    }
    fun createWithTrack(name: String, trackId: Long) {
        if (name.isBlank()) return
        save(_playlists.value + Playlist(System.currentTimeMillis(), name.trim(), listOf(trackId)))
    }
    fun delete(id: Long) = save(_playlists.value.filterNot { it.id == id })
    fun addTrack(playlistId: Long, trackId: Long) = save(_playlists.value.map { playlist ->
        if (playlist.id == playlistId && trackId !in playlist.trackIds) playlist.copy(trackIds = playlist.trackIds + trackId) else playlist
    })
    fun removeTrack(playlistId: Long, trackId: Long) = save(_playlists.value.map { if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it })

    private fun save(value: List<Playlist>) {
        _playlists.value = value
        val array = JSONArray(); value.forEach { p -> val ids = JSONArray(); p.trackIds.forEach(ids::put); array.put(JSONObject().put("id", p.id).put("name", p.name).put("tracks", ids)) }
        prefs.edit().putString("items", array.toString()).apply()
    }
    private fun read(): List<Playlist> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]")); buildList { repeat(array.length()) { index -> val o = array.getJSONObject(index); val ids = o.getJSONArray("tracks"); add(Playlist(o.getLong("id"), o.getString("name"), buildList { repeat(ids.length()) { add(ids.getLong(it)) } }.distinct())) } }
    }.getOrDefault(emptyList())
}
