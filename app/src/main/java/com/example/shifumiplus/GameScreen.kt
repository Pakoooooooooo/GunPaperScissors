package com.example.shifumiplus.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("MutableCollectionMutableState")
@Composable
fun GameScreen(
    players: List<PlayerState>,
    meId: String?,
    choices: Map<String, Map<String, Any>>,
    onSubmit: (action: String, target: String?) -> Unit,
    onAnimationsCompleted: () -> Unit = {}
) {
    val alivePlayers = players.filter { it.lives > 0 }
    val myState = players.find { it.id == meId }
    val isEliminated = (myState?.lives ?: 1) <= 0
    val myChoice = choices[meId]

    // remember last seen full-choices snapshot so startAnimation triggers only on new rounds
    val lastResolvedChoices = remember { mutableStateOf<Map<String, Map<String, Any>>?>(null) }
    var targetMode by remember { mutableStateOf<String?>(null) }
    val selectedTargets = remember { mutableStateListOf<String>() }
    val bulletRadius = 10f
    val bulletRelativePos = remember { Animatable(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    var isAnimatingBullet by remember { mutableStateOf(false) }
    var isAnimatingBomb by remember { mutableStateOf(false) }
    var isAnimatingStops by remember { mutableStateOf(false) }
    var isAnimatingReload by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var curShotPair by remember { mutableStateOf(0 to 0) }
    var curBombThrower by remember { mutableIntStateOf(0) }
    var curReloader by remember { mutableIntStateOf(0) }
    var showShields by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showSuperShields by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showStopedPlayers by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun startAnimation(
        reloads: List<Int>,
        stops: List<Pair<Int, Int>>,
        shots: List<Pair<Int, Int>>,
        shields: List<Int>,
        superShields: List<Int>
    ) {
        scope.launch {
            // map player ids to absolute indices in alivePlayers
            isAnimating = true
            showShields = shields
            showSuperShields = superShields

            bulletRelativePos.animateTo(
                targetValue = 100f,
                animationSpec = tween(durationMillis = 200, easing = LinearEasing)
            )
            bulletRelativePos.snapTo(0f)

            reloads.forEach { loader ->
                isAnimatingReload = true
                curReloader = loader
                bulletRelativePos.animateTo(
                    targetValue = 100f,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                )
                isAnimatingReload = false
                bulletRelativePos.snapTo(0f)
            }
            stops.forEach { pair ->
                isAnimatingStops = true
                curShotPair = pair
                bulletRelativePos.animateTo(
                    targetValue = 100f,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                )
                showStopedPlayers = showStopedPlayers + pair.second
                isAnimatingStops = false
                bulletRelativePos.snapTo(0f)
            }
            shots.forEach { pair ->
                // for bombs pair.first == pair.second
                if (pair.first == pair.second) {
                    isAnimatingBomb = true
                    curBombThrower = pair.first
                    bulletRelativePos.animateTo(
                        targetValue = 100f,
                        animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                    )
                    isAnimatingBomb = false
                    bulletRelativePos.snapTo(0f)
                } else {
                    isAnimatingBullet = true
                    curShotPair = pair
                    bulletRelativePos.animateTo(
                        targetValue = 100f,
                        animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                    )
                    isAnimatingBullet = false
                    bulletRelativePos.snapTo(0f)
                }

            }
            showStopedPlayers = emptyList()

            bulletRelativePos.animateTo(
                targetValue = 100f,
                animationSpec = tween(durationMillis = 200, easing = LinearEasing)
            )
            bulletRelativePos.snapTo(0f)

            isAnimating = false
            // notify host that animations ended for this resolved turn (used to delay final ranking)
            try {
                onAnimationsCompleted()
            } catch (t: Throwable) {
                // swallow: callback must not break UI
                println("!!! onAnimationsCompleted callback threw: ${'$'}t")
            }
        }
    }

    // start animation automatically when server returns choices for every alive player
    LaunchedEffect(choices, players) {
        val aliveIds = alivePlayers.map { it.id }
        val hasAll = aliveIds.isNotEmpty() && choices.keys.containsAll(aliveIds)
        if (hasAll && lastResolvedChoices.value != choices) {
            // build parameter lists from choices map
            val reloadsIds = mutableListOf<String>()
            val idToIndex = alivePlayers.mapIndexed { idx, p -> p.id to idx }.toMap()
            val reloads = mutableListOf<Int>()
            val stops = mutableListOf<Pair<Int, Int>>()
            val shots = mutableListOf<Pair<Int, Int>>()
            val shields = mutableListOf<Int>()
            val superShields = mutableListOf<Int>()

            choices.forEach { (pid, c) ->
                val action = c["action"] as? String ?: return@forEach
                val targetRaw = c["target"] as? String
                val ai = idToIndex[pid]
                when (action) {
                    "Reload" -> ai?.let { reloads.add(it) }
                    "Protect" -> ai?.let { shields.add(it) }
                    "SuperProtect" -> ai?.let { superShields.add(it) }
                    "Block" -> if (targetRaw != null) {
                        val bi = idToIndex[targetRaw.trim()]
                        if (ai != null && bi != null) stops.add(ai to bi)
                    }
                    "Shoot" -> if (targetRaw != null) {
                        val bi = idToIndex[targetRaw.trim()]
                        if (ai != null && bi != null) shots.add(ai to bi)
                    }
                    "DoubleShoot" -> if (targetRaw != null) {
                        targetRaw.split(";").map { it.trim() }.forEach { tid ->
                            val bi = idToIndex[tid]
                            if (ai != null && bi != null) shots.add(ai to bi)
                        }
                    }
                    "Bomb" -> ai?.let { shots.add(it to it) }
                }
            }

            startAnimation(reloads, stops, shots, shields, superShields)

            // remember this snapshot to avoid re-triggering
            lastResolvedChoices.value = choices.toMap()
        }
    }

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

                alivePlayers.forEachIndexed { j, player ->
                    val relativeIndex = (j - meIndex + count) % count
                    val angle = ((2 * Math.PI * relativeIndex / count) + Math.PI / 2) % (2 * Math.PI)
                    val radius = 140
                    val shieldRadius = 70
                    val x = (radius * cos(angle)).toFloat()
                    val y = (radius * sin(angle)).toFloat()
                    val shieldX = (shieldRadius * cos(angle)).toFloat()
                    val shieldY = (shieldRadius * sin(angle)).toFloat()

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
                        action = if (myChoice == null) if (player.id in selectedTargets) "Shoot" else ""
                                else if (player.id == meId) if (myChoice["target"] == null) myChoice["action"] as? String ?: "" else ""
                                else if (myChoice["target"] != null && player.id == myChoice["target"] as String) myChoice["action"] as? String ?: ""
                                else if (myChoice["target"] != null && "${ player.id };${ player.name }" == myChoice["target"] as String) "DoubleShoot"
                                else if (myChoice["target"] != null && (
                                    player.name == (myChoice["target"] as String).substringBefore(";") ||
                                    player.name == (myChoice["target"] as String).substringAfter(";"))) "Shoot"
                                else "",
                        blocked = isAnimating && j in showStopedPlayers,
                        isMe = player.id == meId
                        )

                    if (isAnimating && j in showShields) Image(
                        painter = painterResource(id = R.drawable.topsideshield),
                        contentDescription = "Description de l'image",
                        modifier = Modifier
                            .height(if (players.size <= 7) 20.dp else 15.dp)
                            .offset(shieldX.dp, shieldY.dp)
                            .align(Alignment.Center)
                            .rotate((angle * 180 / Math.PI).toFloat() - 90f),
                    ) else if (isAnimating && j in showSuperShields) Image(
                        painter = painterResource(id = R.drawable.topsidesupershield),
                        contentDescription = "Description de l'image",
                        modifier = Modifier
                            .height(if (players.size <= 7) 20.dp else 15.dp)
                            .offset(shieldX.dp, shieldY.dp)
                            .align(Alignment.Center)
                            .rotate((angle * 180 / Math.PI).toFloat() - 90f),
                    )
                    if (isAnimatingBullet || isAnimatingStops) {
                        if (curShotPair.first == j) {
                            val it = curShotPair.second
                            if (it != j) {
                                val angle2 = ((2 * Math.PI * it / count) + Math.PI / 2) % (2 * Math.PI)
                                // compute actual positions of shooter (x,y) and target (tx,ty) on same basis
                                val x2 = (radius * cos(angle2)).toFloat()
                                val y2 = (radius * sin(angle2)).toFloat()
                                // direction vector from shooter to target
                                val dx = x2 - x
                                val dy = y2 - y
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy) - bulletRadius
                                // angle toward target (atan2 uses y then x)
                                val bulletAngle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                                val bulletPos = bulletRadius + (bulletRelativePos.value / 100f) * dist
                                val bulletX = x + (bulletPos * cos(bulletAngle)).toFloat()
                                val bulletY = y + (bulletPos * sin(bulletAngle)).toFloat()
                                // rotation in degrees: adjust if sprite needs orientation fix (+/- 90)
                                val rotationDeg = (bulletAngle * 180 / Math.PI).toFloat()

                                Image(
                                    painter = painterResource(id = when {
                                        isAnimatingStops -> R.drawable.stop
                                        else -> R.drawable.bullet
                                    }),
                                    contentDescription = "Description de l'image",
                                    modifier = Modifier
                                        .height(when {
                                            isAnimatingStops -> 30.dp
                                            else -> 20.dp
                                        })
                                        .offset(bulletX.dp, bulletY.dp)
                                        .align(Alignment.Center)
                                        .rotate(rotationDeg + 90f),
                                    )
                            }
                        }
                    } else if (isAnimatingBomb) {
                        if (curBombThrower == j) {
                            val dist = kotlin.math.sqrt(x * x + y * y) - bulletRadius
                            // angle toward target (atan2 uses y then x)
                            val bulletAngle = kotlin.math.atan2((-y).toDouble(), (-x).toDouble())
                            val bulletPos = bulletRadius + (bulletRelativePos.value / 100f) * dist
                            val bulletX = x + (bulletPos * cos(bulletAngle)).toFloat()
                            val bulletY = y + (bulletPos * sin(bulletAngle)).toFloat()
                            // rotation in degrees: adjust if sprite needs orientation fix (+/- 90)
                            val rotationDeg = (bulletAngle * 180 / Math.PI).toFloat()

                            Image(
                                painter = painterResource(id = R.drawable.bombe),
                                contentDescription = "Description de l'image",
                                modifier = Modifier
                                    .height(30.dp)
                                    .offset(bulletX.dp, bulletY.dp)
                                    .align(Alignment.Center)
                                    .rotate(rotationDeg + 90f),
                                )
                            }
                        } else if (isAnimatingReload) {
                            if (curReloader == j) {
                                val bulletX = x + 15
                                val bulletY = y - 15 - bulletRelativePos.value/9

                                Image(
                                    painter = painterResource(id = R.drawable.reload),
                                    contentDescription = "Description de l'image",
                                    modifier = Modifier
                                        .height(30.dp)
                                        .offset(bulletX.dp, bulletY.dp)
                                        .align(Alignment.Center),
                                    )
                            }
                        }
                    }
                }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
            {
                if (!isAnimating){
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
                        if (targetMode == null) {
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
        //"p1" to mapOf("action" to "Protect"),
        "p2" to mapOf("action" to "Protect"),
        "p3" to mapOf("action" to "DoubleShoot", "target" to "p2;p4")
    )

    // Force light theme + disable dynamic colors so preview background is white
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        GameScreen(
            players = players,
            meId = "p1",
            choices = choices,
            onSubmit = { _, _ -> },
            onAnimationsCompleted = {}
        )
    }
}