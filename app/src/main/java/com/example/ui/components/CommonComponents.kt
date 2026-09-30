package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.LifeLinkViewModel
import com.example.ui.ScreenRoute
import com.example.ui.theme.*
import com.example.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeLinkTopBar(
    viewModel: LifeLinkViewModel,
    onMenuClick: () -> Unit = {}
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.navigateTo(ScreenRoute.HOME) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(BloodRedLight, BloodRedPrimary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "LifeLink Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Strings.get("app_title", currentLang),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Live Emergency beacon dot
                            LivePulsingDot()
                        }
                        Text(
                            text = Strings.get("app_subtitle", currentLang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                // Language Switcher Chip
                FilledTonalButton(
                    onClick = {
                        val nextLang = if (currentLang == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
                        viewModel.setLanguage(nextLang)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("language_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Switch Language",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentLang == AppLanguage.ENGLISH) "HI" else "EN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Profile / Auth Button
                IconButton(
                    onClick = {
                        if (currentUser != null) {
                            when (currentUser?.role) {
                                UserRole.DONOR -> viewModel.navigateTo(ScreenRoute.DONOR_DASHBOARD)
                                UserRole.ADMIN -> viewModel.navigateTo(ScreenRoute.ADMIN_PANEL)
                                else -> viewModel.navigateTo(ScreenRoute.AUTH)
                            }
                        } else {
                            viewModel.navigateTo(ScreenRoute.AUTH)
                        }
                    },
                    modifier = Modifier.testTag("profile_top_button")
                ) {
                    if (currentUser != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BloodRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "User Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun LivePulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(BloodRedLight)
    )
}

@Composable
fun LifeLinkBottomBar(
    currentScreen: ScreenRoute,
    onNavigate: (ScreenRoute) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.HOME,
            onClick = { onNavigate(ScreenRoute.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp, maxLines = 1) },
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.BLOOD_SEARCH,
            onClick = { onNavigate(ScreenRoute.BLOOD_SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Availability") },
            label = { Text("Banks", fontSize = 11.sp, maxLines = 1) },
            modifier = Modifier.testTag("nav_search")
        )
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.EMERGENCY_REQUESTS,
            onClick = { onNavigate(ScreenRoute.EMERGENCY_REQUESTS) },
            icon = { Icon(Icons.Default.Campaign, contentDescription = "Requests") },
            label = { Text("Requests", fontSize = 11.sp, maxLines = 1) },
            modifier = Modifier.testTag("nav_requests")
        )
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.DONOR_FINDER,
            onClick = { onNavigate(ScreenRoute.DONOR_FINDER) },
            icon = { Icon(Icons.Default.People, contentDescription = "Find Donors") },
            label = { Text("Donors", fontSize = 11.sp, maxLines = 1) },
            modifier = Modifier.testTag("nav_donors")
        )
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.MAP_RADAR ||
                    currentScreen == ScreenRoute.DONOR_DASHBOARD ||
                    currentScreen == ScreenRoute.ADMIN_PANEL ||
                    currentScreen == ScreenRoute.EDUCATION ||
                    currentScreen == ScreenRoute.AUTH,
            onClick = {
                // Toggle between map or education
                if (currentScreen != ScreenRoute.MAP_RADAR) {
                    onNavigate(ScreenRoute.MAP_RADAR)
                } else {
                    onNavigate(ScreenRoute.EDUCATION)
                }
            },
            icon = { Icon(Icons.Default.Explore, contentDescription = "Radar & More") },
            label = { Text("Radar/More", fontSize = 11.sp, maxLines = 1) },
            modifier = Modifier.testTag("nav_more")
        )
    }
}

@Composable
fun BloodGroupBadge(
    bloodGroup: BloodGroup,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val size = if (isLarge) 48.dp else 36.dp
    val fontSize = if (isLarge) 16.sp else 12.sp

    Surface(
        shape = CircleShape,
        color = BloodRedContainer,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BloodRedPrimary),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = bloodGroup.label,
                fontWeight = FontWeight.Black,
                color = BloodRedPrimary,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun UrgencyBadge(urgency: UrgencyLevel) {
    val (bgColor, textColor, label) = when (urgency) {
        UrgencyLevel.CRITICAL -> Triple(Color(0xFFFFEBEE), BloodRedPrimary, "CRITICAL")
        UrgencyLevel.WITHIN_24H -> Triple(Color(0xFFFFF3E0), EmergencyOrange, "24 HOURS")
        UrgencyLevel.PLANNED -> Triple(Color(0xFFE8F5E9), VitalGreen, "PLANNED")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (urgency == UrgencyLevel.CRITICAL) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BloodRedPrimary)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun NotificationBannerView(
    notification: com.example.ui.UiNotification,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (notification.isEmergency) BloodRedPrimary else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (notification.isEmergency) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (notification.isEmergency) Color.White else VitalGreen,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontWeight = FontWeight.Bold,
                    color = if (notification.isEmergency) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = if (notification.isEmergency) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = if (notification.isEmergency) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SosEmergencyDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (patientName: String, bloodGroup: BloodGroup, units: Int, hospital: String, city: String, phone: String) -> Unit
) {
    if (!isOpen) return

    var patientName by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(BloodGroup.O_NEG) }
    var unitsNeeded by remember { mutableIntStateOf(2) }
    var hospital by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Delhi") }
    var contactPhone by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BloodRedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "1-Tap SOS Emergency Broadcast",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = BloodRedPrimary
                    )
                    Text(
                        text = "Minimal steps. Broadcasts to all nearby donors instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_patient_name")
                )

                // Blood Group Selector
                Text("Select Required Blood Group:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(BloodGroup.O_NEG, BloodGroup.O_POS, BloodGroup.A_POS, BloodGroup.B_POS).forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group.label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(BloodGroup.A_NEG, BloodGroup.B_NEG, BloodGroup.AB_POS, BloodGroup.AB_NEG).forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group.label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Units Needed Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Units Needed:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { if (unitsNeeded > 1) unitsNeeded-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$unitsNeeded Units",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { if (unitsNeeded < 10) unitsNeeded++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Hospital / Emergency Ward") },
                    placeholder = { Text("e.g. AIIMS Trauma Center") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_hospital")
                )

                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Emergency Contact Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_phone")
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contactPhone.filter { it.isDigit() }.length < 10) {
                        errorText = "Please enter a valid 10-digit emergency contact phone."
                        return@Button
                    }
                    errorText = null
                    onSubmit(patientName, selectedGroup, unitsNeeded, hospital, city, contactPhone)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sos_broadcast_submit")
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BROADCAST SOS ALERT NOW", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}
