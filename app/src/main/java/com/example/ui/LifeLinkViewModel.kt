package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LifeLinkRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenRoute {
    HOME,
    BLOOD_SEARCH,
    EMERGENCY_REQUESTS,
    DONOR_FINDER,
    MAP_RADAR,
    DONOR_DASHBOARD,
    ADMIN_PANEL,
    EDUCATION,
    AUTH
}

data class UiNotification(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val isEmergency: Boolean = false
)

class LifeLinkViewModel(
    val repository: LifeLinkRepository = LifeLinkRepository()
) : ViewModel() {

    val bloodBanks = repository.bloodBanks
    val emergencyRequests = repository.emergencyRequests
    val donors = repository.donors
    val donationRecords = repository.donationRecords
    val currentUser = repository.currentUser
    val selectedLanguage = repository.selectedLanguage

    private val _currentScreen = MutableStateFlow(ScreenRoute.HOME)
    val currentScreen: StateFlow<ScreenRoute> = _currentScreen.asStateFlow()

    private val _bannerNotification = MutableStateFlow<UiNotification?>(null)
    val bannerNotification: StateFlow<UiNotification?> = _bannerNotification.asStateFlow()

    // Blood search filters
    val searchBloodGroup = MutableStateFlow<BloodGroup?>(null)
    val searchComponent = MutableStateFlow(BloodComponent.WHOLE_BLOOD)
    val searchCity = MutableStateFlow("")

    // Donor search filters
    val donorBloodGroup = MutableStateFlow(BloodGroup.O_NEG)
    val includeCompatibleDonors = MutableStateFlow(true)
    val donorMaxDistance = MutableStateFlow(15f)
    val donorCityFilter = MutableStateFlow("")

    // Requests filter
    val requestsFilterOpenOnly = MutableStateFlow(false)

    // SOS Dialog State
    val isSosDialogOpen = MutableStateFlow(false)

    // Log Donation Dialog State
    val isLogDonationDialogOpen = MutableStateFlow(false)

    // Quick Contact Request Dialog for Donor (privacy shield)
    val contactDonorDialogTarget = MutableStateFlow<DonorProfile?>(null)

    // Auth Form State
    val isSignUpMode = MutableStateFlow(false)
    val authEmailOrPhone = MutableStateFlow("")
    val authPassword = MutableStateFlow("")
    val authConfirmPassword = MutableStateFlow("")
    val authName = MutableStateFlow("")
    val authRole = MutableStateFlow(UserRole.DONOR)
    val authBloodGroup = MutableStateFlow(BloodGroup.O_POS)
    val authCity = MutableStateFlow("Delhi")
    val authRememberMe = MutableStateFlow(true)
    val authDonorConsent = MutableStateFlow(true)
    val authShowPassword = MutableStateFlow(false)
    val authErrorMessage = MutableStateFlow<String?>(null)
    val authOtpDialogVisible = MutableStateFlow(false)
    val authSimulatedOtp = MutableStateFlow("4829")
    val authEnteredOtp = MutableStateFlow("")

    fun navigateTo(route: ScreenRoute) {
        _currentScreen.value = route
    }

    fun dismissNotification() {
        _bannerNotification.value = null
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
    }

    fun openSosDialog() {
        isSosDialogOpen.value = true
    }

    fun closeSosDialog() {
        isSosDialogOpen.value = false
    }

    fun triggerSosBroadcast(
        patientName: String,
        bloodGroup: BloodGroup,
        unitsNeeded: Int,
        hospital: String,
        city: String,
        contactNumber: String
    ) {
        repository.broadcastEmergencyRequest(
            patientName = if (patientName.isBlank()) "Emergency Patient" else patientName,
            bloodGroup = bloodGroup,
            component = BloodComponent.WHOLE_BLOOD,
            unitsNeeded = unitsNeeded,
            hospital = if (hospital.isBlank()) "Central Trauma Hospital" else hospital,
            city = if (city.isBlank()) "Delhi" else city,
            urgency = UrgencyLevel.CRITICAL,
            contactNumber = contactNumber,
            notes = "CRITICAL SOS Broadcast via 1-Tap Emergency Trigger"
        )
        isSosDialogOpen.value = false
        _bannerNotification.value = UiNotification(
            title = "SOS Emergency Broadcast Sent!",
            message = "Alert dispatched to 14 verified $bloodGroup donors and 5 blood banks nearby!",
            isEmergency = true
        )
        _currentScreen.value = ScreenRoute.EMERGENCY_REQUESTS
    }

    fun broadcastFullRequest(
        patientName: String,
        bloodGroup: BloodGroup,
        component: BloodComponent,
        unitsNeeded: Int,
        hospital: String,
        city: String,
        urgency: UrgencyLevel,
        contactNumber: String,
        notes: String
    ) {
        val req = repository.broadcastEmergencyRequest(
            patientName = patientName,
            bloodGroup = bloodGroup,
            component = component,
            unitsNeeded = unitsNeeded,
            hospital = hospital,
            city = city,
            urgency = urgency,
            contactNumber = contactNumber,
            notes = notes
        )
        _bannerNotification.value = UiNotification(
            title = "Emergency Request Broadcasted",
            message = "Alert posted for ${req.unitsNeeded} units of ${req.bloodGroup.label} ${req.component.displayName} in ${req.city}."
        )
    }

    fun pledgeToDonate(context: Context, requestId: String) {
        val success = repository.pledgeToDonate(requestId)
        if (success) {
            Toast.makeText(context, "Thank you! Your pledge has been registered.", Toast.LENGTH_SHORT).show()
        }
    }

    fun markRequestFulfilled(requestId: String) {
        repository.markRequestFulfilled(requestId)
    }

    fun updateBloodStock(
        bankId: String,
        bloodGroup: BloodGroup,
        component: BloodComponent,
        delta: Int
    ) {
        repository.updateBloodStock(bankId, bloodGroup, component, delta)
    }

    fun toggleDonorAvailability(donorId: String, isAvailable: Boolean) {
        repository.toggleDonorAvailability(donorId, isAvailable)
    }

    fun logNewDonation(donorId: String, bloodBankName: String, units: Int) {
        repository.logNewDonation(donorId, bloodBankName, units)
        isLogDonationDialogOpen.value = false
        _bannerNotification.value = UiNotification(
            title = "Donation Recorded!",
            message = "Thank you for saving lives! Your 90-day cooldown period has commenced."
        )
    }

    // Intents
    fun launchPhoneDialer(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumber.replace(" ", "")}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchDirections(context: Context, latitude: Double, longitude: Double, label: String) {
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Directions: Lat $latitude, Long $longitude", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareEmergencyRequest(context: Context, req: EmergencyRequest) {
        try {
            val text = """
                URGENT BLOOD REQUIREMENT:
                Patient: ${req.patientName}
                Blood Group: ${req.bloodGroup.label} (${req.component.displayName})
                Units Needed: ${req.unitsNeeded}
                Hospital: ${req.hospital}, ${req.city}
                Urgency: ${req.urgency.title}
                Contact: ${req.contactNumber}
                Notes: ${req.notes}
                
                Broadcasted via LifeLink Blood Network. Please share!
            """.trimIndent()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Emergency Blood Request")
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share alert", Toast.LENGTH_SHORT).show()
        }
    }

    // Auth logic
    fun executeAuth(context: Context) {
        authErrorMessage.value = null
        val input = authEmailOrPhone.value.trim()
        val pass = authPassword.value

        if (input.isEmpty()) {
            authErrorMessage.value = "Please enter your email or 10-digit mobile number."
            return
        }

        val isEmail = input.contains("@") && input.contains(".")
        val is10DigitPhone = input.filter { it.isDigit() }.length >= 10

        if (!isEmail && !is10DigitPhone) {
            authErrorMessage.value = "Enter a valid email address or 10-digit phone number."
            return
        }

        if (pass.length < 8) {
            authErrorMessage.value = "Password must be at least 8 characters long."
            return
        }

        if (isSignUpMode.value) {
            if (pass != authConfirmPassword.value) {
                authErrorMessage.value = "Passwords do not match."
                return
            }
            if (authRole.value == UserRole.DONOR && !authDonorConsent.value) {
                authErrorMessage.value = "Donors must accept emergency contact consent."
                return
            }
        }

        repository.login(
            emailOrPhone = input,
            role = authRole.value,
            bloodGroup = if (authRole.value == UserRole.DONOR) authBloodGroup.value else null
        )

        Toast.makeText(context, "Logged in as ${authRole.value.displayName}!", Toast.LENGTH_SHORT).show()

        // Redirect by role
        when (authRole.value) {
            UserRole.DONOR -> _currentScreen.value = ScreenRoute.DONOR_DASHBOARD
            UserRole.ADMIN -> _currentScreen.value = ScreenRoute.ADMIN_PANEL
            UserRole.REQUESTER -> _currentScreen.value = ScreenRoute.EMERGENCY_REQUESTS
        }
    }

    fun startForgotPasswordFlow() {
        authSimulatedOtp.value = (1000..9999).random().toString()
        authEnteredOtp.value = ""
        authOtpDialogVisible.value = true
    }

    fun verifyOtpAndReset(context: Context) {
        if (authEnteredOtp.value.trim() == authSimulatedOtp.value) {
            authOtpDialogVisible.value = false
            Toast.makeText(context, "OTP verified! Password reset link sent.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Invalid OTP code. Please enter ${authSimulatedOtp.value}", Toast.LENGTH_SHORT).show()
        }
    }

    fun loginWithGoogle(context: Context) {
        repository.login("google.user@gmail.com", UserRole.DONOR, BloodGroup.O_POS)
        Toast.makeText(context, "Signed in via Google successfully!", Toast.LENGTH_SHORT).show()
        _currentScreen.value = ScreenRoute.DONOR_DASHBOARD
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = ScreenRoute.HOME
    }
}
