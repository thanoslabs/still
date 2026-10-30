package dev.j6n.still

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StillApp() }
    }
}

enum class StillScreen { CAMERA, REVIEW, LOOKS }

@Composable
fun StillApp() {
    var screen by remember { mutableStateOf(StillScreen.CAMERA) }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(bottomBar = {
            NavigationBar {
                StillScreen.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = screen == destination,
                        onClick = { screen = destination },
                        icon = { Text(when (destination) {
                            StillScreen.CAMERA -> "◉"
                            StillScreen.REVIEW -> "▣"
                            StillScreen.LOOKS -> "◈"
                        }) },
                        label = { Text(destination.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("still.", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(12.dp))
                Text(when (screen) {
                    StillScreen.CAMERA -> "Camera foundation · capture coming next"
                    StillScreen.REVIEW -> "Non-destructive photo review coming next"
                    StillScreen.LOOKS -> "Cinematic looks coming next"
                })
            }
        }
    }
}
