package com.example.shifumiplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shifumiplus.ui.theme.BackgroundColor
import androidx.compose.ui.tooling.preview.Preview
import com.example.shifumiplus.ui.theme.Bronson
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun LoadingScreen(text: String = "Loading...") {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .background(BackgroundColor)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Text(
                    text,
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = Bronson,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        LoadingScreen(text = "Creating game...")
    }
}