package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class LifeLinkRepository {

    private val _bloodBanks = MutableStateFlow<List<BloodBank>>(getInitialBloodBanks())
    val bloodBanks: StateFlow<List<BloodBank>> = _bloodBanks.asStateFlow()

    private val _emergencyRequests = MutableStateFlow<List<EmergencyRequest>>(getInitialEmergencyRequests())
    val emergencyRequests: StateFlow<List<EmergencyRequest>> = _emergencyRequests.asStateFlow()

    private val _donors = MutableStateFlow<List<DonorProfile>>(getInitialDonors())
    val donors: StateFlow<List<DonorProfile>> = _donors.asStateFlow()

    private val _donationRecords = MutableStateFlow<List<DonationRecord>>(getInitialDonationRecords())
    val donationRecords: StateFlow<List<DonationRecord>> = _donationRecords.asStateFlow()

    private val _currentUser = MutableStateFlow<UserSession?>(
        // Default initial session for immediate ease of exploration: Donor role
        UserSession(
            id = "user_donor_1",
            name = "Dr. Sameer Verma",
            email = "sameer.verma@example.com",
            phone = "9812345678",
            role = UserRole.DONOR,
            bloodGroup = BloodGroup.O_NEG,
            city = "Delhi",
            consentGranted = true
        )
    )
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
    }

    // Emergency Broadcast
    fun broadcastEmergencyRequest(
        patientName: String,
        bloodGroup: BloodGroup,
        component: BloodComponent,
        unitsNeeded: Int,
        hospital: String,
        city: String,
        urgency: UrgencyLevel,
        contactNumber: String,
        notes: String
    ): EmergencyRequest {
        val newRequest = EmergencyRequest(
            id = "REQ-" + UUID.randomUUID().toString().take(6).uppercase(),
            patientName = patientName.trim(),
            bloodGroup = bloodGroup,
            component = component,
            unitsNeeded = unitsNeeded,
            hospital = hospital.trim(),
            city = city.trim(),
            urgency = urgency,
            contactNumber = contactNumber.trim(),
            notes = notes.trim(),
            timestampMillis = System.currentTimeMillis(),
            isFulfilled = false,
            donorPledgesCount = 0,
            requesterId = _currentUser.value?.id ?: "guest"
        )
        _emergencyRequests.update { listOf(newRequest) + it }
        return newRequest
    }

    fun pledgeToDonate(requestId: String): Boolean {
        var updated = false
        _emergencyRequests.update { list ->
            list.map { req ->
                if (req.id == requestId) {
                    updated = true
                    req.copy(donorPledgesCount = req.donorPledgesCount + 1)
                } else req
            }
        }
        return updated
    }

    fun markRequestFulfilled(requestId: String) {
        _emergencyRequests.update { list ->
            list.map { req ->
                if (req.id == requestId) req.copy(isFulfilled = true) else req
            }
        }
    }

    // Admin Stock Updates
    fun updateBloodStock(
        bankId: String,
        bloodGroup: BloodGroup,
        component: BloodComponent,
        deltaUnits: Int
    ) {
        _bloodBanks.update { banks ->
            banks.map { bank ->
                if (bank.id == bankId) {
                    val currentGroupStock = bank.stockMap[bloodGroup]?.toMutableMap() ?: mutableMapOf()
                    val currentUnits = currentGroupStock[component] ?: 0
                    val newUnits = (currentUnits + deltaUnits).coerceAtLeast(0)
                    currentGroupStock[component] = newUnits

                    val newStockMap = bank.stockMap.toMutableMap()
                    newStockMap[bloodGroup] = currentGroupStock

                    bank.copy(
                        stockMap = newStockMap,
                        lastUpdatedMinutesAgo = 0
                    )
                } else bank
            }
        }
    }

    // Donor Profile Actions
    fun toggleDonorAvailability(donorId: String, isAvailable: Boolean) {
        _donors.update { list ->
            list.map { if (it.id == donorId) it.copy(isAvailable = isAvailable) else it }
        }
    }

    fun logNewDonation(donorId: String, bloodBankName: String, units: Int = 1) {
        val now = System.currentTimeMillis()
        _donors.update { list ->
            list.map {
                if (it.id == donorId) {
                    it.copy(
                        lastDonationMillis = now,
                        totalDonations = it.totalDonations + 1
                    )
                } else it
            }
        }
        val newRecord = DonationRecord(
            id = "DON-" + UUID.randomUUID().toString().take(5).uppercase(),
            dateDisplay = "Today",
            bloodBank = bloodBankName,
            component = BloodComponent.WHOLE_BLOOD,
            units = units
        )
        _donationRecords.update { listOf(newRecord) + it }
    }

    // Auth & Session
    fun login(emailOrPhone: String, role: UserRole, bloodGroup: BloodGroup? = null): Boolean {
        val user = UserSession(
            id = "user_" + UUID.randomUUID().toString().take(6),
            name = if (emailOrPhone.contains("@")) emailOrPhone.substringBefore("@").replace(".", " ").capitalize() else "User $emailOrPhone",
            email = if (emailOrPhone.contains("@")) emailOrPhone else "$emailOrPhone@lifelink.org",
            phone = if (emailOrPhone.all { it.isDigit() }) emailOrPhone else "9876543210",
            role = role,
            bloodGroup = bloodGroup ?: BloodGroup.O_POS,
            city = "Delhi",
            consentGranted = true
        )
        _currentUser.value = user
        return true
    }

    fun switchRole(role: UserRole) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(role = role)
    }

    fun logout() {
        _currentUser.value = null
    }

    companion object {
        private fun getInitialBloodBanks(): List<BloodBank> {
            val defaultStock = { oNeg: Int, oPos: Int, aPos: Int, bPos: Int, abPos: Int ->
                mapOf(
                    BloodGroup.O_NEG to mapOf(BloodComponent.WHOLE_BLOOD to oNeg, BloodComponent.PLASMA to 4, BloodComponent.PLATELETS to 2),
                    BloodGroup.O_POS to mapOf(BloodComponent.WHOLE_BLOOD to oPos, BloodComponent.PLASMA to 12, BloodComponent.PLATELETS to 8),
                    BloodGroup.A_POS to mapOf(BloodComponent.WHOLE_BLOOD to aPos, BloodComponent.PLASMA to 8, BloodComponent.PLATELETS to 5),
                    BloodGroup.A_NEG to mapOf(BloodComponent.WHOLE_BLOOD to 3, BloodComponent.PLASMA to 2, BloodComponent.PLATELETS to 1),
                    BloodGroup.B_POS to mapOf(BloodComponent.WHOLE_BLOOD to bPos, BloodComponent.PLASMA to 9, BloodComponent.PLATELETS to 6),
                    BloodGroup.B_NEG to mapOf(BloodComponent.WHOLE_BLOOD to 2, BloodComponent.PLASMA to 3, BloodComponent.PLATELETS to 1),
                    BloodGroup.AB_POS to mapOf(BloodComponent.WHOLE_BLOOD to abPos, BloodComponent.PLASMA to 5, BloodComponent.PLATELETS to 4),
                    BloodGroup.AB_NEG to mapOf(BloodComponent.WHOLE_BLOOD to 1, BloodComponent.PLASMA to 1, BloodComponent.PLATELETS to 1)
                )
            }

            return listOf(
                BloodBank(
                    id = "bank_1",
                    name = "Red Cross Central Blood Bank",
                    hospitalAffiliation = "All India Institute of Medical Sciences (AIIMS)",
                    city = "Delhi",
                    address = "Ansari Nagar East, Ring Road, New Delhi",
                    phone = "+91 11 2658 8500",
                    lastUpdatedMinutesAgo = 8,
                    stockMap = defaultStock(6, 24, 18, 15, 7),
                    distanceKm = 1.8,
                    latitude = 28.5672,
                    longitude = 77.2100,
                    expiringSoonUnits = 3
                ),
                BloodBank(
                    id = "bank_2",
                    name = "City Life Blood Center",
                    hospitalAffiliation = "Max Super Speciality Hospital",
                    city = "Delhi",
                    address = "1, 2, Press Enclave Marg, Saket, New Delhi",
                    phone = "+91 11 2651 5050",
                    lastUpdatedMinutesAgo = 22,
                    stockMap = defaultStock(2, 14, 10, 8, 4),
                    distanceKm = 3.6,
                    latitude = 28.5284,
                    longitude = 77.2185,
                    expiringSoonUnits = 5
                ),
                BloodBank(
                    id = "bank_3",
                    name = "Rotary Blood Bank & Research Hub",
                    hospitalAffiliation = "Sir Ganga Ram Hospital",
                    city = "Delhi",
                    address = "Rajinder Nagar, New Delhi",
                    phone = "+91 11 4225 4000",
                    lastUpdatedMinutesAgo = 45,
                    stockMap = defaultStock(1, 8, 12, 11, 2),
                    distanceKm = 5.2,
                    latitude = 28.6385,
                    longitude = 77.1895,
                    expiringSoonUnits = 1
                ),
                BloodBank(
                    id = "bank_4",
                    name = "Apollo Blood Bank & Transfusion Medicine",
                    hospitalAffiliation = "Apollo Hospitals",
                    city = "Mumbai",
                    address = "Mathura Road, Sarita Vihar, Mumbai Central",
                    phone = "+91 22 2307 8899",
                    lastUpdatedMinutesAgo = 14,
                    stockMap = defaultStock(4, 19, 16, 12, 6),
                    distanceKm = 4.1,
                    latitude = 18.9712,
                    longitude = 72.8197,
                    expiringSoonUnits = 2
                ),
                BloodBank(
                    id = "bank_5",
                    name = "Fortis Memorial Blood & Cell Bank",
                    hospitalAffiliation = "Fortis Hospital",
                    city = "Bengaluru",
                    address = "Bannerghatta Main Road, Bengaluru",
                    phone = "+91 80 6621 4444",
                    lastUpdatedMinutesAgo = 30,
                    stockMap = defaultStock(3, 11, 14, 9, 3),
                    distanceKm = 6.4,
                    latitude = 12.8954,
                    longitude = 77.5990,
                    expiringSoonUnits = 0
                )
            )
        }

        private fun getInitialEmergencyRequests(): List<EmergencyRequest> {
            val now = System.currentTimeMillis()
            return listOf(
                EmergencyRequest(
                    id = "REQ-84729",
                    patientName = "Aarav Sharma",
                    bloodGroup = BloodGroup.O_NEG,
                    component = BloodComponent.WHOLE_BLOOD,
                    unitsNeeded = 3,
                    hospital = "AIIMS Trauma Centre, Bed 14",
                    city = "Delhi",
                    urgency = UrgencyLevel.CRITICAL,
                    contactNumber = "+91 98112 34567",
                    notes = "Emergency surgery scheduled following road accident. Rare O- donor desperately required.",
                    timestampMillis = now - (18 * 60 * 1000), // 18 mins ago
                    isFulfilled = false,
                    donorPledgesCount = 1
                ),
                EmergencyRequest(
                    id = "REQ-59312",
                    patientName = "Priya Mukherjee",
                    bloodGroup = BloodGroup.B_POS,
                    component = BloodComponent.PLATELETS,
                    unitsNeeded = 4,
                    hospital = "Max Healthcare Saket, Onco Ward",
                    city = "Delhi",
                    urgency = UrgencyLevel.WITHIN_24H,
                    contactNumber = "+91 98765 89012",
                    notes = "Platelet count dropped below 15,000 during chemotherapy. Single donor platelets preferred.",
                    timestampMillis = now - (75 * 60 * 1000), // 1.25 hours ago
                    isFulfilled = false,
                    donorPledgesCount = 2
                ),
                EmergencyRequest(
                    id = "REQ-31844",
                    patientName = "Mohammed Rafiq",
                    bloodGroup = BloodGroup.AB_NEG,
                    component = BloodComponent.WHOLE_BLOOD,
                    unitsNeeded = 2,
                    hospital = "Apollo Hospital, Cardio Ward",
                    city = "Mumbai",
                    urgency = UrgencyLevel.CRITICAL,
                    contactNumber = "+91 91234 56789",
                    notes = "Emergency bypass surgery preparation. Rare blood group.",
                    timestampMillis = now - (120 * 60 * 1000),
                    isFulfilled = false,
                    donorPledgesCount = 0
                ),
                EmergencyRequest(
                    id = "REQ-11204",
                    patientName = "Kavita Reddy",
                    bloodGroup = BloodGroup.A_POS,
                    component = BloodComponent.PLASMA,
                    unitsNeeded = 2,
                    hospital = "Fortis Hospital, ICU 3",
                    city = "Bengaluru",
                    urgency = UrgencyLevel.PLANNED,
                    contactNumber = "+91 99445 12345",
                    notes = "Plasma exchange therapy planned for tomorrow morning.",
                    timestampMillis = now - (300 * 60 * 1000),
                    isFulfilled = true,
                    donorPledgesCount = 3
                )
            )
        }

        private fun getInitialDonors(): List<DonorProfile> {
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 60 * 60 * 1000
            return listOf(
                DonorProfile(
                    id = "donor_1",
                    name = "Rahul Malhotra",
                    age = 29,
                    bloodGroup = BloodGroup.O_NEG,
                    city = "Delhi",
                    phone = "+91 98101 23456",
                    lastDonationMillis = now - (120L * dayMillis), // >90 days ago -> Eligible!
                    isAvailable = true,
                    totalDonations = 8,
                    consentToShareContact = true,
                    distanceKm = 1.4,
                    latitude = 28.5700,
                    longitude = 77.2150
                ),
                DonorProfile(
                    id = "donor_2",
                    name = "Ananya Desai",
                    age = 26,
                    bloodGroup = BloodGroup.B_POS,
                    city = "Delhi",
                    phone = "+91 98234 56789",
                    lastDonationMillis = now - (150L * dayMillis), // Eligible
                    isAvailable = true,
                    totalDonations = 5,
                    consentToShareContact = true,
                    distanceKm = 2.1,
                    latitude = 28.5350,
                    longitude = 77.2220
                ),
                DonorProfile(
                    id = "donor_3",
                    name = "Vikramaditya Rao",
                    age = 34,
                    bloodGroup = BloodGroup.O_POS,
                    city = "Delhi",
                    phone = "+91 98345 67890",
                    lastDonationMillis = now - (42L * dayMillis), // Donated 42 days ago -> Ineligible (cooldown)
                    isAvailable = true,
                    totalDonations = 12,
                    consentToShareContact = true,
                    distanceKm = 3.5,
                    latitude = 28.6400,
                    longitude = 77.1950
                ),
                DonorProfile(
                    id = "donor_4",
                    name = "Sneha Patel",
                    age = 23,
                    bloodGroup = BloodGroup.A_NEG,
                    city = "Delhi",
                    phone = "+91 98456 78901",
                    lastDonationMillis = null, // First time donor -> Eligible
                    isAvailable = true,
                    totalDonations = 0,
                    consentToShareContact = true,
                    distanceKm = 4.2,
                    latitude = 28.5500,
                    longitude = 77.2300
                ),
                DonorProfile(
                    id = "donor_5",
                    name = "Karan Singh",
                    age = 31,
                    bloodGroup = BloodGroup.AB_POS,
                    city = "Delhi",
                    phone = "+91 98567 89012",
                    lastDonationMillis = now - (95L * dayMillis), // Eligible
                    isAvailable = false, // Temporarily marked busy
                    totalDonations = 4,
                    consentToShareContact = true,
                    distanceKm = 5.0,
                    latitude = 28.6100,
                    longitude = 77.1800
                ),
                DonorProfile(
                    id = "donor_6",
                    name = "Pooja Hegde",
                    age = 27,
                    bloodGroup = BloodGroup.O_NEG,
                    city = "Delhi",
                    phone = "+91 98678 90123",
                    lastDonationMillis = now - (105L * dayMillis), // Eligible
                    isAvailable = true,
                    totalDonations = 6,
                    consentToShareContact = true,
                    distanceKm = 2.8,
                    latitude = 28.5800,
                    longitude = 77.2050
                )
            )
        }

        private fun getInitialDonationRecords(): List<DonationRecord> {
            return listOf(
                DonationRecord("DON-101", "12 Oct 2025", "Red Cross Central Blood Bank", BloodComponent.WHOLE_BLOOD, 1),
                DonationRecord("DON-102", "18 Jun 2025", "AIIMS Blood Transfusion Center", BloodComponent.WHOLE_BLOOD, 1),
                DonationRecord("DON-103", "04 Jan 2025", "Rotary Blood Bank & Research Hub", BloodComponent.PLATELETS, 1),
                DonationRecord("DON-104", "15 Aug 2024", "City Life Blood Center", BloodComponent.WHOLE_BLOOD, 1)
            )
        }
    }
}
