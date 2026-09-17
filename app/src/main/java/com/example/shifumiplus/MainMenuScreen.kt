package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.MenuButton
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun MainMenuScreen(onNewGame: () -> Unit, onJoin: () -> Unit) {
    Box(
        modifier = Modifier
            .background(BackgroundColor)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .padding(5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MenuButton("Nouvelle partie", onNewGame)
            Spacer(modifier = Modifier.height(10.dp))
            MenuButton("Rejoindre une partie", onJoin)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        MainMenuScreen(onNewGame = {}, onJoin = {})
    }
}
