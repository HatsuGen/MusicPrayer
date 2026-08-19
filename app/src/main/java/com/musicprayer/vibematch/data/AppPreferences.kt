package com.musicprayer.vibematch.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppPreferencesState(
    val themeName: String = "EVA-01",
    val showTopMixes: Boolean = true,
    val folderUri: String? = null,
    val folderName: String? = null,
    val equalizerEnabled: Boolean = false,
    val preamp: Float = 0f,
    val eqBands: List<Float> = List(10) { 0f },
)

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state = _state.asStateFlow()

    fun selectTheme(name: String) = update { copy(themeName = name) }
    fun setShowTopMixes(show: Boolean) = update { copy(showTopMixes = show) }
    fun setFolder(uri: String?, name: String?) = update { copy(folderUri = uri, folderName = name) }
    fun setEqualizerEnabled(enabled: Boolean) = update { copy(equalizerEnabled = enabled) }
    fun setPreamp(value: Float) = update { copy(preamp = value) }
    fun setEqBand(index: Int, value: Float) = update { copy(eqBands = eqBands.toMutableList().also { it[index] = value }) }
    fun resetEqualizer() = update { copy(preamp = 0f, eqBands = List(10) { 0f }) }

    private fun update(block: AppPreferencesState.() -> AppPreferencesState) {
        _state.value = _state.value.block()
        val value = _state.value
        prefs.edit().putString("theme", value.themeName).putBoolean("show_top_mixes", value.showTopMixes)
            .putString("folder_uri", value.folderUri).putString("folder_name", value.folderName)
            .putBoolean("eq_enabled", value.equalizerEnabled).putFloat("preamp", value.preamp)
            .putString("eq_bands", value.eqBands.joinToString(",")).apply()
    }

    private fun read() = AppPreferencesState(
        themeName = prefs.getString("theme", "EVA-01") ?: "EVA-01",
        showTopMixes = prefs.getBoolean("show_top_mixes", true),
        folderUri = prefs.getString("folder_uri", null), folderName = prefs.getString("folder_name", null),
        equalizerEnabled = prefs.getBoolean("eq_enabled", false), preamp = prefs.getFloat("preamp", 0f),
        eqBands = prefs.getString("eq_bands", null)?.split(',')?.mapNotNull(String::toFloatOrNull)?.takeIf { it.size == 10 } ?: List(10) { 0f },
    )
}
