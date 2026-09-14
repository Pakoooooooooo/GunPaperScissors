package com.example.shifumiplus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shifumiplus.R
import com.example.shifumiplus.ui.theme.ActionGreen
import com.example.shifumiplus.ui.theme.ActionGreenTame
import com.example.shifumiplus.ui.theme.ActionRed
import com.example.shifumiplus.ui.theme.ActionRedTame
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun ActionButton(
    icon: Int,
    modifier: Modifier = Modifier,
    redStyle: Boolean = false,
    enabled: Boolean = true,
    onPress: () -> Unit
) {
    Button(
        onClick = onPress,
        enabled = enabled,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(5.dp, if(enabled) {
            if (redStyle) ActionRed else ActionGreen
        } else MaterialTheme.colorScheme.tertiary),
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(
                vertical = 3.dp,
                horizontal = 3.dp
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if(redStyle) ActionRedTame else ActionGreenTame,
            contentColor = MaterialTheme.colorScheme.secondary
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = "Description de l'image",
                alpha = if(enabled) 1f else 0.5f,
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            //Spacer(modifier = Modifier.height(5.dp))
            //Text(text = title, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionButtonPreview() {
    // Force light theme and disable dynamic colors so preview background is white
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            ActionButton(icon = R.drawable.shoot, enabled = true, onPress = {})
            Spacer(modifier = Modifier.height(8.dp))
            ActionButton(icon = R.drawable.bombe, redStyle = true, enabled = false, onPress = {})
        }
    }
}
