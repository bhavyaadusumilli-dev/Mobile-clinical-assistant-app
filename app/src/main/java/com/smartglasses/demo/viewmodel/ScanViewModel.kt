package com.smartglasses.demo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.smartglasses.demo.data.scanning.BarcodeAnalyzer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScanUiState - Enhanced immutable state class with edge case handling
 */
data class ScanUiState(
    val isCameraActive: Boolean = false,
    val scannedValue: String? = null,
    val barcodeFormat: String? = null,
    val isScanning: Boolean = true,
    val errorMessage: String? = null,
    // Edge case status
    val isLowLight: Boolean = false,
    val isShaky: Boolean = false,
    val showHoldSteadyHint: Boolean = false,
    val showAlignBarcodeHint: Boolean = false,
    val showRetryButton: Boolean = false,
    val averageLuminance: Double = 0.0,
    val detectedBarcodeCount: Int = 0,
    val scanningStatusMessage: String? = null
)

/**
 * ScanViewModel - Enhanced barcode scanning with real-world edge case handling
 * 
 * Features:
 * - Poor lighting detection and warnings
 * - Motion/shakiness detection
 * - No-detection timeout with retry
 * - Multiple barcode filtering feedback
 * - Real device optimized
 */
class ScanViewModel : ViewModel() {

    companion object {
        private const val TAG = "ScanViewModel"
        private const val NO_DETECTION_TIMEOUT_MS = 8000L // 8 seconds before showing retry
        private const val LOW_LIGHT_LUMINANCE_THRESHOLD = 50.0
        private const val STEADY_HINT_FAILURE_THRESHOLD = 8 // Consecutive failures before hint
    }

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var scanningStartTime: Long = 0
    private var timeoutJob: Job? = null
    private var consecutiveFailures = 0

    /**
     * Initialize camera and start scanning with timeout monitoring
     */
    fun initializeScanning() {
        Log.d(TAG, "Initializing scanning with timeout monitoring")
        scanningStartTime = System.currentTimeMillis()
        consecutiveFailures = 0
        
        _uiState.value = ScanUiState(
            isCameraActive = true,
            isScanning = true,
            scannedValue = null,
            barcodeFormat = null,
            errorMessage = null,
            isLowLight = false,
            isShaky = false,
            showHoldSteadyHint = false,
            showAlignBarcodeHint = false,
            showRetryButton = false,
            scanningStatusMessage = "Point camera at barcode"
        )

        // Start timeout monitoring
        startTimeoutMonitoring()
    }

    /**
     * Start coroutine to monitor for no-detection timeout
     */
    private fun startTimeoutMonitoring() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(NO_DETECTION_TIMEOUT_MS)
            // If still scanning after timeout, show retry button
            if (_uiState.value.isScanning && _uiState.value.scannedValue == null) {
                Log.d(TAG, "No barcode detected within $NO_DETECTION_TIMEOUT_MS ms, showing retry")
                _uiState.value = _uiState.value.copy(
                    showRetryButton = true,
                    scanningStatusMessage = "No barcode detected. Try again or enter manually."
                )
            }
        }
    }

    /**
     * Update scanning status from analyzer feedback
     */
    fun updateScanningStatus(status: BarcodeAnalyzer.ScanningStatus) {
        if (!_uiState.value.isScanning) return

        val isLowLight = status.averageLuminance < LOW_LIGHT_LUMINANCE_THRESHOLD
        
        // Track consecutive failures for steady hint
        if (status.failureCount > 0) {
            consecutiveFailures = status.failureCount
        } else {
            consecutiveFailures = 0
        }
        
        val showHoldSteadyHint = status.isShaky || consecutiveFailures >= STEADY_HINT_FAILURE_THRESHOLD
        val showAlignBarcodeHint = consecutiveFailures >= STEADY_HINT_FAILURE_THRESHOLD && !status.isShaky

        // Build status message based on conditions
        val statusMessage = when {
            isLowLight -> "⚠ Low light - improve lighting"
            showHoldSteadyHint -> "📱 Hold device steady"
            showAlignBarcodeHint -> "📐 Align barcode within frame"
            status.detectedBarcodeCount > 1 -> "📊 ${status.detectedBarcodeCount} codes detected"
            status.detectedBarcodeCount == 1 -> "⌛ Processing..."
            else -> "Point camera at barcode"
        }

        // Only update if state changed (reduce recompositions)
        val currentState = _uiState.value
        if (currentState.isLowLight != isLowLight ||
            currentState.isShaky != status.isShaky ||
            currentState.showHoldSteadyHint != showHoldSteadyHint ||
            currentState.showAlignBarcodeHint != showAlignBarcodeHint ||
            currentState.detectedBarcodeCount != status.detectedBarcodeCount ||
            currentState.scanningStatusMessage != statusMessage
        ) {
            _uiState.value = currentState.copy(
                isLowLight = isLowLight,
                isShaky = status.isShaky,
                showHoldSteadyHint = showHoldSteadyHint,
                showAlignBarcodeHint = showAlignBarcodeHint,
                averageLuminance = status.averageLuminance,
                detectedBarcodeCount = status.detectedBarcodeCount,
                scanningStatusMessage = statusMessage
            )
            
            Log.d(TAG, "Status updated: lowLight=$isLowLight, shaky=${status.isShaky}, " +
                    "luminance=${"%.1f".format(status.averageLuminance)}, barcodes=${status.detectedBarcodeCount}")
        }
    }

    /**
     * Called when a barcode is successfully detected
     */
    fun onBarcodeDetected(value: String, format: Int) {
        Log.d(TAG, "Barcode detected: $value")
        timeoutJob?.cancel()
        
        val formatName = when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_ITF -> "ITF"
            else -> "Unknown"
        }

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            scannedValue = value,
            barcodeFormat = formatName,
            showRetryButton = false,
            scanningStatusMessage = "✓ Barcode detected!"
        )
    }

    /**
     * Handle scanning errors
     */
    fun onScanError(error: String) {
        Log.e(TAG, "Scan error: $error")
        _uiState.value = _uiState.value.copy(
            errorMessage = error,
            isScanning = false,
            showRetryButton = true
        )
    }

    /**
     * Manual retry when no barcode detected
     */
    fun retryScanning() {
        Log.d(TAG, "Manual retry requested")
        scanAgain()
    }

    /**
     * Reset to scan again
     */
    fun scanAgain() {
        timeoutJob?.cancel()
        initializeScanning()
    }

    /**
     * Stop camera when leaving screen
     */
    fun stopCamera() {
        Log.d(TAG, "Stopping camera")
        timeoutJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isCameraActive = false,
            isScanning = false
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Handle manual barcode input from user
     * Reuses same logic as camera scan but marks format as "Manual Entry"
     */
    fun onManualBarcodeEntered(value: String) {
        if (value.isBlank()) {
            Log.w(TAG, "Manual entry rejected: empty value")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a valid value"
            )
            return
        }

        Log.d(TAG, "Manual barcode entered: $value")
        timeoutJob?.cancel()

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            scannedValue = value.trim(),
            barcodeFormat = "Manual Entry",
            showRetryButton = false,
            errorMessage = null,
            scanningStatusMessage = "✓ Value entered manually"
        )
    }

    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
    }

    /**
     * Simple factory - no dependencies needed
     */
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
                return ScanViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
