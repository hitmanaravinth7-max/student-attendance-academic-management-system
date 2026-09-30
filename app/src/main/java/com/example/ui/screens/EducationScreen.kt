package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BloodGroup
import com.example.ui.LifeLinkViewModel
import com.example.ui.components.BloodGroupBadge
import com.example.ui.theme.*

data class MythFact(val myth: String, val fact: String)
data class FaqItem(val question: String, val answer: String)

@Composable
fun EducationScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Eligibility Quiz, 1: Compatibility Chart, 2: Myths & FAQs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BloodRedPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Eligibility Check", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Compatibility", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Myths & FAQs", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> EligibilityQuizView()
            1 -> CompatibilityMatrixView()
            2 -> MythsAndFaqView()
        }
    }
}

@Composable
fun EligibilityQuizView() {
    var ageOver18 by remember { mutableStateOf(true) }
    var weightOver50 by remember { mutableStateOf(true) }
    var hemoglobinGood by remember { mutableStateOf(true) }
    var noRecentTattoo by remember { mutableStateOf(true) }
    var noRecentSurgery by remember { mutableStateOf(true) }
    var passed90Days by remember { mutableStateOf(true) }

    val isAllEligible = ageOver18 && weightOver50 && hemoglobinGood && noRecentTattoo && noRecentSurgery && passed90Days

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAllEligible) VitalGreenContainer else Color(0xFFFFEBEE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAllEligible) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isAllEligible) VitalGreen else BloodRedPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAllEligible) "You Are Eligible to Donate Blood!" else "You May Be Currently Ineligible",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isAllEligible) Color(0xFF1B5E20) else BloodRedPrimary
                        )
                        Text(
                            text = if (isAllEligible) "Visit your nearest blood bank to save up to 3 lives." else "Review criteria below. Some restrictions are temporary.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Interactive Pre-Donation Checklist:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            EligibilityItem(
                title = "Age between 18 and 65 years",
                subtitle = "Standard medical criteria for donor consent",
                checked = ageOver18,
                onCheckedChange = { ageOver18 = it }
            )
        }
        item {
            EligibilityItem(
                title = "Weight is at least 50 kg (110 lbs)",
                subtitle = "Ensures safe blood volume collection (approx 350-450 ml)",
                checked = weightOver50,
                onCheckedChange = { weightOver50 = it }
            )
        }
        item {
            EligibilityItem(
                title = "Good general health & hemoglobin (>12.5 g/dL)",
                subtitle = "No active colds, fever, or infectious symptoms",
                checked = hemoglobinGood,
                onCheckedChange = { hemoglobinGood = it }
            )
        }
        item {
            EligibilityItem(
                title = "No tattoos or piercings in last 6 months",
                subtitle = "Standard safety window to prevent blood-borne pathogens",
                checked = noRecentTattoo,
                onCheckedChange = { noRecentTattoo = it }
            )
        }
        item {
            EligibilityItem(
                title = "No major dental or surgery in last 3 months",
                subtitle = "Body must be completely healed",
                checked = noRecentSurgery,
                onCheckedChange = { noRecentSurgery = it }
            )
        }
        item {
            EligibilityItem(
                title = "At least 90 days since your last whole blood donation",
                subtitle = "Mandatory recovery cooldown for iron stores and red cells",
                checked = passed90Days,
                onCheckedChange = { passed90Days = it }
            )
        }
    }
}

@Composable
fun EligibilityItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun CompatibilityMatrixView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Red Blood Cell Compatibility Matrix",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = BloodRedPrimary
            )
            Text(
                text = "Check who you can receive from and donate to safely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(BloodGroup.entries) { group ->
            val canReceiveFrom = BloodGroup.getCompatibleDonors(group)
            val canDonateTo = BloodGroup.getCanDonateTo(group)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BloodGroupBadge(bloodGroup = group, isLarge = true)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Blood Type ${group.label}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            if (group == BloodGroup.O_NEG) {
                                Text("Universal Donor (Red Cells)", color = BloodRedPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            } else if (group == BloodGroup.AB_POS) {
                                Text("Universal Recipient", color = VitalGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Can receive red cells from:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        canReceiveFrom.forEach { fromGroup ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VitalGreenContainer
                            ) {
                                Text(
                                    text = fromGroup.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VitalGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Can donate red cells to:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        canDonateTo.forEach { toGroup ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BloodRedContainer
                            ) {
                                Text(
                                    text = toGroup.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BloodRedPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MythsAndFaqView() {
    val myths = listOf(
        MythFact("Donating blood makes you physically weak or sick.", "Your body replenishes blood fluid in 24-48 hours and red blood cells within weeks. Normal activities can be resumed the same day."),
        MythFact("You can contract diseases like HIV or Hepatitis by donating.", "Every needle, tube, and blood bag used is 100% sterile, single-use, and disposed of immediately. It is impossible to catch an infection from donating."),
        MythFact("People on medications can never donate blood.", "Most common medications like birth control, mild allergy or blood pressure medicines do not disqualify you. Ask the donation doctor."),
        MythFact("Vegetarians cannot donate blood because of low iron.", "As long as your hemoglobin test reaches 12.5 g/dL, your diet is completely fine!")
    )

    val faqs = listOf(
        FaqItem("How much blood is taken during a donation?", "Usually 350 ml to 450 ml, which is only about 8-10% of an adult's total blood volume. Your body easily compensates."),
        FaqItem("How long does the entire blood donation process take?", "The actual blood collection takes only 8-10 minutes. The total visit including registration and resting takes about 30 minutes."),
        FaqItem("Why must donors wait 90 days between donations?", "90 days allows full recovery of your red blood cells and bone marrow iron reserves, ensuring your personal health remains pristine.")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Busting Common Blood Donation Myths",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = BloodRedPrimary
            )
        }

        items(myths) { item ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Myth: ${item.myth}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BloodRedPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "Fact: ${item.fact}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
            )
        }

        items(faqs) { faq ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(faq.question, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(faq.answer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
