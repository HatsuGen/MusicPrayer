package com.musicprayer.vibematch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicprayer.vibematch.MainUiState
import com.musicprayer.vibematch.ui.theme.AppThemes

private val frequencies = listOf("31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz", "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz")

@Composable fun SettingsScreen(
    state: MainUiState, chooseFolder: () -> Unit, onTheme: (String) -> Unit, onShowMixes: (Boolean) -> Unit,
    onEqEnabled: (Boolean) -> Unit, onPreamp: (Float) -> Unit, onBand: (Int, Float) -> Unit, onResetEq: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { Text("Settings", Modifier.padding(22.dp, 16.dp, 22.dp, 8.dp), fontSize = 30.sp, fontWeight = FontWeight.Black) }
        item { SettingsHeading("Appearance") }
        item { Column(Modifier.padding(horizontal = 18.dp)) { AppThemes.forEach { theme -> ThemeOption(theme.name, theme.dark, theme.background, theme.primary, theme.secondary, state.preferences.themeName == theme.name) { onTheme(theme.name) } } } }
        item { SettingsHeading("Home") }
        item { SettingsSwitch("Show Your Top Mixes", "Display playlists you create on Home", state.preferences.showTopMixes, onShowMixes) }
        item { SettingsHeading("Music folder") }
        item {
            Row(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable(enabled = !state.isScanning, onClick = chooseFolder).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(state.preferences.folderName ?: "Not selected", fontWeight = FontWeight.Bold); Text(if (state.preferences.folderUri == null) "Tap to choose exactly one folder" else "Library stays cached until you press RESCAN", color = MaterialTheme.colorScheme.onSurface.copy(.58f), fontSize = 12.sp) }; Text(if (state.isScanning) "SCANNING" else "CHANGE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
        item { SettingsHeading("Equalizer") }
        item { SettingsSwitch("Enable equalizer", "Controls are saved; audio processing will be connected later", state.preferences.equalizerEnabled, onEqEnabled) }
        item {
            Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                EqSlider("Preamp", state.preferences.preamp, state.preferences.equalizerEnabled, onPreamp)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                frequencies.forEachIndexed { index, label -> EqSlider(label, state.preferences.eqBands[index], state.preferences.equalizerEnabled) { onBand(index, it) } }
                TextButton(onClick = onResetEq, Modifier.align(Alignment.End)) { Text("RESET EQ") }
            }
        }
        if (state.usbDacs.isNotEmpty()) item { Text("USB audio: ${state.usbDacs.first().name}", Modifier.padding(22.dp), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun SettingsHeading(text: String) { Text(text.uppercase(), Modifier.padding(start = 22.dp, top = 22.dp, bottom = 10.dp), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.3.sp) }

@Composable private fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(.58f), fontSize = 12.sp) }; Switch(checked, change) }
}

@Composable private fun ThemeOption(name: String, dark: Boolean, background: androidx.compose.ui.graphics.Color, primary: androidx.compose.ui.graphics.Color, secondary: androidx.compose.ui.graphics.Color, selected: Boolean, choose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) MaterialTheme.colorScheme.primary.copy(.16f) else MaterialTheme.colorScheme.surface).clickable(onClick = choose).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(background, primary, secondary).forEach { color -> Box(Modifier.padding(end = 5.dp).size(22.dp).background(color, CircleShape)) }; Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold); Text(if (dark) "Dark mode" else "Light mode", color = MaterialTheme.colorScheme.onSurface.copy(.55f), fontSize = 11.sp) }; RadioButton(selected, choose)
    }
}

@Composable private fun EqSlider(label: String, value: Float, enabled: Boolean, change: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.width(60.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold); Slider(value, change, Modifier.weight(1f), enabled = enabled, valueRange = -12f..12f); Text("%+.1f".format(value), Modifier.width(48.dp), fontSize = 11.sp) }
}
