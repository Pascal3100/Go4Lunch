package fr.plopez.go4lunch.view.landing_page

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LandingPageViewModel @Inject constructor() : ViewModel() {

    fun setLoginSuccessful(success: Boolean){

    }

    fun setPermissionsAccepted(accepted: Boolean){

    }

    fun connectedUser(user: FirebaseUser?){
        if (user != null){
            user.displayName
            user.email
            user.photoUrl
        }
    }

}