package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.LifeLinkViewModel
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorFinderScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val donors by viewModel.donors.collectAsState()
    val targetGroup by viewModel.donorBloodGroup.collectAsState()
    val includeCompatible by viewModel.includeCompatibleDonors.collectAsState()
    val maxDistance by viewModel.donorMaxDistance.collectAsState()
    val cityFilter by viewModel.donorCityFilter.collectAsState()

    var consentDialogOpenForDonor by remember { mutableStateOf<DonorProfile?>(null) }
    var unmaskedDonorId by remember { mutableStateOf<String?>(null) }

    // Compute compatible donor blood groups
    val compatibleGroups = remember(targetGroup, includeCompatible) {
        if (includeCompatible) {
            BloodGroup.getCompatibleDonors(targetGroup)
        } else {
            listOf(targetGroup)
        }
    }

    // Filter donors
    val matchingDonors = remember(donors, compatibleGroups, maxDistance, cityFilter) {
        donors.filter { donor ->
            val matchesGroup = donor.bloodGroup in compatibleGroups
            val matchesDist = donor.distanceKm <= maxDistance
            val matchesCity = cityFilter.isBlank() || donor.city.contains(cityFilter, ignoreCase = true)
            matchesGroup && matchesDist && matchesCity
        }.sortedWith(
            compareByDescending<DonorProfile> { it.isAvailable && it.isEligibleBy90DayRule() }
                .thenBy { it.distanceKm }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Filters Surface
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Find Emergency Donors for Blood Group:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Blood Group Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BloodGroup.entries.forEach { group ->
                        FilterChip(
                            selected = targetGroup == group,
                            onClick = { viewModel.donorBloodGroup.value = group },
                            label = { Text(group.label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compatibility Logic Toggle Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BloodRedContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Blood Compatibility Logic",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BloodRedPrimary
                            )
                            Text(
                                text = if (includeCompatible)
                                    "Showing ${targetGroup.label} + compatible (${compatibleGroups.joinToString { it.label }})"
                                else "Showing only exact ${targetGroup.label} match",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeCompatible,
                            onCheckedChange = { viewModel.includeCompatibleDonors.value = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Distance Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Radius: Up to ${maxDistance.toInt()} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = maxDistance,
                        onValueChange = { viewModel.donorMaxDistance.value = it },
                        valueRange = 2f..50f,
                        steps = 8,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                }
            }
        }

        // Header Results Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${matchingDonors.size} Matching Donors Found",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = VitalGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Privacy Shield Active",
                    fontSize = 11.sp,
                    color = VitalGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Donors List
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (matchingDonors.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No donors found within ${maxDistance.toInt()} km radius.")
                            TextButton(onClick = { viewModel.donorMaxDistance.value = 50f }) {
                                Text("Expand Radius to 50 km")
                            }
                        }
                    }
                }
            } else {
                items(matchingDonors, key = { it.id }) { donor ->
                    DonorItemCard(
                        donor = donor,
                        isPhoneUnmasked = unmaskedDonorId == donor.id,
                        onRequestContact = { consentDialogOpenForDonor = donor },
                        onDirectCall = { viewModel.launchPhoneDialer(context, donor.phone) },
                        onDirectWhatsApp = {
                            val cleanNumber = donor.phone.filter { it.isDigit() }
                            try {
                                val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=Hello%20${donor.name},%20urgent%20blood%20donation%20requirement%20via%20LifeLink"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.launchPhoneDialer(context, donor.phone)
                            }
                        }
                    )
                }
            }
        }
    }

    // Consent and Contact Sheet/Dialog
    if (consentDialogOpenForDonor != null) {
        val targetDonor = consentDialogOpenForDonor!!
        var userConsentGiven by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { consentDialogOpenForDonor = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Emergency Contact Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To respect donor privacy, direct phone contact is restricted to genuine medical emergencies.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Connecting with: ${targetDonor.name} (${targetDonor.bloodGroup.label})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = userConsentGiven,
                            onCheckedChange = { userConsentGiven = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I certify that this is a critical medical need for a patient.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        unmaskedDonorId = targetDonor.id
                        consentDialogOpenForDonor = null
                        Toast.makeText(context, "Contact details unlocked for ${targetDonor.name}", Toast.LENGTH_SHORT).show()
                    },
                    enabled = userConsentGiven,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary)
                ) {
                    Text("Unlock & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { consentDialogOpenForDonor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DonorItemCard(
    donor: DonorProfile,
    isPhoneUnmasked: Boolean,
    onRequestContact: () -> Unit,
    onDirectCall: () -> Unit,
    onDirectWhatsApp: () -> Unit
) {
    val isEligible = donor.isEligibleBy90DayRule()
    val daysRemaining = donor.daysUntilEligible()

    // Mask phone e.g. "+91 98*** **456"
    val maskedPhone = remember(donor.phone, isPhoneUnmasked) {
        if (isPhoneUnmasked) {
            donor.phone
        } else {
            val digits = donor.phone
            if (digits.length >= 10) {
                "${digits.take(5)} **** ${digits.takeLast(3)}"
            } else {
                "***-***-****"
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Blood Group, Name, Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodGroupBadge(bloodGroup = donor.bloodGroup, isLarge = true)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = donor.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Age: ${donor.age} • ${donor.totalDonations} Donations so far",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${donor.distanceKm} km away",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Strip: Availability & 90-day Cooldown Eligibility
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (donor.isAvailable) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VitalGreenContainer
                    ) {
                        Text(
                            text = "Available Now",
                            color = VitalGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEEEEEE)
                    ) {
                        Text(
                            text = "Temporarily Busy",
                            color = Color(0xFF777777),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isEligible) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VitalGreenContainer
                    ) {
                        Text(
                            text = "Eligible to Donate",
                            color = VitalGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = "Ineligible: Cooldown ($daysRemaining days left)",
                            color = EmergencyOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Phone number with Privacy Status
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPhoneUnmasked) Icons.Default.Phone else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isPhoneUnmasked) VitalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = maskedPhone,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    if (!isPhoneUnmasked) {
                        Text(
                            text = "Consent Required",
                            fontSize = 11.sp,
                            color = BloodRedPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Call / WhatsApp
            if (!isPhoneUnmasked) {
                Button(
                    onClick = onRequestContact,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_donor_contact_button")
                ) {
                    Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Emergency Contact", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDirectCall,
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Now", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onDirectWhatsApp,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = VitalGreenContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = VitalGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = VitalGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
