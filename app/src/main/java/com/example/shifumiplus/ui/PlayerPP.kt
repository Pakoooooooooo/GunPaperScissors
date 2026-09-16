package com.example.shifumiplus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.shifumiplus.R

@Composable
fun PlayerPP(modifier: Modifier, action: String = ""){
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.face),
            contentDescription = "Description de l'image",
            modifier = modifier
        )
        when (action) {
            "" -> Unit
            "Shoot" ->
                Image(
                    painter = painterResource(id = R.drawable.shoot),
                    contentDescription = "Description de l'image",
                    modifier = Modifier.align(alignment = Alignment.Center)
                )
            "DoubleShoot" ->
                Image(
                    painter = painterResource(id = R.drawable.doubleshoot),
                    contentDescription = "Description de l'image",
                    modifier = Modifier.align(alignment = Alignment.Center)
                )
            "Block" ->
                Image(
                    painter = painterResource(id = R.drawable.stop),
                    contentDescription = "Description de l'image",
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .align(alignment = Alignment.Center)
                )
            else -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Image(
                    painter = painterResource(id = when(action) {
                        "Reload" -> R.drawable.reload
                        "Protect" -> R.drawable.shield
                        "SuperProtect" -> R.drawable.supershield
                        "Bomb" -> R.drawable.bombe
                        else -> R.drawable.stop
                    }),
                    contentDescription = "Description de l'image",
                    modifier = Modifier.fillMaxSize(0.4f)
                )
            }
        }
    }
}

@Preview
@Composable
fun PlayerPPPreview(){
    PlayerPP(Modifier)
}

@Preview
@Composable
fun PlayerPPPreviewShot(){
    PlayerPP(Modifier, "Shoot")
}

@Preview
@Composable
fun PlayerPPPreviewBlocked(){
    PlayerPP(Modifier, "Block")
}

@Preview
@Composable
fun PlayerPPPreviewDoubleShot(){
    PlayerPP(Modifier, "DoubleShoot")
}

@Preview
@Composable
fun PlayerPPPreviewReload(){
    PlayerPP(Modifier, "Reload")
}

@Preview
@Composable
fun PlayerPPPreviewProtect(){
    PlayerPP(Modifier, "Protect")
}

@Preview
@Composable
fun PlayerPPPreviewBombe(){
    PlayerPP(Modifier, "Bomb")
}

@Preview
@Composable
fun PlayerPPPreviewSuperProtect(){
    PlayerPP(Modifier, "SuperProtect")
}