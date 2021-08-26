package fr.plopez.go4lunch.view.landing_page

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser

class LandingPageViewModel: ViewModel() {

    fun setLoginSuccessful(success: Boolean){

    }

    fun setPermissionsAccepted(accepted: Boolean){

    }

    fun setUserInformation(user: FirebaseUser?){
        if (user != null){

            user.displayName
            user.email
            user.photoUrl
        }
    }

}