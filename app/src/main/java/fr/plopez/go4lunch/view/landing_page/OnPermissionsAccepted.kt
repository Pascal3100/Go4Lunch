package fr.plopez.go4lunch.view.landing_page

import android.content.Intent

interface OnPermissionsAccepted {
    fun onPermissionsAccepted(accepted: Boolean)
    fun onGPSActivationRequest(intent: Intent)
}