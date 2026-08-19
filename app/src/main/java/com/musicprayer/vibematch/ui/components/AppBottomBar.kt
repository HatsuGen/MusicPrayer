package com.musicprayer.vibematch.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.musicprayer.vibematch.ui.navigation.AppDestination

@Composable fun AppBottomBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination, onClick = { onSelect(destination) },
                icon = { Icon(painterResource(destination.iconRes), contentDescription = destination.label) }, label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(.2f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(.6f),
                ),
            )
        }
    }
}
