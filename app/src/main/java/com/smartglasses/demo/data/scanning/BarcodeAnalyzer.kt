package com.smartglasses.demo.data.scanning

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * BarcodeAnalyzer - Enhanced real-time barcode detection with edge case handling
 * 
 * Features:
 * - Poor lighting detection via image luminance analysis
 * - Motion/shakiness detection via frame comparison
 * - Failure tracking with debounce
 * - Multiple barcode filtering (strongest result)
 * - Timeout tracking for no-detection scenarios
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String, Int) -> Unit,
    private val onError: (Exception) -> Unit,
    private val onScanningStatusUpdate: (ScanningStatus) -> Unit = {}
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "BarcodeAnalyzer"
        
        // Edge case thresholds
        private const val LOW_LIGHT_THRESHOLD = 50 // Average pixel brightness (0-255)
        private const val MOTION_THRESHOLD = 1000000 // Frame difference threshold
        private const val FAILURE_DEBOUNCE_COUNT = 5 // Frames before warning
        private const val NO_DETECTION_TIMEOUT_MS = 10000L // 10 seconds
    }

    /**
     * Scanning status for UI feedback
     */
    data class ScanningStatus(
        val isLowLight: Boolean = false,
        val isShaky: Boolean = false,
        val failureCount: Int = 0,
        val timeSinceLastDetection: Long = 0L,
        val detectedBarcodeCount: Int = 0,
        val averageLuminance: Double = 0.0
    )

    // Barcode scanner configuration
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_ITF
            )
            .build()
    )
    
    private var isScanning = true
    private var frameCount = 0
    private var lastDetectionTime = System.currentTimeMillis()
    private var consecutiveFailures = 0
    private var lastFrameChecksum: Long = 0
    private var stableBarcodeValue: String? = null
    private var stableBarcodeFrames = 0
    private val STABILITY_THRESHOLD = 3 // Frames to confirm stable detection

    /**
     * Stop scanning to prevent multiple detections
     */
    fun stopScanning() {
        isScanning = false
        Log.d(TAG, "Scanning stopped")
    }

    /**
     * Resume scanning and reset state
     */
    fun startScanning() {
        isScanning = true
        frameCount = 0
        consecutiveFailures = 0
        lastDetectionTime = System.currentTimeMillis()
        stableBarcodeValue = null
        stableBarcodeFrames = 0
        Log.d(TAG, "Scanning started, state reset")
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            Log.w(TAG, "Media image is null")
            imageProxy.close()
            return
        }

        frameCount++
        val currentTime = System.currentTimeMillis()
        val timeSinceLastDetection = currentTime - lastDetectionTime
        
        // Analyze image quality (lighting, motion)
        val luminance = calculateAverageLuminance(mediaImage)
        val isLowLight = luminance < LOW_LIGHT_THRESHOLD
        val isShaky = detectMotion(mediaImage)
        
        // Log frame info every 30 frames (~1 second)
        if (frameCount % 30 == 0) {
            Log.d(TAG, "Frame #$frameCount | Size: ${mediaImage.width}x${mediaImage.height} | " +
                    "Luminance: ${"%.1f".format(luminance)} | LowLight: $isLowLight | Shaky: $isShaky")
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                handleDetectionResult(barcodes, luminance, isLowLight, isShaky, timeSinceLastDetection)
            }
            .addOnFailureListener { exception ->
                handleDetectionFailure(exception, luminance, isLowLight, isShaky, timeSinceLastDetection)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Handle successful barcode detection with stability filtering
     */
    private fun handleDetectionResult(
        barcodes: List<Barcode>,
        luminance: Double,
        isLowLight: Boolean,
        isShaky: Boolean,
        timeSinceLastDetection: Long
    ) {
        if (barcodes.isNotEmpty()) {
            // Reset failure count on success
            consecutiveFailures = 0
            lastDetectionTime = System.currentTimeMillis()
            
            // Find the best barcode (largest area = most prominent)
            val bestBarcode = barcodes.maxByOrNull { 
                val corners = it.cornerPoints
                if (corners != null && corners.size >= 4) {
                    // Calculate bounding box area
                    val width = corners[2].x - corners[0].x
                    val height = corners[2].y - corners[0].y
                    width * height
                } else 0
            } ?: barcodes.first()
            
            val value = bestBarcode.rawValue
            val format = bestBarcode.format
            
            Log.d(TAG, "Detected ${barcodes.size} barcodes, best: $value (format: $format)")
            
            if (!value.isNullOrBlank()) {
                // Stability debounce - require multiple consistent detections
                if (value == stableBarcodeValue) {
                    stableBarcodeFrames++
                    Log.d(TAG, "Barcode stable for $stableBarcodeFrames frames")
                    
                    if (stableBarcodeFrames >= STABILITY_THRESHOLD) {
                        Log.d(TAG, "✓ Barcode confirmed stable, triggering detection: $value")
                        stopScanning()
                        onBarcodeDetected(value, format)
                    }
                } else {
                    // New barcode detected, reset stability counter
                    stableBarcodeValue = value
                    stableBarcodeFrames = 1
                    Log.d(TAG, "New barcode candidate: $value (reset stability)")
                }
            }
            
            // Update status
            onScanningStatusUpdate(ScanningStatus(
                isLowLight = isLowLight,
                isShaky = isShaky,
                failureCount = 0,
                timeSinceLastDetection = 0,
                detectedBarcodeCount = barcodes.size,
                averageLuminance = luminance
            ))
        } else {
            // No barcodes in this frame
            stableBarcodeValue = null
            stableBarcodeFrames = 0
            
            onScanningStatusUpdate(ScanningStatus(
                isLowLight = isLowLight,
                isShaky = isShaky,
                failureCount = consecutiveFailures,
                timeSinceLastDetection = timeSinceLastDetection,
                detectedBarcodeCount = 0,
                averageLuminance = luminance
            ))
        }
    }

    /**
     * Handle detection failure with debounce tracking
     */
    private fun handleDetectionFailure(
        exception: Exception,
        luminance: Double,
        isLowLight: Boolean,
        isShaky: Boolean,
        timeSinceLastDetection: Long
    ) {
        consecutiveFailures++
        
        if (consecutiveFailures >= FAILURE_DEBOUNCE_COUNT) {
            Log.w(TAG, "ML Kit failure #$consecutiveFailures: ${exception.message}")
        }
        
        // Reset stable barcode on failure
        stableBarcodeValue = null
        stableBarcodeFrames = 0
        
        onScanningStatusUpdate(ScanningStatus(
            isLowLight = isLowLight,
            isShaky = isShaky,
            failureCount = consecutiveFailures,
            timeSinceLastDetection = timeSinceLastDetection,
            detectedBarcodeCount = 0,
            averageLuminance = luminance
        ))
        
        onError(exception)
    }

    /**
     * Calculate average luminance of YUV image (Y channel)
     */
    private fun calculateAverageLuminance(mediaImage: android.media.Image): Double {
        return try {
            val buffer = mediaImage.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            buffer.rewind()
            
            // Calculate average of Y channel (luminance)
            var sum = 0L
            val sampleSize = maxOf(1, bytes.size / 1000) // Sample every Nth pixel for performance
            var sampleCount = 0
            
            for (i in bytes.indices step sampleSize) {
                // Convert byte (signed) to unsigned int (0-255)
                val luminance = bytes[i].toInt() and 0xFF
                sum += luminance
                sampleCount++
            }
            
            if (sampleCount > 0) sum.toDouble() / sampleCount else 128.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to calculate luminance", e)
            128.0 // Default mid-brightness
        }
    }

    /**
     * Detect motion/shakiness by comparing frame checksums
     */
    private fun detectMotion(mediaImage: android.media.Image): Boolean {
        return try {
            val buffer = mediaImage.planes[0].buffer
            val bytes = ByteArray(minOf(buffer.remaining(), 10000)) // Sample first 10KB
            buffer.get(bytes)
            buffer.rewind()
            
            // Simple checksum of sampled pixels
            var checksum = 0L
            for (i in bytes.indices step 100) {
                checksum += (bytes[i].toInt() and 0xFF) * (i + 1)
            }
            
            val diff = kotlin.math.abs(checksum - lastFrameChecksum)
            lastFrameChecksum = checksum
            
            val isShaky = diff > MOTION_THRESHOLD
            if (isShaky && frameCount % 30 == 0) {
                Log.d(TAG, "Motion detected (diff: $diff)")
            }
            isShaky
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up scanner")
        scanner.close()
    }
}
