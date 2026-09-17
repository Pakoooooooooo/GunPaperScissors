package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.shifumiplus.R
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
                .padding(50.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally){
            Image(
                painter = painterResource(id = R.drawable.title),
                contentDescription = "Description de l'image",
                modifier = Modifier
            )
            Spacer(modifier = Modifier.height(60.dp))
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .padding(5.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MenuButton("Nouvelle partie", onNewGame)
                Spacer(modifier = Modifier.height(10.dp))
                MenuButton("Rejoindre une partie", onJoin)
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Sonapyx")
                    withStyle(
                        style = SpanStyle(
                            baselineShift = BaselineShift.Superscript,
                            fontSize = 10.sp // ajuste selon la taille de ton texte principal
                        )
                    ) {
                        append("®")
                    }
                },
                modifier = Modifier.padding(10.dp),
                color = MaterialTheme.colorScheme.secondary
            )
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
