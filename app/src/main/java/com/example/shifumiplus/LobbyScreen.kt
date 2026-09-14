package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shifumiplus.domain.PlayerState
import androidx.compose.ui.tooling.preview.Preview
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun LobbyScreen(
    gameId: String,
    players: List<PlayerState>,
    playerWins: Map<String, Int>,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Spacer(modifier = Modifier.height(50.dp))
        Column(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Game ID", color = MaterialTheme.colorScheme.secondary)
                Text(text = gameId, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Players:",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.secondary
            )
            LazyColumn(
                modifier = Modifier.padding(top = 4.dp)) {
                items(players) { p ->
                    val wins = playerWins[p.name] ?: 0
                    val winsLabel = if (wins == 1) "1 victoire" else "${wins} victoires"
                    Text(
                        "  - ${p.name} ($winsLabel)",
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Button(onClick = onStart, modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
            Text("Commencer", color = MaterialTheme.colorScheme.secondary)
        }
        Button(onClick = onBack, modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
            Text("Retour", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LobbyScreenPreview() {
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        val players = listOf(
            PlayerState(name = "Pako", lives = 3, bullets = 2, protectedLastTurn = false, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false),
            PlayerState(name = "Alice", lives = 2, bullets = 1, protectedLastTurn = true, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false)
        )
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            LobbyScreen(
                gameId = "AR67QX",
                players = players,
                playerWins = mapOf("Pako" to 2, "Alice" to 1),
                onStart = {},
                onBack = {}
            )
        }
    }
}

