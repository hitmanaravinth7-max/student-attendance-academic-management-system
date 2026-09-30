package com.example.ui.screens

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

@Composable
fun AdminPanelScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val bloodBanks by viewModel.bloodBanks.collectAsState()

    var selectedBankId by remember { mutableStateOf(bloodBanks.firstOrNull()?.id ?: "bank_1") }
    val currentBank = remember(bloodBanks, selectedBankId) {
        bloodBanks.firstOrNull { it.id == selectedBankId } ?: bloodBanks.first()
    }
    var selectedComponent by remember { mutableStateOf(BloodComponent.WHOLE_BLOOD) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Admin Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Blood Bank Admin Panel",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = BloodRedPrimary
                            )
                            Text(
                                text = "Real-time stock adjustment & low inventory management",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BloodRedContainer
                        ) {
                            Text(
                                text = "ADMIN ACCESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bank Selector Chips
                    Text("Select Blood Bank Facility:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bloodBanks.forEach { bank ->
                            FilterChip(
                                selected = selectedBankId == bank.id,
                                onClick = { selectedBankId = bank.id },
                                label = { Text(bank.name.take(24), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Low Stock & Expiry Alert Notice
        item {
            val lowStockCount = remember(currentBank, selectedComponent) {
                BloodGroup.entries.count { group ->
                    (currentBank.stockMap[group]?.get(selectedComponent) ?: 0) < 5
                }
            }

            if (lowStockCount > 0) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = BloodRedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Critical Inventory Alert!",
                                fontWeight = FontWeight.Bold,
                                color = BloodRedPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "$lowStockCount blood groups have fewer than 5 units in stock. Replenishment requested.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Component Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventory Grid (${selectedComponent.displayName}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BloodComponent.entries.forEach { comp ->
                        FilterChip(
                            selected = selectedComponent == comp,
                            onClick = { selectedComponent = comp },
                            label = { Text(comp.displayName.take(5), fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // 8 Blood Groups Inventory Stepper Cards
        items(BloodGroup.entries) { group ->
            val units = currentBank.stockMap[group]?.get(selectedComponent) ?: 0
            val isLowStock = units < 5

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BloodGroupBadge(bloodGroup = group, isLarge = false)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Group ${group.label}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isLowStock) {
                                Text(
                                    text = "LOW STOCK ALERT (<5)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BloodRedPrimary
                                )
                            } else {
                                Text(
                                    text = "Optimal Reserve",
                                    fontSize = 11.sp,
                                    color = VitalGreen
                                )
                            }
                        }
                    }

                    // Stepper (- / count / +)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = {
                                if (units > 0) {
                                    viewModel.updateBloodStock(currentBank.id, group, selectedComponent, -1)
                                }
                            },
                            enabled = units > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(
                            text = "$units",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                viewModel.updateBloodStock(currentBank.id, group, selectedComponent, 1)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }
        }

        // Quick Sync Notice
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = VitalGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stock changes sync instantly with public donor and emergency search.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
