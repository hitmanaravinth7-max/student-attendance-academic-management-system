package com.example.model

enum class BloodGroup(val label: String) {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    companion object {
        fun fromLabel(label: String): BloodGroup {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: O_POS
        }

        // Full red blood cell compatibility matrix
        fun getCompatibleDonors(recipient: BloodGroup): List<BloodGroup> {
            return when (recipient) {
                A_POS -> listOf(A_POS, A_NEG, O_POS, O_NEG)
                A_NEG -> listOf(A_NEG, O_NEG)
                B_POS -> listOf(B_POS, B_NEG, O_POS, O_NEG)
                B_NEG -> listOf(B_NEG, O_NEG)
                AB_POS -> listOf(AB_POS, AB_NEG, A_POS, A_NEG, B_POS, B_NEG, O_POS, O_NEG) // Universal Recipient
                AB_NEG -> listOf(AB_NEG, A_NEG, B_NEG, O_NEG)
                O_POS -> listOf(O_POS, O_NEG)
                O_NEG -> listOf(O_NEG) // Can only receive O-
            }
        }

        fun getCanDonateTo(donor: BloodGroup): List<BloodGroup> {
            return when (donor) {
                O_NEG -> listOf(A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG) // Universal Donor
                O_POS -> listOf(O_POS, A_POS, B_POS, AB_POS)
                A_NEG -> listOf(A_NEG, A_POS, AB_NEG, AB_POS)
                A_POS -> listOf(A_POS, AB_POS)
                B_NEG -> listOf(B_NEG, B_POS, AB_NEG, AB_POS)
                B_POS -> listOf(B_POS, AB_POS)
                AB_NEG -> listOf(AB_NEG, AB_POS)
                AB_POS -> listOf(AB_POS)
            }
        }
    }
}

enum class BloodComponent(val displayName: String) {
    WHOLE_BLOOD("Whole Blood"),
    PLASMA("Plasma"),
    PLATELETS("Platelets"),
    CRYO("Cryoprecipitate")
}

enum class UrgencyLevel(val title: String, val timeWindow: String, val severityLevel: Int) {
    CRITICAL("Critical", "Immediate (0-2 hrs)", 1),
    WITHIN_24H("Within 24 Hours", "Required today", 2),
    PLANNED("Planned", "Within 48-72 hrs", 3)
}

enum class UserRole(val displayName: String) {
    DONOR("Donor"),
    REQUESTER("Requester"),
    ADMIN("Blood Bank Admin")
}

data class BloodBank(
    val id: String,
    val name: String,
    val hospitalAffiliation: String,
    val city: String,
    val address: String,
    val phone: String,
    val lastUpdatedMinutesAgo: Int,
    val stockMap: Map<BloodGroup, Map<BloodComponent, Int>>,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val expiringSoonUnits: Int = 0
)

data class EmergencyRequest(
    val id: String,
    val patientName: String,
    val bloodGroup: BloodGroup,
    val component: BloodComponent,
    val unitsNeeded: Int,
    val hospital: String,
    val city: String,
    val urgency: UrgencyLevel,
    val contactNumber: String,
    val notes: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val isFulfilled: Boolean = false,
    val donorPledgesCount: Int = 0,
    val requesterId: String = ""
)

data class DonorProfile(
    val id: String,
    val name: String,
    val age: Int,
    val bloodGroup: BloodGroup,
    val city: String,
    val phone: String,
    val lastDonationMillis: Long?,
    val isAvailable: Boolean = true,
    val totalDonations: Int = 0,
    val consentToShareContact: Boolean = true,
    val distanceKm: Double = 2.4,
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090
) {
    // 90-day donation cooldown rule
    fun isEligibleBy90DayRule(): Boolean {
        if (lastDonationMillis == null) return true
        val ninetyDaysMillis = 90L * 24 * 60 * 60 * 1000
        val diff = System.currentTimeMillis() - lastDonationMillis
        return diff >= ninetyDaysMillis
    }

    fun daysUntilEligible(): Int {
        if (lastDonationMillis == null) return 0
        val ninetyDaysMillis = 90L * 24 * 60 * 60 * 1000
        val diff = System.currentTimeMillis() - lastDonationMillis
        if (diff >= ninetyDaysMillis) return 0
        val remainingMillis = ninetyDaysMillis - diff
        return (remainingMillis / (24 * 60 * 60 * 1000)).toInt() + 1
    }
}

data class DonationRecord(
    val id: String,
    val dateDisplay: String,
    val bloodBank: String,
    val component: BloodComponent,
    val units: Int
)

data class UserSession(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val bloodGroup: BloodGroup? = null,
    val city: String = "Delhi",
    val consentGranted: Boolean = true
)

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)")
}
