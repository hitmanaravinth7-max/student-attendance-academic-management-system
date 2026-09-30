package com.example.util

import com.example.model.AppLanguage

object Strings {
    private val en = mapOf(
        "app_title" to "LifeLink",
        "app_subtitle" to "Emergency Blood Finder",
        "sos_button" to "SOS EMERGENCY",
        "sos_broadcast" to "Instant Blood Broadcast",
        "sos_subtitle" to "Alert nearest donors and blood banks immediately",
        "blood_availability" to "Blood Availability",
        "search_banks" to "Find Blood Units",
        "emergency_request" to "Emergency Requests",
        "post_request" to "Post Urgent Request",
        "find_donors" to "Emergency Donor Finder",
        "donor_dashboard" to "Donor Dashboard",
        "admin_panel" to "Blood Bank Stock Panel",
        "education" to "Education & FAQs",
        "map_view" to "Live Radar & Map",
        "login" to "Login",
        "register" to "Register",
        "logout" to "Logout",
        "units_available" to "Units Available",
        "critical" to "CRITICAL",
        "open" to "Open",
        "fulfilled" to "Fulfilled",
        "call_now" to "Call",
        "directions" to "Get Directions",
        "i_can_donate" to "I Can Donate",
        "share" to "Share Alert",
        "eligible" to "Eligible to Donate",
        "ineligible_cooldown" to "Ineligible (Cooldown)",
        "consent_notice" to "I consent to sharing my phone number for verified blood emergencies",
        "privacy_protected" to "Phone number protected. Tap to request contact."
    )

    private val hi = mapOf(
        "app_title" to "लाइफलिंक",
        "app_subtitle" to "आपातकालीन रक्त खोजक",
        "sos_button" to "आपातकालीन SOS",
        "sos_broadcast" to "त्वरित रक्त प्रसारण",
        "sos_subtitle" to "निकटतम रक्तदाताओं और ब्लड बैंकों को तुरंत सूचित करें",
        "blood_availability" to "रक्त उपलब्धता",
        "search_banks" to "रक्त यूनिट खोजें",
        "emergency_request" to "आपातकालीन अनुरोध",
        "post_request" to "आपातकालीन अनुरोध भेजें",
        "find_donors" to "रक्तदाता खोजें",
        "donor_dashboard" to "रक्तदाता डैशबोर्ड",
        "admin_panel" to "ब्लड बैंक स्टॉक पैनल",
        "education" to "जानकारी एवं प्रश्नोत्तर",
        "map_view" to "लाइव रडार और मैप",
        "login" to "लॉग इन",
        "register" to "रजिस्टर करें",
        "logout" to "लॉग आउट",
        "units_available" to "उपलब्ध यूनिट्स",
        "critical" to "अति आवश्यक",
        "open" to "सक्रिय",
        "fulfilled" to "पूर्ण",
        "call_now" to "कॉल करें",
        "directions" to "दिशा-निर्देश",
        "i_can_donate" to "मैं रक्तदान कर सकता हूँ",
        "share" to "साझा करें",
        "eligible" to "रक्तदान के योग्य",
        "ineligible_cooldown" to "प्रतीक्षा अवधि (अयोग्य)",
        "consent_notice" to "मैं आपात स्थिति में अपना फोन नंबर साझा करने की सहमति देता हूँ",
        "privacy_protected" to "फोन नंबर सुरक्षित है। संपर्क करने के लिए टैप करें।"
    )

    fun get(key: String, lang: AppLanguage): String {
        val map = if (lang == AppLanguage.HINDI) hi else en
        return map[key] ?: en[key] ?: key
    }
}
