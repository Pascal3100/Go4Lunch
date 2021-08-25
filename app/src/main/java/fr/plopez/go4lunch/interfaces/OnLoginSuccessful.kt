package fr.plopez.go4lunch.interfaces

import com.google.firebase.auth.FirebaseUser

interface OnLoginSuccessful {
    fun onLoginSuccessful(success: Boolean)
}