package com.example.shifumiplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.shifumiplus.FriendsStore
import com.example.shifumiplus.GameInvite
import com.example.shifumiplus.data.FirestoreRepository
import com.example.shifumiplus.ui.LoadingScreen
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.example.shifumiplus.ui.theme.Bronson
import com.example.shifumiplus.ui.theme.InvitationButton
import com.example.shifumiplus.ui.theme.MenuButton
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme

@Composable
fun JoinScreen(
    playerId: String,
    onJoin: (String) -> Unit,
    onJoinInvite: (gameId: String, inviteId: String) -> Unit
) {
    val context = LocalContext.current
    var id by remember { mutableStateOf("") }
    var invites by remember(playerId) { mutableStateOf(emptyList<GameInvite>()) }
    var isCheckingInvites by remember(playerId) { mutableStateOf(playerId.isNotBlank()) }
    var listenForInvites by remember(playerId) { mutableStateOf(playerId.isBlank()) }

    LaunchedEffect(playerId) {
        if (playerId.isNotBlank()) {
            try {
                val currentInvites = FriendsStore.fetchGameInvites(playerId)
                val validInvites = mutableListOf<GameInvite>()
                currentInvites.forEach { invite ->
                    when (FirestoreRepository.gameExists(invite.gameId)) {
                        false -> {
                            try {
                                FriendsStore.deleteGameInvite(playerId, invite.id)
                            } catch (_: Exception) {
                                validInvites += invite
                                Toast.makeText(
                                    context,
                                    "Could not remove an expired invitation.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        true, null -> validInvites += invite
                    }
                }
                invites = validInvites
            } catch (_: Exception) {
                Toast.makeText(context, "Could not check invitations.", Toast.LENGTH_SHORT).show()
            } finally {
                isCheckingInvites = false
                listenForInvites = true
            }
        }
    }

    DisposableEffect(playerId, listenForInvites) {
        val listener = if (playerId.isBlank() || !listenForInvites) {
            null
        } else {
            FriendsStore.listenToGameInvites(
                playerId = playerId,
                onUpdate = { updated -> invites = updated },
                onError = {
                    Toast.makeText(context, "Could not load invitations.", Toast.LENGTH_SHORT).show()
                }
            )
        }
        onDispose { listener?.remove() }
    }

    if (isCheckingInvites) {
        LoadingScreen(text = "Boom...")
        return
    }

    Box(
        modifier = Modifier
            .background(BackgroundColor)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(70.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Entrer l'ID du Jeu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Bronson,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(5.dp))
            OutlinedTextField(
                value = id,
                onValueChange = { if (it.length <= 6) id = it.uppercase() },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrect = false
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = Bronson,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
            MenuButton(
                "Rejoindre",
                { if (id.isNotBlank()) onJoin(id.trim()) },
                id.length == 6
            )
            if (invites.isNotEmpty()) {
                Text(
                    text = "Invitations",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Start,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Bronson,
                    color = MaterialTheme.colorScheme.secondary
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    invites.forEach { invite ->
                        InvitationButton(
                            text = "${invite.senderName} veut jouer !",
                            onPress = { onJoinInvite(invite.gameId, invite.id) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JoinScreenPreview() {
    ShiFuMiPlusTheme(darkTheme = false, dynamicColor = false) {
        JoinScreen(
            playerId = "",
            onJoin = {},
            onJoinInvite = { _, _ -> }
        )
    }
}
