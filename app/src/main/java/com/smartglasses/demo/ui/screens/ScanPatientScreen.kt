package com.smartglasses.demo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartglasses.demo.data.scanning.BarcodeAnalyzer
import com.smartglasses.demo.viewmodel.ScanViewModel
import java.util.concurrent.Executors

/**
 * ScanPatientScreen - REAL barcode scanning with CameraX and ML Kit
 * 
 * This implementation uses the REAL device/emulator camera:
 * - Shows actual camera preview from webcam
 * - Performs real-time barcode detection
 * - Displays actual scanned values
 */
@Composable
fun ScanPatientScreen(
    username: String,
    viewModel: ScanViewModel,
    onScanComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permission state
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

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !hasCameraPermission -> {
                PermissionDeniedView(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    viewModel = viewModel
                )
            }
            uiState.scannedValue != null -> {
                // Show scanned result
                val scannedValue = uiState.scannedValue
                if (scannedValue != null) {
                    ScannedResultView(
                        scannedValue = scannedValue,
                        barcodeFormat = uiState.barcodeFormat,
                        onScanAgain = { viewModel.scanAgain() },
                        onContinue = { onScanComplete() }
                    )
                }
            }
            else -> {
                // REAL Camera preview with scanning
                RealCameraPreviewWithOverlay(
                    viewModel = viewModel,
                    onCancel = onCancel
                )
            }
        }
    }
}

/**
 * REAL Camera Preview with scanning overlay
 * 
 * This composable:
 * 1. Creates a PreviewView for actual camera feed
 * 2. Binds CameraX use cases to the lifecycle
 * 3. Analyzes frames for barcodes in real-time with edge case handling
 * 4. Shows scanning UI overlay with status warnings
 */
@Composable
private fun RealCameraPreviewWithOverlay(
    viewModel: ScanViewModel,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // State to track camera initialization
    var cameraInitialized by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // REAL Camera Preview using AndroidView
        AndroidView(
            factory = { ctx ->
                Log.d("RealCamera", "=== Creating REAL Camera Preview ===")
                
                // Create the PreviewView that will display the actual camera feed
                val previewView = PreviewView(ctx).apply {
                    // PERFORMANCE mode for smoother preview on real device
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    // FILL_START ensures the preview fills the view
                    scaleType = PreviewView.ScaleType.FILL_START
                    // Set background to transparent to see the camera feed
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }

                // Get the CameraProvider
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        Log.d("RealCamera", "CameraProvider obtained successfully")

                        // Build the preview use case - this shows the actual camera feed
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                // Connect preview to the PreviewView's surface
                                it.surfaceProvider = previewView.surfaceProvider
                                Log.d("RealCamera", "Preview surface provider set")
                            }

                        // Build the image analysis use case - this analyzes frames for barcodes
                        val cameraExecutor = Executors.newSingleThreadExecutor()
                        
                        // Create enhanced analyzer with edge case detection
                        val analyzer = BarcodeAnalyzer(
                            onBarcodeDetected = { value, format ->
                                Log.d("RealCamera", "✓ Barcode detected: $value (format: $format)")
                                viewModel.onBarcodeDetected(value, format)
                            },
                            onError = { error ->
                                Log.e("RealCamera", "Barcode analysis error: ${error.message}")
                            },
                            onScanningStatusUpdate = { status ->
                                // Feed scanning status to ViewModel for UI updates
                                viewModel.updateScanningStatus(status)
                            }
                        )

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor, analyzer)
                                Log.d("RealCamera", "ImageAnalysis configured with enhanced analyzer")
                            }

                        // Select the back camera for real device
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        // Unbind any previous use cases
                        cameraProvider.unbindAll()
                        Log.d("RealCamera", "Previous use cases unbound")

                        // Bind the camera to the lifecycle with both preview and analysis
                        val camera: Camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        
                        // Enable auto-focus and torch if available (for real device)
                        camera.cameraControl.let { control ->
                            // Set focus mode to continuous auto-focus
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(
                                previewView.width / 2f,
                                previewView.height / 2f
                            )
                            val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                            control.startFocusAndMetering(action)
                            Log.d("RealCamera", "Auto-focus configured")
                        }
                        
                        Log.d("RealCamera", "=== Camera BOUND SUCCESSFULLY ===")
                        Log.d("RealCamera", "Camera info: ${camera.cameraInfo}")
                        Log.d("RealCamera", "PreviewView size: ${previewView.width}x${previewView.height}")
                        
                        cameraInitialized = true

                    } catch (exc: Exception) {
                        Log.e("RealCamera", "=== Camera binding FAILED ===", exc)
                        cameraError = exc.message
                        viewModel.onScanError("Camera error: ${exc.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                // Return the PreviewView - this will display the actual camera feed
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show error if camera failed with manual input fallback
        cameraError?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF330000)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera Error",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = error,
                            color = Color(0xFFFFAAAA),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera unavailable. Enter manually below.",
                            color = Color(0xFFFFA000),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Scanning UI Overlay with edge case warnings
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scan Barcode",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Scanning indicator
                if (uiState.isScanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF00B4D8),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning...",
                            color = Color(0xFF00B4D8),
                            fontSize = 12.sp
                        )
                    }
                }
                
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color(0xFFEF476F))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Edge case warning banners
            AnimatedVisibility(
                visible = uiState.isLowLight,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WarningBanner(
                    icon = Icons.Default.Warning,
                    message = "⚠ Low light detected - improve lighting",
                    backgroundColor = Color(0xFFFFA000)
                )
            }
            
            AnimatedVisibility(
                visible = uiState.showHoldSteadyHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WarningBanner(
                    icon = Icons.Default.CameraAlt,
                    message = "📱 Hold device steady",
                    backgroundColor = Color(0xFF2196F3)
                )
            }
            
            AnimatedVisibility(
                visible = uiState.showAlignBarcodeHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WarningBanner(
                    icon = Icons.Default.Fullscreen,
                    message = "📐 Align barcode within frame",
                    backgroundColor = Color(0xFF9C27B0)
                )
            }
            
            AnimatedVisibility(
                visible = uiState.detectedBarcodeCount > 1,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WarningBanner(
                    icon = Icons.Default.SelectAll,
                    message = "📊 ${uiState.detectedBarcodeCount} codes detected - selecting strongest",
                    backgroundColor = Color(0xFF00B4D8)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Scanning frame overlay (center of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Corner brackets to indicate scan area
                ScanningFrame()
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status message with icon
            uiState.scanningStatusMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            message.contains("✓") -> Color(0xFF00E5A0).copy(alpha = 0.2f)
                            message.contains("⚠") -> Color(0xFFFFA000).copy(alpha = 0.2f)
                            else -> Color(0xFF112240)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = message,
                        color = when {
                            message.contains("✓") -> Color(0xFF00E5A0)
                            message.contains("⚠") -> Color(0xFFFFA000)
                            else -> Color.White
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Retry button for no-detection timeout
            AnimatedVisibility(
                visible = uiState.showRetryButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = { viewModel.retryScanning() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETRY SCANNING",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            if (!uiState.showRetryButton) {
                Spacer(modifier = Modifier.height(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Input Section (always visible as fallback)
            ManualInputSection(viewModel = viewModel)
        }
    }
}

/**
 * Manual Input Section - Fallback when camera fails or timeout
 */
@Composable
private fun ManualInputSection(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var manualInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Toggle button to show/hide manual input
        TextButton(
            onClick = { showManualInput = !showManualInput },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (showManualInput) "Hide manual entry ↑" else "Unable to scan? Enter manually ↓",
                color = Color(0xFF90CAF9),
                fontSize = 14.sp
            )
        }

        AnimatedVisibility(
            visible = showManualInput,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF112240)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Show appropriate hint based on state
                    val hintText = when {
                        uiState.showRetryButton -> "No barcode detected. Try again or enter manually:"
                        uiState.isLowLight -> "Low light detected. Enter manually:"
                        else -> "Enter barcode / ID manually:"
                    }

                    Text(
                        text = hintText,
                        color = Color(0xFF90CAF9),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        label = { Text("Barcode / ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            focusedLabelColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color(0xFF3A506B),
                            unfocusedLabelColor = Color(0xFF90CAF9)
                        )
                    )

                    // Error message
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        uiState.errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.onManualBarcodeEntered(manualInput)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

/**
 * Warning banner for edge case feedback
 */
@Composable
private fun WarningBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    backgroundColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.9f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Scanning frame corners
 */
@Composable
private fun ScanningFrame() {
    val cornerColor = Color(0xFF00B4D8)
    val cornerSize = 40.dp
    val strokeWidth = 4.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(cornerColor)
            )
        }

        // Top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .background(cornerColor)
            )
        }

        // Bottom-left corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .align(Alignment.BottomStart)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(cornerColor)
            )
        }

        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .align(Alignment.BottomStart)
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .background(cornerColor)
            )
        }
    }
}

/**
 * Permission Denied View with Manual Input Fallback
 */
@Composable
private fun PermissionDeniedView(
    onRequestPermission: () -> Unit,
    viewModel: ScanViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var manualInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1628)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF112240)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF00B4D8),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera permission is needed to scan barcodes.",
                    color = Color(0xFF90CAF9),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                ) {
                    Text("Grant Permission")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider with text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color(0xFF3A506B))
                    Text(
                        text = " OR ",
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color(0xFF3A506B))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual input fallback
                Text(
                    text = "Camera permission denied. Enter manually:",
                    color = Color(0xFFFFA000),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Barcode / ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B4D8),
                        focusedLabelColor = Color(0xFF00B4D8),
                        unfocusedBorderColor = Color(0xFF3A506B),
                        unfocusedLabelColor = Color(0xFF90CAF9)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Error message for invalid input
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.onManualBarcodeEntered(manualInput)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

/**
 * Scanned Result View - Shows the barcode value after scanning
 */
@Composable
private fun ScannedResultView(
    scannedValue: String,
    barcodeFormat: String?,
    onScanAgain: () -> Unit,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1628))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Success icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF00E5A0),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Barcode Scanned!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Format: ${barcodeFormat ?: "Unknown"}",
                color = Color(0xFF90CAF9),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Result card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF112240)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scanned Value:",
                        color = Color(0xFF90CAF9),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scannedValue,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
            ) {
                Text(
                    text = "CONTINUE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SCAN AGAIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF00B4D8)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
