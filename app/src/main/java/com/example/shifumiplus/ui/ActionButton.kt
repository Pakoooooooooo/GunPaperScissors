package com.example.shifumiplus.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun ActionButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPress: () -> Unit
) {
    Button(
        onClick = onPress,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().padding(vertical = 1.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.secondary)
    }
}

@Preview(showBackground = true)
@Composable
fun ActionButtonPreview() {
    // Force light theme and disable dynamic colors so preview background is white
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            ActionButton(title = "Recharger", enabled = true, onPress = {})
            Spacer(modifier = Modifier.height(8.dp))
            ActionButton(title = "Tirer (disabled)", enabled = false, onPress = {})
        }
    }
}
