package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.TrackRepository
import com.example.ui.DjConsole
import com.example.ui.DjViewModel
import com.example.ui.DjViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        val context = LocalContext.current
        val database = AppDatabase.getDatabase(context.applicationContext)
        val repository = TrackRepository(database.trackDao)
        val vm: DjViewModel = viewModel(
          factory = DjViewModelFactory(context.applicationContext, repository)
        )

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          DjConsole(
            viewModel = vm,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}
