package com.smartglasses.demo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartglasses.demo.data.Patient
import com.smartglasses.demo.viewmodel.PatientUiState
import com.smartglasses.demo.viewmodel.PatientViewModel

// ── Colour palette ────────────────────────────────────────────────────────
private val BackgroundDark = Color(0xFF060D1B)
private val SurfaceCard = Color(0xFF0E1F38)
private val Accent = Color(0xFF00B4D8)
private val AccentSecondary = Color(0xFF0077B6)
private val TextPrimary = Color(0xFFF0F6FF)
private val TextSecondary = Color(0xFF7FA8CC)
private val VitalGreen = Color(0xFF00E5A0)
private val VitalAmber = Color(0xFFFFD166)
private val VitalRed = Color(0xFFEF476F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    username: String,
    viewModel: PatientViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.MedicalServices,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Smart Glasses Assistant",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Logout", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A1628)
                )
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "DashboardContent"
            ) { state ->
                when {
                    state.isLoading -> LoadingContent()
                    state.errorMessage != null -> ErrorContent(
                        message = state.errorMessage,
                        onRetry = { viewModel.retry(username) }
                    )
                    state.patient != null -> SuccessContent(
                        username = username,
                        patient = state.patient
                    )
                    else -> LoadingContent()
                }
            }
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────
@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Accent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Loading patient data…",
                color = TextSecondary,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = VitalRed, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, color = TextPrimary, textAlign = TextAlign.Center, fontSize = 15.sp)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Success ───────────────────────────────────────────────────────────────
@Composable
private fun SuccessContent(username: String, patient: Patient) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome header
        WelcomeHeader(username)

        // Patient info card
        PatientInfoCard(patient)

        // Vitals card
        VitalsCard(patient)

        Spacer(Modifier.height(8.dp))
    }
}

// ── Welcome header ────────────────────────────────────────────────────────
@Composable
private fun WelcomeHeader(username: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Accent, AccentSecondary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column {
            Text(
                text = "Welcome back,",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Text(
                text = "Dr. $username",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Patient info card ─────────────────────────────────────────────────────
@Composable
private fun PatientInfoCard(patient: Patient) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Card header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PersonPin, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Patient Record", color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text(patient.name, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1A3050), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // Info grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PatientInfoItem(label = "Age", value = "${patient.age} yrs")
                PatientInfoItem(label = "Gender", value = patient.gender)
                PatientInfoItem(label = "Blood Type", value = patient.bloodType)
            }

            Spacer(Modifier.height(16.dp))

            // Diagnosis pill
            DiagnosisSection(patient.diagnosis)
        }
    }
}

@Composable
private fun PatientInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 11.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DiagnosisSection(diagnosis: String) {
    Column {
        Text("DIAGNOSIS", color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A2744))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(diagnosis, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Vitals card ───────────────────────────────────────────────────────────
@Composable
private fun VitalsCard(patient: Patient) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = VitalRed, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Live Vitals", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                // Live badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1B3A1A))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(VitalGreen)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("LIVE", color = VitalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1A3050), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalRow(
                    icon = Icons.Filled.Favorite,
                    label = "Heart Rate",
                    value = patient.heartRate,
                    tint = VitalRed,
                    background = Color(0xFF2D1212)
                )
                VitalRow(
                    icon = Icons.Filled.WaterDrop,
                    label = "Blood Pressure",
                    value = patient.bloodPressure,
                    tint = VitalAmber,
                    background = Color(0xFF2D2412)
                )
                VitalRow(
                    icon = Icons.Filled.DeviceThermostat,
                    label = "Temperature",
                    value = patient.temperature,
                    tint = VitalGreen,
                    background = Color(0xFF0D2D1F)
                )
            }
        }
    }
}

@Composable
private fun VitalRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, color = TextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
                Text(value, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

