package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.LifeLinkViewModel
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

sealed class MapMarkerItem {
    data class Bank(val bank: BloodBank) : MapMarkerItem()
    data class Donor(val donor: DonorProfile) : MapMarkerItem()
}

@Composable
fun MapViewScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bloodBanks by viewModel.bloodBanks.collectAsState()
    val donors by viewModel.donors.collectAsState()

    var filterType by remember { mutableStateOf("ALL") } // ALL, BANKS, DONORS
    var selectedMarker by remember { mutableStateOf<MapMarkerItem?>(null) }

    // Prepare simulated relative screen offsets for markers
    val bankPositions = remember(bloodBanks) {
        listOf(
            Offset(0.25f, 0.35f),
            Offset(0.70f, 0.28f),
            Offset(0.35f, 0.72f),
            Offset(0.78f, 0.65f),
            Offset(0.18f, 0.55f)
        )
    }

    val donorPositions = remember(donors) {
        listOf(
            Offset(0.42f, 0.22f),
            Offset(0.58f, 0.38f),
            Offset(0.68f, 0.78f),
            Offset(0.28f, 0.82f),
            Offset(0.82f, 0.42f),
            Offset(0.38f, 0.48f)
        )
    }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Filter Bar
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("All Pins (${bloodBanks.size + donors.size})") }
                )
                FilterChip(
                    selected = filterType == "BANKS",
                    onClick = { filterType = "BANKS" },
                    label = { Text("Blood Banks (${bloodBanks.size})") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = BloodRedPrimary, modifier = Modifier.size(14.dp))
                    }
                )
                FilterChip(
                    selected = filterType == "DONORS",
                    onClick = { filterType = "DONORS" },
                    label = { Text("Donors (${donors.size})") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = VitalGreen, modifier = Modifier.size(14.dp))
                    }
                )
            }
        }

        // Radar / Interactive Map Canvas Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF161B22))
        ) {
            // Background Canvas: Grid, Rings, Sweeper
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Hit test nearest marker
                            val w = size.width
                            val h = size.height

                            var closestItem: MapMarkerItem? = null
                            var minDistance = 60f // touch radius in pixels

                            if (filterType == "ALL" || filterType == "BANKS") {
                                bloodBanks.forEachIndexed { i, bank ->
                                    val rel = bankPositions.getOrElse(i) { Offset(0.5f, 0.5f) }
                                    val target = Offset(rel.x * w, rel.y * h)
                                    val dist = (offset - target).getDistance()
                                    if (dist < minDistance) {
                                        minDistance = dist
                                        closestItem = MapMarkerItem.Bank(bank)
                                    }
                                }
                            }

                            if (filterType == "ALL" || filterType == "DONORS") {
                                donors.forEachIndexed { i, donor ->
                                    val rel = donorPositions.getOrElse(i) { Offset(0.5f, 0.5f) }
                                    val target = Offset(rel.x * w, rel.y * h)
                                    val dist = (offset - target).getDistance()
                                    if (dist < minDistance) {
                                        minDistance = dist
                                        closestItem = MapMarkerItem.Donor(donor)
                                    }
                                }
                            }

                            selectedMarker = closestItem
                        }
                    }
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = minOf(size.width, size.height) * 0.45f

                // Draw radar distance concentric rings
                listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                    drawCircle(
                        color = Color(0xFF30363D),
                        radius = maxRadius * fraction,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }

                // Center crosshairs
                drawLine(
                    color = Color(0xFF21262D),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF21262D),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1f
                )

                // Expanding pulse wave
                drawCircle(
                    color = BloodRedLight.copy(alpha = (1f - radarPulse) * 0.4f),
                    radius = maxRadius * radarPulse,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // User Location (Center)
                drawCircle(
                    color = BloodRedPrimary,
                    radius = 8f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = center
                )

                // Draw Blood Bank Markers
                if (filterType == "ALL" || filterType == "BANKS") {
                    bloodBanks.forEachIndexed { i, _ ->
                        val rel = bankPositions.getOrElse(i) { Offset(0.5f, 0.5f) }
                        val pos = Offset(rel.x * size.width, rel.y * size.height)

                        // Outer ring
                        drawCircle(
                            color = BloodRedLight.copy(alpha = 0.3f),
                            radius = 18f,
                            center = pos
                        )
                        drawCircle(
                            color = BloodRedPrimary,
                            radius = 12f,
                            center = pos
                        )
                        // White cross inside
                        drawLine(
                            color = Color.White,
                            start = Offset(pos.x - 5, pos.y),
                            end = Offset(pos.x + 5, pos.y),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(pos.x, pos.y - 5),
                            end = Offset(pos.x, pos.y + 5),
                            strokeWidth = 3f
                        )
                    }
                }

                // Draw Donor Markers
                if (filterType == "ALL" || filterType == "DONORS") {
                    donors.forEachIndexed { i, _ ->
                        val rel = donorPositions.getOrElse(i) { Offset(0.5f, 0.5f) }
                        val pos = Offset(rel.x * size.width, rel.y * size.height)

                        drawCircle(
                            color = VitalGreen.copy(alpha = 0.3f),
                            radius = 16f,
                            center = pos
                        )
                        drawCircle(
                            color = VitalGreen,
                            radius = 10f,
                            center = pos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = pos
                        )
                    }
                }
            }

            // Radar Legend Overlay
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xCC0D1117),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BloodRedPrimary))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Blood Banks", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(VitalGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ready Donors", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("You (Center)", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // Tip Banner at bottom of canvas
            if (selectedMarker == null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD0D1117),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Tap any pin on the radar map to view details",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Selected Pin Details Card
        AnimatedVisibility(visible = selectedMarker != null) {
            when (val item = selectedMarker) {
                is MapMarkerItem.Bank -> {
                    BankMapDetailCard(
                        bank = item.bank,
                        onClose = { selectedMarker = null },
                        onCall = { viewModel.launchPhoneDialer(context, item.bank.phone) },
                        onDirections = { viewModel.launchDirections(context, item.bank.latitude, item.bank.longitude, item.bank.name) }
                    )
                }
                is MapMarkerItem.Donor -> {
                    DonorMapDetailCard(
                        donor = item.donor,
                        onClose = { selectedMarker = null },
                        onCall = { viewModel.launchPhoneDialer(context, item.donor.phone) }
                    )
                }
                null -> {}
            }
        }
    }
}

@Composable
fun BankMapDetailCard(
    bank: BloodBank,
    onClose: () -> Unit,
    onCall: () -> Unit,
    onDirections: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(bank.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${bank.distanceKm} km away • ${bank.city}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Bank")
                }

                OutlinedButton(
                    onClick = onDirections,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions")
                }
            }
        }
    }
}

@Composable
fun DonorMapDetailCard(
    donor: DonorProfile,
    onClose: () -> Unit,
    onCall: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodGroupBadge(bloodGroup = donor.bloodGroup, isLarge = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(donor.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${donor.distanceKm} km away • ${donor.totalDonations} donations", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(containerColor = VitalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Contact Donor (Emergency)")
            }
        }
    }
}
