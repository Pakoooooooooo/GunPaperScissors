package com.example.shifumiplus.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuButton(text: String, onPress: () -> Unit) {
    Button(
        onClick = onPress,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(3.dp, DarkGrey),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ActionRed)
    ) {
        Text(
            text,
            color = DarkGrey,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}