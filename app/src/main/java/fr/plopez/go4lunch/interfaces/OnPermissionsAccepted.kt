package fr.plopez.go4lunch.interfaces

import android.content.Intent

interface OnPermissionsAccepted {
    fun onPermissionsAccepted(accepted: Boolean)
    fun onGPSActivationRequest(intent: Intent)
}