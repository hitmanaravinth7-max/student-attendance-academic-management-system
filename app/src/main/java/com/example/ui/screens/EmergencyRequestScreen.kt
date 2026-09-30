package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.LifeLinkViewModel
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.*

@Composable
fun EmergencyRequestScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val emergencyRequests by viewModel.emergencyRequests.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Feed, 1: Broadcast New

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row: Live Feed vs Post New Request
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BloodRedPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active Requests (${emergencyRequests.count { !it.isFulfilled }})", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_active_requests")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast Alert", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_post_request")
            )
        }

        if (selectedTab == 0) {
            // Live Feed List
            ActiveRequestsListView(
                requests = emergencyRequests,
                onPledgeDonate = { viewModel.pledgeToDonate(context, it) },
                onCallRequester = { viewModel.launchPhoneDialer(context, it) },
                onShareRequest = { viewModel.shareEmergencyRequest(context, it) },
                onMarkFulfilled = { viewModel.markRequestFulfilled(it) }
            )
        } else {
            // Broadcast Form
            BroadcastRequestForm(
                onSubmit = { name, group, comp, units, hosp, city, urg, phone, notes ->
                    viewModel.broadcastFullRequest(name, group, comp, units, hosp, city, urg, phone, notes)
                    selectedTab = 0 // Switch back to live list to see the posted alert!
                }
            )
        }
    }
}

@Composable
fun ActiveRequestsListView(
    requests: List<EmergencyRequest>,
    onPledgeDonate: (String) -> Unit,
    onCallRequester: (String) -> Unit,
    onShareRequest: (EmergencyRequest) -> Unit,
    onMarkFulfilled: (String) -> Unit
) {
    var showOnlyOpen by remember { mutableStateOf(false) }

    val displayedRequests = remember(requests, showOnlyOpen) {
        if (showOnlyOpen) requests.filter { !it.isFulfilled } else requests
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-Time Emergency Feed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                FilterChip(
                    selected = showOnlyOpen,
                    onClick = { showOnlyOpen = !showOnlyOpen },
                    label = { Text("Open Only", fontSize = 12.sp) }
                )
            }
        }

        if (displayedRequests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No emergency requests in this view.")
                }
            }
        } else {
            items(displayedRequests, key = { it.id }) { req ->
                EmergencyRequestItemCard(
                    req = req,
                    onPledgeDonate = { onPledgeDonate(req.id) },
                    onCallRequester = { onCallRequester(req.contactNumber) },
                    onShareRequest = { onShareRequest(req) },
                    onMarkFulfilled = { onMarkFulfilled(req.id) }
                )
            }
        }
    }
}

@Composable
fun EmergencyRequestItemCard(
    req: EmergencyRequest,
    onPledgeDonate: () -> Unit,
    onCallRequester: () -> Unit,
    onShareRequest: () -> Unit,
    onMarkFulfilled: () -> Unit
) {
    val minutesAgo = ((System.currentTimeMillis() - req.timestampMillis) / (60 * 1000)).coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (req.isFulfilled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (req.isFulfilled) 1.dp else 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Blood Group, Status Badge, Urgency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodGroupBadge(bloodGroup = req.bloodGroup, isLarge = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${req.unitsNeeded} Units ${req.component.displayName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (req.isFulfilled) MaterialTheme.colorScheme.onSurfaceVariant else BloodRedPrimary
                        )
                        Text(
                            text = "Patient: ${req.patientName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (req.isFulfilled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VitalGreenContainer
                        ) {
                            Text(
                                text = "FULFILLED",
                                color = VitalGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        UrgencyBadge(urgency = req.urgency)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$minutesAgo min ago",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hospital Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${req.hospital}, ${req.city}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (req.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${req.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Pledges Count Strip
            if (!req.isFulfilled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = null,
                        tint = VitalGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${req.donorPledgesCount} verified donors responded",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VitalGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            if (!req.isFulfilled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPledgeDonate,
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Bloodtype, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I Can Donate", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onCallRequester,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = VitalGreenContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = VitalGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", color = VitalGreen, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onShareRequest,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                }

                TextButton(
                    onClick = onMarkFulfilled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text("Mark as Fulfilled (Received Blood)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun BroadcastRequestForm(
    onSubmit: (
        patientName: String,
        bloodGroup: BloodGroup,
        component: BloodComponent,
        unitsNeeded: Int,
        hospital: String,
        city: String,
        urgency: UrgencyLevel,
        contactNumber: String,
        notes: String
    ) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(BloodGroup.O_NEG) }
    var selectedComponent by remember { mutableStateOf(BloodComponent.WHOLE_BLOOD) }
    var unitsNeeded by remember { mutableIntStateOf(2) }
    var hospital by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Delhi") }
    var urgency by remember { mutableStateOf(UrgencyLevel.CRITICAL) }
    var contactPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Post Urgent Blood Requirement",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = BloodRedPrimary
            )
            Text(
                text = "Alerts verified nearby donors with compatible blood types instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("Patient Name *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("broadcast_patient_name")
            )
        }

        // Blood Group Selector
        item {
            Text("Required Blood Group *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BloodGroup.entries.forEach { group ->
                    FilterChip(
                        selected = selectedGroup == group,
                        onClick = { selectedGroup = group },
                        label = { Text(group.label, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Component & Units
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Component *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BloodComponent.entries.forEach { comp ->
                            FilterChip(
                                selected = selectedComponent == comp,
                                onClick = { selectedComponent = comp },
                                label = { Text(comp.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Units Needed:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { if (unitsNeeded > 1) unitsNeeded-- }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = "$unitsNeeded Units",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    FilledTonalIconButton(onClick = { if (unitsNeeded < 20) unitsNeeded++ }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        }

        // Hospital and City
        item {
            OutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it },
                label = { Text("Hospital Name & Room/Ward *") },
                placeholder = { Text("e.g. AIIMS Trauma Center, ICU Ward 2") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("broadcast_hospital")
            )
        }

        item {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("broadcast_city")
            )
        }

        // Urgency Level
        item {
            Text("Urgency Level *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UrgencyLevel.entries.forEach { level ->
                    FilterChip(
                        selected = urgency == level,
                        onClick = { urgency = level },
                        label = { Text(level.title, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Emergency Contact Phone Number *") },
                placeholder = { Text("10-digit mobile number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("broadcast_contact_phone")
            )
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Clinical Notes (Optional)") },
                placeholder = { Text("e.g., Replacement donor accepted, surgery at 4 PM") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (patientName.isBlank()) {
                        errorMessage = "Please enter patient name."
                        return@Button
                    }
                    if (hospital.isBlank()) {
                        errorMessage = "Please enter hospital name and ward."
                        return@Button
                    }
                    if (contactPhone.filter { it.isDigit() }.length < 10) {
                        errorMessage = "Please enter a valid 10-digit phone number."
                        return@Button
                    }
                    errorMessage = null
                    onSubmit(patientName, selectedGroup, selectedComponent, unitsNeeded, hospital, city, urgency, contactPhone, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("broadcast_submit_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BROADCAST EMERGENCY ALERT", fontWeight = FontWeight.Black)
            }
        }
    }
}
