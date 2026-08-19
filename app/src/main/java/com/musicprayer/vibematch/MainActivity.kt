package com.musicprayer.vibematch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.musicprayer.vibematch.ui.MusicPrayerApp

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                uri?.let { selectedUri ->
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    runCatching { contentResolver.takePersistableUriPermission(selectedUri, flags) }
                        .onSuccess { viewModel.selectFolder(selectedUri) }
                        .onFailure { error -> viewModel.reportFolderError(error.localizedMessage ?: "Folder access was not granted") }
                }
            }
            MusicPrayerApp(viewModel = viewModel, chooseFolder = { folderPicker.launch(null) })
        }
    }
}
