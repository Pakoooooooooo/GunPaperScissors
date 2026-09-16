package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shifumiplus.ui.PlayerPP
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.MenuButton
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun NameScreen(initial: String = "", onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    Box(
        modifier = Modifier
            .background(BackgroundColor)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(30.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                PlayerPP(Modifier.size(150.dp))
            }
            Spacer(modifier = Modifier.height(50.dp))
            Text(
                "Choose your name",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(5.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 20) name = it },
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
                "Confirm",
                { if (name.isNotBlank()) onConfirm(name.trim()) },
                name.isNotBlank()
            )
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@Preview
@Composable
fun NameScreenPreview(){
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        NameScreen(
            "Jeanne-Alexandra"
        ) {}
    }
}