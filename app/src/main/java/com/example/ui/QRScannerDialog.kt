package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class BarcodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var isScanned = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isScanned) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { rawValue ->
                            isScanned = true
                            onQrCodeScanned(rawValue)
                            return@addOnSuccessListener
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore errors
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun QRScannerDialog(
    onDismissRequest: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0C1017)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header (Top bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Escanear QR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Iluminación de flash automática activada", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build, // Representing flash option in default icons
                            contentDescription = "Flash",
                            tint = Color.White
                        )
                    }
                }

                // Subtitle Instruction
                Text(
                    text = "Escanea el código QR de Allsender\npara conectar automáticamente tu gateway.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = Color(0xFFCBCFDC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // Finder area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (hasCameraPermission) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CameraPreview(
                                onQrCodeScanned = { rawVal ->
                                    onQrCodeScanned(rawVal)
                                }
                            )
                            ScannerOverlay()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFFC19754),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Permiso de Cámara Requerido",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Para poder escanear el código QR ofrecido por Allsender debemos utilizar la cámara de este dispositivo celular.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC19754))
                            ) {
                                Text("Otorgar permiso", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Info check bar below camera viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131A26), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, // Representing shield protection check
                            contentDescription = null,
                            tint = Color(0xFFC19754),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "La conexión se realizará automáticamente al escanear.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }
                }

                // Help bulb action
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Soporte: Abre la consola web en tu ordenador, ve a SMS Gateways y haz click en 'Conectar Dispositivo' para generar el código QR.", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, // Representing help/bulb
                            contentDescription = "Ayuda",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "¿Necesitas ayuda?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBCFDC)
                        ),
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Soporte: Abre la consola web en tu ordenador, ve a SMS Gateways y haz click en 'Conectar Dispositivo' para generar el código QR.", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isTerminated by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { qrStr ->
                            if (!isTerminated) {
                                isTerminated = true
                                onQrCodeScanned(qrStr)
                            }
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            isTerminated = true
            cameraExecutor.shutdown()
        }
    }
}

@Composable
fun ScannerOverlay() {
    val goldColor = Color(0xFFC19754)
    val scrimColor = Color.Black.copy(alpha = 0.65f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val viewportWidth = width * 0.72f
        val viewportHeight = viewportWidth
        val left = (width - viewportWidth) / 2
        val top = (height - viewportHeight) / 2

        // 1. Draw scrims
        drawRect(color = scrimColor, topLeft = Offset(0f, 0f), size = Size(width, top))
        drawRect(color = scrimColor, topLeft = Offset(0f, top + viewportHeight), size = Size(width, height - (top + viewportHeight)))
        drawRect(color = scrimColor, topLeft = Offset(0f, top), size = Size(left, viewportHeight))
        drawRect(color = scrimColor, topLeft = Offset(left + viewportWidth, top), size = Size(width - (left + viewportWidth), viewportHeight))

        // 2. Draw golden corners (4 corners)
        val lineThickness = 12f
        val lineLength = 70f

        // Top Left Corner
        drawRect(color = goldColor, topLeft = Offset(left - 4f, top - 4f), size = Size(lineLength, lineThickness))
        drawRect(color = goldColor, topLeft = Offset(left - 4f, top - 4f), size = Size(lineThickness, lineLength))

        // Top Right Corner
        drawRect(color = goldColor, topLeft = Offset(left + viewportWidth - lineLength + 4f, top - 4f), size = Size(lineLength, lineThickness))
        drawRect(color = goldColor, topLeft = Offset(left + viewportWidth - lineThickness + 4f, top - 4f), size = Size(lineThickness, lineLength))

        // Bottom Left Corner
        drawRect(color = goldColor, topLeft = Offset(left - 4f, top + viewportHeight - lineThickness + 4f), size = Size(lineLength, lineThickness))
        drawRect(color = goldColor, topLeft = Offset(left - 4f, top + viewportHeight - lineLength + 4f), size = Size(lineThickness, lineLength))

        // Bottom Right Corner
        drawRect(color = goldColor, topLeft = Offset(left + viewportWidth - lineLength + 4f, top + viewportHeight - lineThickness + 4f), size = Size(lineLength, lineThickness))
        drawRect(color = goldColor, topLeft = Offset(left + viewportWidth - lineThickness + 4f, top + viewportHeight - lineLength + 4f), size = Size(lineThickness, lineLength))

        // 3. Draw scanning laser line
        drawRect(
            color = goldColor.copy(alpha = 0.85f),
            topLeft = Offset(left + 15f, top + (viewportHeight * 0.48f)),
            size = Size(viewportWidth - 30f, 6f)
        )
    }
}
