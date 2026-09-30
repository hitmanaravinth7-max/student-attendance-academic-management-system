package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
fun BloodSearchScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bloodBanks by viewModel.bloodBanks.collectAsState()
    val selectedGroup by viewModel.searchBloodGroup.collectAsState()
    val selectedComponent by viewModel.searchComponent.collectAsState()
    val searchCity by viewModel.searchCity.collectAsState()

    // Filter logic
    val filteredBanks = remember(bloodBanks, selectedGroup, selectedComponent, searchCity) {
        bloodBanks.filter { bank ->
            val matchesCity = searchCity.isBlank() || bank.city.contains(searchCity, ignoreCase = true) || bank.name.contains(searchCity, ignoreCase = true)
            matchesCity
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Header & Filters Bar
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Search Input for City / Hospital
                OutlinedTextField(
                    value = searchCity,
                    onValueChange = { viewModel.searchCity.value = it },
                    placeholder = { Text("Search city (Delhi, Mumbai...) or hospital") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchCity.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchCity.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blood_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Component Filter (Whole Blood, Plasma, Platelets)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BloodComponent.entries.forEach { comp ->
                        FilterChip(
                            selected = selectedComponent == comp,
                            onClick = { viewModel.searchComponent.value = comp },
                            label = { Text(comp.displayName, fontSize = 12.sp) },
                            leadingIcon = if (selectedComponent == comp) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Blood Group Horizontal Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { viewModel.searchBloodGroup.value = null },
                        label = { Text("All Groups", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )

                    BloodGroup.entries.forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = {
                                viewModel.searchBloodGroup.value = if (selectedGroup == group) null else group
                            },
                            label = { Text(group.label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // Results Count & Status Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredBanks.size} Blood Banks & Hospitals Available",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedGroup != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BloodRedContainer
                ) {
                    Text(
                        text = "Filtering: ${selectedGroup?.label} (${selectedComponent.displayName})",
                        color = BloodRedPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // List of Blood Banks
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredBanks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No blood banks found for current search filter.")
                            TextButton(onClick = {
                                viewModel.searchCity.value = ""
                                viewModel.searchBloodGroup.value = null
                            }) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            } else {
                items(filteredBanks, key = { it.id }) { bank ->
                    BloodBankItemCard(
                        bank = bank,
                        selectedGroup = selectedGroup,
                        selectedComponent = selectedComponent,
                        onCallClick = { viewModel.launchPhoneDialer(context, bank.phone) },
                        onDirectionsClick = {
                            viewModel.launchDirections(context, bank.latitude, bank.longitude, bank.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BloodBankItemCard(
    bank: BloodBank,
    selectedGroup: BloodGroup?,
    selectedComponent: BloodComponent,
    onCallClick: () -> Unit,
    onDirectionsClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Name, Hospital, Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bank.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = bank.hospitalAffiliation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${bank.distanceKm} km",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address and Last Updated time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BloodRedPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${bank.address}, ${bank.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Last updated ${bank.lastUpdatedMinutesAgo} mins ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (bank.expiringSoonUnits > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${bank.expiringSoonUnits} units expiring <48h",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmergencyOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock Inventory Grid Preview
            Text(
                text = "Available Units (${selectedComponent.displayName}):",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Grid of blood groups with unit counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BloodGroup.entries.forEach { group ->
                    val units = bank.stockMap[group]?.get(selectedComponent) ?: 0
                    val isHighlighted = selectedGroup == null || selectedGroup == group

                    val (chipBg, chipBorder, textColor) = when {
                        units == 0 -> Triple(Color(0xFFEEEEEE), Color(0xFFCCCCCC), Color(0xFF888888))
                        units < 5 -> Triple(Color(0xFFFFEBEE), BloodRedLight, BloodRedPrimary) // Low stock
                        else -> Triple(Color(0xFFE8F5E9), VitalGreen, Color(0xFF1B5E20)) // Good stock
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isHighlighted) chipBg else chipBg.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = group.label,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = textColor
                            )
                            Text(
                                text = "$units u",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Call & Get Directions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCallClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Now", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDirectionsClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Get Directions", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
