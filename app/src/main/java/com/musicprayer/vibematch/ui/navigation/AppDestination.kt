package com.musicprayer.vibematch.ui.navigation

import androidx.annotation.DrawableRes
import com.musicprayer.vibematch.R

enum class AppDestination(val label: String, @DrawableRes val iconRes: Int) {
    HOME("Home", R.drawable.ic_home),
    LIBRARY("Music", R.drawable.ic_library_music),
    SETTINGS("Settings", R.drawable.ic_settings),
}
