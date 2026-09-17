package com.example.shifumiplus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shifumiplus.R
import com.example.shifumiplus.domain.PlayerState
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun PlayerView(player: PlayerState, modifier: Modifier = Modifier, action: String = "") {
    // Modifier should include size/offset/align when called from a Box scope
    Box(modifier = modifier.clip(MaterialTheme.shapes.medium)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerPP(
                Modifier.size(60.dp),
                action
            )
            Text(
                player.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            ItemList(player.lives, "heart", 10.dp)
            Spacer(modifier = Modifier.height(3.dp))
            ItemList(player.bullets, "bullet", 12.dp)
            if (player.protectedLastTurn) Text("Protected", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun OverlappingRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val count = placeables.size

        if (count == 0) {
            return@Layout layout(0, 0) {}
        }

        val itemWidth = placeables.maxOf { it.width }
        val maxHeight = placeables.maxOf { it.height }
        val maxWidth = constraints.maxWidth

        // Calcule le décalage entre chaque élément
        // Si tout rentre sans déborder, on utilise la largeur normale
        // Sinon, on compresse l'espacement pour que tout tienne dans maxWidth
        val step = if (count * itemWidth <= maxWidth || count == 1) {
            itemWidth
        } else {
            (maxWidth - itemWidth) / (count - 1)
        }

        val totalWidth = if (count == 1) itemWidth else minOf(maxWidth, itemWidth + (count - 1) * step)

        layout(totalWidth, maxHeight) {
            var xPosition = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x = xPosition, y = 0)
                xPosition += step
            }
        }
    }
}

@Composable
fun ItemList(number: Int, type: String, size: Dp) {
    val drawableRes = when (type) {
        "bullet" -> R.drawable.bullet
        else -> R.drawable.heart
    }
    Row (
        modifier = Modifier.padding(horizontal = 35.dp)
    ) {
        OverlappingRow(modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until number) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlayerViewPreview() {
    // Force light theme for preview
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        val player = PlayerState(
            id = "hgdicygw",
            name = "Pako",
            lives = 5,
            bullets = 2,
            protectedLastTurn = true,
            usedDoubleShoot = false,
            usedSuperProtection = false,
            usedBomb = false,
            usedBlock = false
        )
        // Center the PlayerView in preview
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            PlayerView(
                player = player,
                modifier = Modifier,
                "reload")
        }
    }
}
