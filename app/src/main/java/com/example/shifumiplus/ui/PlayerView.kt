package com.example.shifumiplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shifumiplus.domain.PlayerState
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun PlayerView(player: PlayerState, modifier: Modifier = Modifier) {
    // Modifier should include size/offset/align when called from a Box scope
    Box(modifier = modifier
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.tertiary)
        .padding(0.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
            Text(player.name, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
            Text("Lives: ${player.lives}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Text("Bullets: ${player.bullets}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            if (player.protectedLastTurn) Text("Protected", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlayerViewPreview() {
    // Force light theme for preview
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        val player = PlayerState(
            name = "Pako",
            lives = 3,
            bullets = 2,
            protectedLastTurn = true,
            usedDoubleShoot = false,
            usedSuperProtection = false,
            usedBomb = false,
            usedBlock = false
        )
        // Center the PlayerView in preview
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            PlayerView(player = player, modifier = Modifier.size(100.dp))
        }
    }
}
