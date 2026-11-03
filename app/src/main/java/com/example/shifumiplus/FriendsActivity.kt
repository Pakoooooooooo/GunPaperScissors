package com.example.shifumiplus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.shifumiplus.ui.PlayerPP
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.Bronson
import com.example.shifumiplus.ui.theme.DarkGrey
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class FriendsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val playerId = intent.getStringExtra("player_id") ?: ""
        val playerName = intent.getStringExtra("player_name") ?: ""

        setContent {
            ShiFuMiPlusTheme {
                FriendsScreen(
                    playerId = playerId,
                    playerName = playerName,
                    onOpenScanner = {
                        startActivity(Intent(this, FriendsScannerActivity::class.java))
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun FriendsScreen(
    playerId: String,
    playerName: String,
    onOpenScanner: () -> Unit,
    onBack: () -> Unit
) {
    val foreground = MaterialTheme.colorScheme.secondary.toArgb()
    val background = BackgroundColor.toArgb()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var friends by remember(playerId) { mutableStateOf(emptyList<FriendProfile>()) }
    var editMode by remember(playerId) { mutableStateOf(false) }
    var selectedFriendIds by remember(playerId) { mutableStateOf(setOf<String>()) }
    val qrBitmap = remember(playerId, playerName) { generateQrBitmap(
FriendsStore.encodeCurrentUser(playerId, playerName),
foreground,
background
    ) }

    LaunchedEffect(playerId) {
friends = FriendsStore.fetchFriends(context, playerId)
    }

    DisposableEffect(lifecycleOwner, playerId) {
val listener = FriendsStore.listenToFriends(context, playerId) { updated ->
    friends = updated
}
val observer = LifecycleEventObserver { _, event ->
    if (event == Lifecycle.Event.ON_RESUME) {
        lifecycleOwner.lifecycleScope.launch {
            friends = FriendsStore.fetchFriends(context, playerId)
        }
    }
}
lifecycleOwner.lifecycle.addObserver(observer)
onDispose {
    listener.remove()
    lifecycleOwner.lifecycle.removeObserver(observer)
}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.height(64.dp))
            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "QR code ami",
                    modifier = Modifier
                        .width(220.dp)
                        .height(220.dp)
                        .border(
                            BorderStroke(15.dp, MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.background,
                            RoundedCornerShape(16.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("friend_code", FriendsStore.encodeCurrentUser(playerId, playerName))
                    clipboard.setPrimaryClip(clip)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                elevation = null
            ) {
                Image(
                    painter = painterResource(id = R.drawable.copy),
                    contentDescription = "Copier le code ami",
                    modifier = Modifier.height(25.dp)
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (editMode) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val idsToRemove = selectedFriendIds.toList()
                                if (idsToRemove.isNotEmpty()) {
                                    scope.launch {
                                        idsToRemove.forEach { friendId ->
                                            FriendsStore.removeMutualFriend(context, playerId, friendId)
                                        }
                                        friends = FriendsStore.fetchFriends(context, playerId)
                                        selectedFriendIds = emptySet()
                                        editMode = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("-")
                        }
                        Button(
                            onClick = {
                                editMode = false
                                selectedFriendIds = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("V")
                        }
                    }
                }
                Text(
                    text = "Amis",
                    fontSize = 28.sp,
                    fontFamily = Bronson,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (!editMode) {
                    IconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.plus),
                            contentDescription = "Ajouter un ami",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (friends.isEmpty()) {
                Text(
                    text = "Aucun ami pour le moment",
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = Bronson
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    friends.forEach { friend ->
                        val isSelected = friend.id in selectedFriendIds
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(DarkGrey)
                                .border(
                                    width = if (editMode && isSelected) 2.dp else 0.dp,
                                    color = if (editMode && isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                    shape = RoundedCornerShape(15.dp)
                                )
                                .combinedClickable(
                                    onLongClick = {
                                        editMode = true
                                        selectedFriendIds = if (friend.id in selectedFriendIds) selectedFriendIds - friend.id else selectedFriendIds + friend.id
                                    },
                                    onClick = {
                                        if (editMode) {
                                            selectedFriendIds = if (friend.id in selectedFriendIds) selectedFriendIds - friend.id else selectedFriendIds + friend.id
                                        }
                                    }
                                )
                                .padding(8.dp)
                        ) {
                            Row (
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically) {
                                PlayerPP(Modifier.fillMaxHeight())
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    friend.name,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    friend.id,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateQrBitmap(
    payload: String,
    foregroundColor: Int,
    backgroundColor: Int
): ImageBitmap? {
    return try {
        val width = 512
        val height = 512
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            width,
            height
        )
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Preview
@Composable
fun FriendsScreenPreview(){
    ShiFuMiPlusTheme {
        FriendsScreen(
            "IDIDIDID",
            "Name",
            {  },
            {  }
        )
    }
}