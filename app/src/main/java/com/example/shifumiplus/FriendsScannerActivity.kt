package com.example.shifumiplus

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import com.example.shifumiplus.ui.theme.BackgroundColor
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.shifumiplus.ui.PlayerPP
import com.example.shifumiplus.ui.theme.DarkGrey
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions

class FriendsScannerActivity : ComponentActivity() {
    private val requestedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            finish()
        }
    }

    private var cameraPreviewView: PreviewView? = null
    private var scannedProfile by mutableStateOf<FriendProfile?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ShiFuMiPlusTheme {
                FriendsScannerScreen(
                    scannedProfile = scannedProfile,
                    onPreviewReady = { previewView ->
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            startCamera(previewView)
                        }
                    },
                    onManualLookup = { rawId ->
                        val candidate = rawId.trim()
                        val parts = candidate.split("|", limit = 2)
                        if (parts.size == 2 && parts[0].length == 8 && parts[0].all { it.isLetterOrDigit() } && parts[1].isNotBlank()) {
                            scannedProfile = FriendProfile(parts[0], parts[1])
                        }
                    },
                    onAccept = {
                        val profile = scannedProfile ?: return@FriendsScannerScreen
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        val meId = prefs.getString("player_id", null) ?: return@FriendsScannerScreen
                        val meName = prefs.getString("player_name", null) ?: return@FriendsScannerScreen
                        lifecycleScope.launch {
                            FriendsStore.addMutualFriend(this@FriendsScannerActivity, FriendProfile(meId, meName), profile)
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestedPermission.launch(Manifest.permission.CAMERA)
        }
    }

    // gris = 255 - rouge, appliqué aux 3 canaux
    private val redInvertFilter = ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                -1f, 0f, 0f, 0f, 255f,
                -1f, 0f, 0f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )

    private val invertPaint = Paint().apply { colorFilter = redInvertFilter }

    private fun redOnBlackToBlackOnWhite(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(source, 0f, 0f, invertPaint)
        return result
    }

    private fun startCamera(previewView: PreviewView) {
        cameraPreviewView = previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // on a besoin des canaux RGB
                .build()

            val executor = Executors.newSingleThreadExecutor()
            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )

            analysis.setAnalyzer(executor) { imageProxy ->
                try {
                    val prepared = redOnBlackToBlackOnWhite(imageProxy.toBitmap())
                    val image = InputImage.fromBitmap(prepared, imageProxy.imageInfo.rotationDegrees)

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val code = barcodes.firstNotNullOfOrNull { it.rawValue }.orEmpty()
                            if (code.isNotBlank()) {
                                FriendProfile.decode(code)?.let { scannedProfile = it }
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                } catch (e: Exception) {
                    imageProxy.close()
                }
            }

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }
}

@Composable
fun FriendsScannerScreen(
    scannedProfile: FriendProfile?,
    onPreviewReady: (PreviewView) -> Unit,
    onManualLookup: (String) -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    var manualId by remember { mutableStateOf("") }
    val isManualIdValid = run {
        val raw = manualId.trim()
        val parts = raw.split("|", limit = 2)
        parts.size == 2 && parts[0].length == 8 && parts[0].all { it.isLetterOrDigit() } && parts[1].isNotBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    onPreviewReady(this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Scannez un QR code",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = manualId,
                onValueChange = { manualId = it },
                singleLine = true,
                placeholder = { Text("IDIDIDID|namenamenamename") },
                modifier = Modifier.fillMaxSize(0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isManualIdValid) {
                        onManualLookup(manualId)
                    }
                },
                enabled = isManualIdValid,
                colors = ButtonDefaults.buttonColors(containerColor = DarkGrey)
            ) {
                Text(
                    text = "OK",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (scannedProfile != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        PlayerPP(Modifier.size(40.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = scannedProfile.name,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "  ID : ${scannedProfile.id}",
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGrey),
                        ) {
                            Text(
                                text = "Valider",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGrey),
                        ) {
                            Text(
                                text = "Annuler",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun FriendsScannerScreenPreview() {
    ShiFuMiPlusTheme {
        FriendsScannerScreen(
            FriendProfile(
                "IDIDIDID",
                "Name"
            ),
            {},
            {},
            {},
            {}
        )
    }
}