package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.shifumiplus.R
import com.example.shifumiplus.domain.PlayerState
import com.example.shifumiplus.ui.ActionButton
import com.example.shifumiplus.ui.PlayerPP
import com.example.shifumiplus.ui.PlayerView
import com.example.shifumiplus.ui.theme.ActionGreen
import com.example.shifumiplus.ui.theme.ActionGreenTame
import com.example.shifumiplus.ui.theme.ActionRed
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.Black
import com.example.shifumiplus.ui.theme.Bronson
import com.example.shifumiplus.ui.theme.DarkGrey
import com.example.shifumiplus.ui.theme.LiteGrey
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex

@Composable
fun GameScreen(players: List<PlayerState>, meId: String?, choices: Map<String, Map<String, Any>>, onSubmit: (action: String, target: String?) -> Unit, onAnimationCompleted: () -> Unit = {}) {
    val alivePlayers = players.filter { it.lives > 0 }
    val myState = players.find { it.id == meId }
    val isEliminated = (myState?.lives ?: 1) <= 0
    val myChoice = choices[meId]
    var targetMode by remember { mutableStateOf<String?>(null) }
    val selectedTargets = remember { mutableStateListOf<String>() }

    // global animation state for this screen so it's accessible from all inner scopes
    val isAnimating = remember { mutableStateOf(false) }
    val animMap = remember { androidx.compose.runtime.mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    val angleMap = remember { androidx.compose.runtime.mutableStateMapOf<String, Double>() }
    // state-backed distances so Compose recomposes each frame while bullets animate
    val bulletState = remember { androidx.compose.runtime.mutableStateMapOf<String, Float>() }
    val animScope = rememberCoroutineScope()
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val count = alivePlayers.size.coerceAtLeast(1)
                val meIndex =
                    alivePlayers.indexOfFirst { it.id == meId }.let { if (it == -1) 0 else it }

                // Build list of bullets to display from current choices: pairs of (shooterIndex, targetIndex)
                val radius = 140
                val shieldRadius = 70
                val bulletsPairs = remember(alivePlayers, choices, meIndex) {
                    val list = mutableListOf<Pair<Int, Int>>()
                    alivePlayers.forEachIndexed { j, p ->
                        val relShooter = (j - meIndex + count) % count
                        val c = choices[p.id]
                        val action = c?.get("action") as? String
                        val targetRaw = c?.get("target") as? String
                        if (action != null && targetRaw != null) {
                            when (action) {
                                "Shoot" -> {
                                    val targetId = targetRaw.trim()
                                    val tIndex = alivePlayers.indexOfFirst { it.id == targetId }
                                    if (tIndex != -1) {
                                        val relTarget = (tIndex - meIndex + count) % count
                                        if (relShooter != relTarget) list.add(relShooter to relTarget)
                                    }
                                }
                                "DoubleShoot" -> {
                                    val parts = targetRaw.split(";").map { it.trim() }
                                    parts.forEach { tid ->
                                        val tIndex = alivePlayers.indexOfFirst { it.id == tid }
                                        if (tIndex != -1) {
                                            val relTarget = (tIndex - meIndex + count) % count
                                            val rel = relShooter
                                            if (rel != relTarget) list.add(rel to relTarget)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    list
                }

                // animation state for bullets (moved to top-level of composable)
                val scope = animScope

                // detect whether all alive players have chosen
                val activeChoicesCount = alivePlayers.count { choices.containsKey(it.id) }
                val allChosen = alivePlayers.isNotEmpty() && activeChoicesCount == alivePlayers.size

                // Start animation only when everyone has chosen
                LaunchedEffect(allChosen) {
                    if (allChosen && bulletsPairs.isNotEmpty() && !isAnimating.value) {
                        // capture snapshot immediately and run animation in separate scope
                        val snapshot = bulletsPairs.toList()
                        if (snapshot.isEmpty()) return@LaunchedEffect
                        if (isAnimating.value) return@LaunchedEffect
                        // launch independent coroutine so it's not cancelled by recomposition
                        scope.launch {

                            // short stabilization delay to avoid races when backend clears choices immediately
                            kotlinx.coroutines.delay(150)
                            // re-check that the snapshot and allChosen are still valid
                            if (!allChosen) return@launch
                            if (bulletsPairs.toList() != snapshot) return@launch
                            if (isAnimating.value) return@launch

                            isAnimating.value = true
                            animMap.clear()
                            angleMap.clear()

                        try {
                            // create bullet ids and anims
                            val bulletIds = snapshot.mapIndexed { idx, pair -> "${pair.first}:${pair.second}" }
                            snapshot.forEachIndexed { idx, pair ->
                                val (shooterRel, targetRel) = pair
                                val shooterAngle = ((2 * Math.PI * shooterRel / count) + Math.PI / 2) % (2 * Math.PI)
                                val targetAngle = ((2 * Math.PI * targetRel / count) + Math.PI / 2) % (2 * Math.PI)
                                val sx = (radius * cos(shooterAngle)).toFloat()
                                val sy = (radius * sin(shooterAngle)).toFloat()
                                val tx = (radius * cos(targetAngle)).toFloat()
                                val ty = (radius * sin(targetAngle)).toFloat()
                                val dx = tx - sx
                                val dy = ty - sy
                                // store angle from shooter to target
                                val bulletAngle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                                val id = bulletIds[idx]
                                angleMap[id] = bulletAngle
                                val anim = Animatable(0f)
                                animMap[id] = anim
                            }

                            // animate all and wait for completion (use LaunchedEffect's coroutine scope)
                            // animate bullets sequentially so each bullet travels ~500ms
                            val perBulletMs = 500L
                            val betweenMs = 50L
                            for ((idx, pair) in snapshot.withIndex()) {
                                val id = bulletIds[idx]
                                val (shooterRel, targetRel) = pair
                                val shooterAngle = ((2 * Math.PI * shooterRel / count) + Math.PI / 2) % (2 * Math.PI)
                                val targetAngle = ((2 * Math.PI * targetRel / count) + Math.PI / 2) % (2 * Math.PI)
                                val sx = (radius * cos(shooterAngle)).toFloat()
                                val sy = (radius * sin(shooterAngle)).toFloat()
                                val tx = (radius * cos(targetAngle)).toFloat()
                                val ty = (radius * sin(targetAngle)).toFloat()
                                val dx = tx - sx
                                val dy = ty - sy
                                val dist = sqrt(dx * dx + dy * dy)

                                println("??? dist = ${dist} for bullet ${id} (idx=${idx})")
                                println("??? animation starts for bullet ${id} (idx=${idx})")

                                // initialize state-backed distance
                                bulletState[id] = 0f

                                // deterministic frame loop to ensure Compose recomposes and shows movement
                                val startTs = System.currentTimeMillis()
                                var elapsed: Long
                                do {
                                    elapsed = System.currentTimeMillis() - startTs
                                    val frac = (elapsed.toFloat() / perBulletMs.toFloat()).coerceIn(0f, 1f)
                                    val cur = frac * dist
                                    bulletState[id] = cur
                                    // also keep animMap for compatibility (not relied upon for rendering)
                                    animMap[id]?.snapTo(cur)
                                    kotlinx.coroutines.delay(16)
                                } while (elapsed < perBulletMs)

                                // ensure final position
                                bulletState[id] = dist
                                animMap[id]?.snapTo(dist)

                                val endTs = System.currentTimeMillis()
                                val took = endTs - startTs
                                println("??? animation ends for bullet ${id} (elapsed=${took} ms)")
                                kotlinx.coroutines.delay(betweenMs)
                            }

                            // small pause so end state is visible for a moment
                            kotlinx.coroutines.delay(350)

                        } finally {
                            // end animation phase (always run cleanup)
                            isAnimating.value = false
                            println("??? all animations joined, calling onAnimationCompleted()")
                            animMap.clear()
                            angleMap.clear()
                            // notify viewmodel/UI that animation completed so choices may be re-enabled
                            try {
                                onAnimationCompleted()
                            } catch (e: Exception) {
                                println("!!! onAnimationCompleted threw: ${e}")
                            }
                        }

                    }
                }

                }
                alivePlayers.forEachIndexed { j, player ->
                    val relativeIndex = (j - meIndex + count) % count
                    val angle = ((2 * Math.PI * relativeIndex / count) + Math.PI / 2) % (2 * Math.PI)
                    val x = (radius * cos(angle)).toFloat()
                    val y = (radius * sin(angle)).toFloat()
                    val shieldX = (shieldRadius * cos(angle)).toFloat()
                    val shieldY = (shieldRadius * sin(angle)).toFloat()
                    val showShield = false
                    val showSuperShield = false

                    PlayerView(
                        player = player,
                        modifier = Modifier
                            .width(100.dp)
                            .height(if (players.size <= 7) 110.dp else 90.dp)
                            .offset(x.dp, y.dp)
                            .align(Alignment.Center)
                            .then(
                                if (myChoice == null && targetMode != null && player.id != meId) {
                                    Modifier.clickable {
                                        if (targetMode == "Double") {
                                            selectedTargets.add(player.id)
                                            if (selectedTargets.size == 2) {
                                                val tstr = selectedTargets.joinToString(";")
                                                onSubmit("DoubleShoot", tstr)
                                                selectedTargets.clear()
                                                targetMode = null
                                            }
                                        } else {
                                            val act =
                                                if (targetMode == "Shoot") "Shoot" else "Block"
                                            onSubmit(act, player.id)
                                            targetMode = null
                                        }
                                    }
                                } else Modifier
                            ),
                        ppsize = if (players.size <= 7) 60 else 40,
                        if (myChoice == null) if (player.id in selectedTargets) "Shoot" else ""
                                else if (player.id == meId) if (myChoice["target"] == null) myChoice["action"] as? String ?: "" else ""
                                else if (myChoice["target"] != null && player.id == myChoice["target"] as String) myChoice["action"] as? String ?: ""
                                else if (myChoice["target"] != null && "${ player.id };${ player.name }" == myChoice["target"] as String) "DoubleShoot"
                                else if (myChoice["target"] != null && (
                                    player.name == (myChoice["target"] as String).substringBefore(";") ||
                                    player.name == (myChoice["target"] as String).substringAfter(";"))) "Shoot"
                                else "",
                        isMe = player.id == meId
                        )

                    if (showShield) Image(
                        painter = painterResource(id = R.drawable.topsideshield),
                        contentDescription = "Description de l'image",
                        modifier = Modifier
                            .height(if (players.size <= 7) 20.dp else 15.dp)
                            .offset(shieldX.dp, shieldY.dp)
                            .align(Alignment.Center)
                            .rotate((angle * 180 / Math.PI).toFloat() - 90f),
                    ) else if (showSuperShield) Image(
                        painter = painterResource(id = R.drawable.topsidesupershield),
                        contentDescription = "Description de l'image",
                        modifier = Modifier
                            .height(if (players.size <= 7) 20.dp else 15.dp)
                            .offset(shieldX.dp, shieldY.dp)
                            .align(Alignment.Center)
                            .rotate((angle * 180 / Math.PI).toFloat() - 90f),
                    ) else {
                        // draw bullets originating from this player while animating (or statically if no anim)
                        bulletsPairs.forEachIndexed { bidx, pair ->
                            val (shooterRel) = pair
                            println("??? shooterRel == relativeIndex : ${shooterRel == relativeIndex}")
                            if (shooterRel == relativeIndex) {
                                val id = "${'$'}{pair.first}:${'$'}{pair.second}"
                                println("??? angleMap = ${angleMap.size}")
                                println("??? animMap = ${animMap.size}")
                                val bulletAngle = angleMap[id]
                                println("??? bullet ${id} angle=${bulletAngle} rad")
                                val curDist = bulletState[id] ?: 0f
                                println("??? bullet ${id} curDist=${curDist}")
                                if (bulletAngle != null) {
                                    val bulletX = x + (curDist * cos(bulletAngle)).toFloat()
                                    val bulletY = y + (curDist * sin(bulletAngle)).toFloat()
                                    println("??? drawing bullet ${id} at (${bulletX}, ${bulletY}) with angle ${bulletAngle} rad")
                                    val rotationDeg = (bulletAngle * 180 / Math.PI).toFloat()
                                    // bullet image (visible, sized, and placed above other UI)
                                    Image(
                                        painter = painterResource(id = R.drawable.bullet),
                                        contentDescription = "bullet",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        modifier = Modifier
                                            .size(if (players.size <= 7) 20.dp else 15.dp)
                                            .offset(bulletX.dp, bulletY.dp)
                                            .align(Alignment.Center)
                                            .zIndex(1f)
                                            .rotate(rotationDeg + 90f)
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (isEliminated) {
                        Text(
                            "☠",
                            fontSize = 70.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = LiteGrey
                        )
                    } else if (myChoice == null) {
                        Text(
                            "Que vas tu faire :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = Bronson,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (isAnimating.value) {
                            Text(
                                "Animation en cours...",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (targetMode == null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row {
                                    Row(modifier = Modifier.weight(1f)) {
                                        ActionButton(
                                            icon = R.drawable.reload,
                                            enabled = true,
                                            onPress = { onSubmit("Reload", null) })
                                    }
                                    Row(modifier = Modifier.weight(1f)) {
                                        ActionButton(
                                            icon = R.drawable.shield,
                                            enabled = myState?.protectedLastTurn != true,
                                            onPress = { onSubmit("Protect", null) })
                                    }
                                    Row(modifier = Modifier.weight(1f)) {
                                        ActionButton(
                                            icon = R.drawable.shoot,
                                            enabled = (myState?.bullets ?: 0) > 0,
                                            onPress = { targetMode = "Shoot" })
                                    }
                                }
                                Row {
                                    Row(modifier = Modifier.weight(1f)) {
                                        ActionButton(
                                            icon = R.drawable.doubleshoot,
                                            redStyle = true,
                                            enabled = (myState?.bullets
                                                ?: 0) >= 2 && (myState?.usedDoubleShoot != true),
                                            onPress = { targetMode = "Double" })
                                    }
                                    Row(modifier = Modifier.weight(1f)) {
                                        ActionButton(
                                            icon = R.drawable.supershield,
                                            redStyle = true,
                                            enabled = myState?.usedSuperProtection != true,
                                            onPress = { onSubmit("SuperProtect", null) })
                                    }
                                    Row(modifier = Modifier.weight(1f)) {
                                        ActionButton(
                                            icon = R.drawable.bombe,
                                            redStyle = true,
                                            enabled = (myState?.bullets
                                                ?: 0) >= 2 && (myState?.usedBomb != true),
                                            onPress = { onSubmit("Bomb", null) })
                                    }
                                    Row(modifier = Modifier.weight(1f)) {
                                        ActionButton(
                                            icon = R.drawable.stop,
                                            redStyle = true,
                                            enabled = myState?.usedBlock != true,
                                            onPress = { targetMode = "Block" })
                                    }
                                }
                            }
                        } else {
                            Text(
                                when (targetMode) {
                                    "Shoot" -> "Clique sur le joueur cible."
                                    "Double" -> "Clique sur les deux joueurs cibles. (tu peux choisir le même joueur)"
                                    "Block" -> "Clique sur le joueur à bloquer."
                                    else -> "Choisis une cible"
                                }, color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text(
                            "En attente des autres joueurs...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontFamily = Bronson,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val otherPlayers = players.filter { it.id != meId }
                        if (otherPlayers.isNotEmpty()) {
                            Column(
                                modifier =
                                    if (otherPlayers.size < 3) Modifier
                                        .fillMaxWidth()
                                        .height((60 * otherPlayers.size).dp)
                                    else Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                            ){
                                otherPlayers.forEach { p ->
                                    val chosen = choices.containsKey(p.id)
                                    val stateText = when {
                                        p.lives <= 0 -> "eliminated"
                                        chosen -> "chosen"
                                        else -> "penting"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when (stateText) {
                                                    "eliminated" -> Black
                                                    "chosen" -> ActionGreen
                                                    else -> DarkGrey
                                                }
                                            )
                                            .padding(5.dp)
                                    ) {
                                        Row (
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PlayerPP(Modifier.fillMaxHeight())
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                p.name,
                                                fontSize = if (otherPlayers.size >= 3) (17 - otherPlayers.size).sp else 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .align(Alignment.CenterVertically),
                                                color = when (stateText) {
                                                    "eliminated" -> LiteGrey
                                                    "chosen" -> ActionGreenTame
                                                    else -> ActionRed
                                                },
                                                style = LocalTextStyle.current.copy(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    ),
                                                    lineHeightStyle = LineHeightStyle(
                                                        alignment = LineHeightStyle.Alignment.Center,
                                                        trim = LineHeightStyle.Trim.Both
                                                    )
                                                )
                                            )
                                            Text(
                                                when (stateText) {
                                                    "eliminated" -> "☠"
                                                    "chosen" -> "✔"
                                                    else -> "..."
                                                },
                                                fontSize = if (otherPlayers.size >= 3) (20 - otherPlayers.size*4/3).sp else 15.sp,
                                                fontWeight = when (stateText) {
                                                    "eliminated" -> FontWeight.Thin
                                                    "chosen" -> FontWeight.Thin
                                                    else -> FontWeight.Bold
                                                },
                                                color = when (stateText) {
                                                    "eliminated" -> LiteGrey
                                                    "chosen" -> ActionGreenTame
                                                    else -> ActionRed
                                                },
                                                style = LocalTextStyle.current.copy(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    ),
                                                    lineHeightStyle = LineHeightStyle(
                                                        alignment = LineHeightStyle.Alignment.Center,
                                                        trim = LineHeightStyle.Trim.Both
                                                    )
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun GameScreenPreview() {
    val players = listOf(
        PlayerState(id = "p1", name = "Pako", lives = 2, bullets = 2, protectedLastTurn = false, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false),
        //PlayerState(id = "p2", name = "Alice", lives = 2, bullets = 1, protectedLastTurn = true, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false),
        //PlayerState(id = "p3", name = "Alice", lives = 2, bullets = 1, protectedLastTurn = true, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false),
        //PlayerState(id = "p4", name = "Alice", lives = 2, bullets = 1, protectedLastTurn = true, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false),
        PlayerState(id = "p5", name = "Alice", lives = 2, bullets = 1, protectedLastTurn = true, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false),
        PlayerState(id = "p6", name = "Alice", lives = 2, bullets = 1, protectedLastTurn = true, usedDoubleShoot = false, usedSuperProtection = false, usedBomb = false, usedBlock = false),
        PlayerState(id = "p7", name = "Bob", lives = 1, bullets = 0, protectedLastTurn = false, usedDoubleShoot = true, usedSuperProtection = false, usedBomb = false, usedBlock = false),
        PlayerState(id = "p8", name = "Eve", lives = 4, bullets = 3, protectedLastTurn = false, usedDoubleShoot = false, usedSuperProtection = true, usedBomb = false, usedBlock = false),
        PlayerState(id = "p9", name = "Edd", lives = 0, bullets = 3, protectedLastTurn = false, usedDoubleShoot = false, usedSuperProtection = true, usedBomb = false, usedBlock = false),
        PlayerState(id = "p10", name = "Steve", lives = 3, bullets = 5, protectedLastTurn = false, usedDoubleShoot = false, usedSuperProtection = true, usedBomb = false, usedBlock = false)
    )

    val choices: Map<String, Map<String, Any>> = mapOf(
        "p1" to mapOf("action" to "Protect"),
        "p2" to mapOf("action" to "Protect"),
        "p3" to mapOf("action" to "DoubleShoot", "target" to "p2;p4")
    )

    // Force light theme + disable dynamic colors so preview background is white
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        GameScreen(players = players, meId = "p1", choices = choices, onSubmit = { _, _ -> })
    }
}