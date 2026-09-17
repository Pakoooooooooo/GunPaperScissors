package com.example.shifumiplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.Bronson
import com.example.shifumiplus.ui.theme.MenuButton
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun FinalRankingScreen(ranking: List<String>, onFinish: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .background(BackgroundColor)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Classement final",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Bronson,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                ranking.forEachIndexed { index, name ->
                    Text(
                        buildAnnotatedString {
                            append("${index + 1}. ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(name)
                            }
                        },
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                MenuButton("Terminer", onFinish)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinalRankingScreenPreview() {
    ShiFuMiPlusTheme {
        FinalRankingScreen(ranking = listOf("Alice", "Bob", "Charlie")) {}
    }
}