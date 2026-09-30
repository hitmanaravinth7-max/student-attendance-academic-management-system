package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.LifeLinkViewModel
import com.example.ui.ScreenRoute
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.*
import com.example.util.Strings

@Composable
fun HomeScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val emergencyRequests by viewModel.emergencyRequests.collectAsState()
    val bloodBanks by viewModel.bloodBanks.collectAsState()
    val donors by viewModel.donors.collectAsState()

    // Count statistics
    val totalActiveUnits = remember(bloodBanks) {
        bloodBanks.sumOf { bank ->
            bank.stockMap.values.sumOf { compMap -> compMap.values.sum() }
        }
    }
    val activeEmergencies = remember(emergencyRequests) {
        emergencyRequests.filter { !it.isFulfilled }
    }
    val availableDonorsCount = remember(donors) {
        donors.count { it.isAvailable && it.isEligibleBy90DayRule() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Section with Big Red SOS Emergency Button
        item {
            HeroSosSection(
                onSosClick = { viewModel.openSosDialog() },
                lang = currentLang
            )
        }

        // Live Vital Statistics Strip
        item {
            VitalStatsStrip(
                unitsCount = totalActiveUnits,
                activeRequests = activeEmergencies.size,
                donorsCount = availableDonorsCount
            )
        }

        // Quick Action Grid (4 Primary Cards)
        item {
            QuickActionsGrid(
                onSearchClick = { viewModel.navigateTo(ScreenRoute.BLOOD_SEARCH) },
                onFindDonorsClick = { viewModel.navigateTo(ScreenRoute.DONOR_FINDER) },
                onPostRequestClick = { viewModel.navigateTo(ScreenRoute.EMERGENCY_REQUESTS) },
                onMapClick = { viewModel.navigateTo(ScreenRoute.MAP_RADAR) }
            )
        }

        // Urgent Blood Broadcasts Header & Carousel
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = BloodRedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Emergency Requests",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    TextButton(onClick = { viewModel.navigateTo(ScreenRoute.EMERGENCY_REQUESTS) }) {
                        Text("View All (${activeEmergencies.size})", color = BloodRedPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                if (activeEmergencies.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No active urgent requests right now. Blood supply is stable.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(activeEmergencies.take(5)) { req ->
                            EmergencyCarouselCard(
                                request = req,
                                onDonateClick = { viewModel.pledgeToDonate(context, req.id) },
                                onCallClick = { viewModel.launchPhoneDialer(context, req.contactNumber) },
                                onShareClick = { viewModel.shareEmergencyRequest(context, req) }
                            )
                        }
                    }
                }
            }
        }

        // Blood Compatibility Quick Cheat-Sheet Card
        item {
            CompatibilityQuickCard(
                onFullGuideClick = { viewModel.navigateTo(ScreenRoute.EDUCATION) }
            )
        }

        // Emergency Helpline Speed Dialer Card
        item {
            HelplineBannerCard(
                onDialHelpline = { viewModel.launchPhoneDialer(context, "108") },
                onDialBloodBank = { viewModel.launchPhoneDialer(context, "104") }
            )
        }
    }
}

@Composable
fun HeroSosSection(
    onSosClick: () -> Unit,
    lang: AppLanguage
) {
    // Pulse animation for big red SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BloodRedContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(top = 16.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BloodRedPrimary.copy(alpha = 0.12f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BloodRedPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CRITICAL SITUATION? 1-TAP BROADCAST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = BloodRedPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Big Red SOS Emergency Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(150.dp)
            ) {
                // Outer glowing pulse ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(BloodRedLight.copy(alpha = glowAlpha))
                )

                // Main circular SOS button
                Surface(
                    shape = CircleShape,
                    color = BloodRedPrimary,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(118.dp)
                        .clickable { onSosClick() }
                        .testTag("sos_emergency_hero_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BloodRedLight, BloodRedPrimary, BloodRedDark)
                                )
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SOS",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "EMERGENCY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Text(
                text = Strings.get("sos_subtitle", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun VitalStatsStrip(
    unitsCount: Int,
    activeRequests: Int,
    donorsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = "$unitsCount",
            label = "Blood Units",
            icon = Icons.Default.Bloodtype,
            tint = BloodRedPrimary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "$activeRequests",
            label = "Urgent Needs",
            icon = Icons.Default.Emergency,
            tint = EmergencyOrange
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "$donorsCount",
            label = "Ready Donors",
            icon = Icons.Default.PersonSearch,
            tint = VitalGreen
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onSearchClick: () -> Unit,
    onFindDonorsClick: () -> Unit,
    onPostRequestClick: () -> Unit,
    onMapClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Check Blood Availability",
                subtitle = "Search banks by type & city",
                icon = Icons.Default.LocalHospital,
                gradient = listOf(Color(0xFFE53935), Color(0xFFC62828)),
                onClick = onSearchClick,
                testTag = "action_search_blood"
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Emergency Donor Finder",
                subtitle = "Matching blood group & distance",
                icon = Icons.Default.PersonSearch,
                gradient = listOf(Color(0xFFEF5350), Color(0xFFD32F2F)),
                onClick = onFindDonorsClick,
                testTag = "action_find_donors"
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Post Urgent Request",
                subtitle = "Alert donors & community",
                icon = Icons.Default.Campaign,
                gradient = listOf(Color(0xFFF57C00), Color(0xFFE65100)),
                onClick = onPostRequestClick,
                testTag = "action_post_request"
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Live Radar & Map",
                subtitle = "Nearby donors & blood banks",
                icon = Icons.Default.Explore,
                gradient = listOf(Color(0xFF43A047), Color(0xFF2E7D32)),
                onClick = onMapClick,
                testTag = "action_map_radar"
            )
        }
    }
}

@Composable
fun ActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 3.dp,
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencyCarouselCard(
    request: EmergencyRequest,
    onDonateClick: () -> Unit,
    onCallClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BloodGroupBadge(bloodGroup = request.bloodGroup, isLarge = true)
                UrgencyBadge(urgency = request.urgency)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${request.unitsNeeded} Units ${request.component.displayName}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Patient: ${request.patientName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${request.hospital}, ${request.city}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (request.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = request.notes,
                    fontSize = 11.sp,
                    color = BloodRedPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onDonateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I Can Help", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VitalGreenContainer)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = VitalGreen, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CompatibilityQuickCard(
    onFullGuideClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BloodRedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Blood Compatibility Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                TextButton(onClick = onFullGuideClick) {
                    Text("Interactive Guide", fontSize = 12.sp, color = BloodRedPrimary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BloodRedContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Universal Donor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BloodRedPrimary
                        )
                        Text(
                            text = "O Negative (O−)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = BloodRedDark
                        )
                        Text(
                            text = "Can donate red blood cells to anyone in need.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VitalGreenContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Universal Recipient",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = VitalGreen
                        )
                        Text(
                            text = "AB Positive (AB+)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Can receive red blood cells from any blood type.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelplineBannerCard(
    onDialHelpline: () -> Unit,
    onDialBloodBank: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = BloodRedDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Emergency Medical Lines",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(
                    text = "National Ambulance (108) • Blood Helpline (104)",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onDialHelpline,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White,
                        contentColor = BloodRedPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("108", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
