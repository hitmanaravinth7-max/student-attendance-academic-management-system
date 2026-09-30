package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.LifeLinkViewModel
import com.example.ui.ScreenRoute
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.*

@Composable
fun DonorDashboardScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val donors by viewModel.donors.collectAsState()
    val donationRecords by viewModel.donationRecords.collectAsState()

    // Find the donor profile matching current user or default to first donor
    val currentDonor = remember(donors, currentUser) {
        donors.firstOrNull { it.id == "donor_1" } ?: donors.first()
    }

    var isAvailable by remember(currentDonor) { mutableStateOf(currentDonor.isAvailable) }
    var showLogDialog by remember { mutableStateOf(false) }

    val isEligible = currentDonor.isEligibleBy90DayRule()
    val daysRemaining = currentDonor.daysUntilEligible()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Donor Header Profile Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BloodGroupBadge(bloodGroup = currentDonor.bloodGroup, isLarge = true)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentDonor.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                )
                                Text(
                                    text = "City: ${currentDonor.city} • Age: ${currentDonor.age}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Hero Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BloodRedContainer
                        ) {
                            Text(
                                text = "HERO DONOR",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = BloodRedPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Emergency Availability Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Emergency Alert Availability",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isAvailable) "You will receive nearby emergency alerts" else "Alerts paused",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAvailable,
                                onCheckedChange = {
                                    isAvailable = it
                                    viewModel.toggleDonorAvailability(currentDonor.id, it)
                                },
                                modifier = Modifier.testTag("donor_availability_switch")
                            )
                        }
                    }
                }
            }
        }

        // 90-Day Donation Cooldown & Eligibility Status Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEligible) VitalGreenContainer else Color(0xFFFFF3E0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEligible) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (isEligible) VitalGreen else EmergencyOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isEligible) "Ready & Eligible to Donate" else "Donation Cooldown Active",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isEligible) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isEligible)
                            "You have passed the mandatory 90-day recovery window. You can save lives today!"
                        else
                            "Medical safety protocol: A 90-day cooldown between whole blood donations protects your hemoglobin and iron stores. $daysRemaining days remaining.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!isEligible) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ((90 - daysRemaining) / 90f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EmergencyOrange,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Impact Statistics (Lives Saved Estimate)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${currentDonor.totalDonations}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = BloodRedPrimary
                        )
                        Text(
                            text = "Total Donations",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${currentDonor.totalDonations * 3}+",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = VitalGreen
                        )
                        Text(
                            text = "Lives Saved (Est.)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Log New Donation Button & Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Donation History Timeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = { showLogDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("log_donation_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Donation", fontSize = 12.sp)
                }
            }
        }

        // Donation Records List
        items(donationRecords, key = { it.id }) { record ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BloodRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bloodtype,
                                contentDescription = null,
                                tint = BloodRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(record.bloodBank, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "${record.units} Unit • ${record.component.displayName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = record.dateDisplay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Log Donation Dialog
    if (showLogDialog) {
        var hospitalInput by remember { mutableStateOf("Red Cross Central Blood Bank") }
        var unitsInput by remember { mutableIntStateOf(1) }

        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = {
                Text("Log a Completed Donation", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recording a donation updates your lifetime counter and resets your 90-day cooldown safety timer.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = hospitalInput,
                        onValueChange = { hospitalInput = it },
                        label = { Text("Hospital or Blood Bank Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Units Donated:", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(onClick = { if (unitsInput > 1) unitsInput-- }) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text("$unitsInput Unit", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            FilledTonalIconButton(onClick = { if (unitsInput < 3) unitsInput++ }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logNewDonation(currentDonor.id, hospitalInput, unitsInput)
                        showLogDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text("Save & Start Cooldown")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
