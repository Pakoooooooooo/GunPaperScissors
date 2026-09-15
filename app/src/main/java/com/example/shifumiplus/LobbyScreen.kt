package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.DarkGrey
import com.example.shifumiplus.ui.theme.MenuButton
import com.example.shifumiplus.ui.PlayerPP

@Composable
fun LobbyScreen(
    gameId: String,
    players: List<PlayerState>,
    playerWins: Map<String, Int>,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(BackgroundColor)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            Spacer(modifier = Modifier.height(50.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Game ID",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = gameId.uppercase(),
                        fontSize = 45.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = "Players:",
                    modifier = Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(players) { p ->
                        val wins = playerWins[p.name] ?: 0
                        val winsLabel = if (wins == 1) "1 victoire" else "${wins} victoires"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(DarkGrey)
                                .padding(8.dp)
                        ) {
                            Row (
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically) {
                                PlayerPP(Modifier.fillMaxHeight())
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(p.name)
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    winsLabel,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
            MenuButton("Commencer", onStart)
            //MenuButton("Retour", onBack)
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
            .fillMaxSize()) {
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

