package com.example.shifumiplus.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.shifumiplus.R

@Composable
fun PlayerPP(modifier: Modifier){
    Image(
        painter = painterResource(id = R.drawable.face),
        contentDescription = "Description de l'image",
        modifier = modifier
    )
}

@Preview
@Composable
fun PlayerPPPreview(){
    PlayerPP(Modifier)
}