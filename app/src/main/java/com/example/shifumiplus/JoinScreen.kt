package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.MenuButton
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun JoinScreen(onJoin: (String) -> Unit) {
    var id by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .background(BackgroundColor)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(70.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Enter game ID",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(5.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { if (it.length <= 6) id = it.uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                )
                MenuButton(
                    "Rejoindre",
                    { if (id.isNotBlank()) onJoin(id.trim()) },
                    id.length == 6
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JoinScreenPreview() {
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        JoinScreen(onJoin = {})
    }
}

