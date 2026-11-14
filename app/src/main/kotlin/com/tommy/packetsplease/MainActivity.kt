package com.tommy.packetsplease

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tommy.packetsplease.ui.GameViewModel
import com.tommy.packetsplease.ui.PacketsPleaseApp
import com.tommy.packetsplease.ui.theme.PacketsPleaseTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
    )
    setContent {
      PacketsPleaseTheme {
        val gameViewModel: GameViewModel = viewModel()
        PacketsPleaseApp(viewModel = gameViewModel)
      }
    }
  }
}
