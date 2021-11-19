package fr.plopez.go4lunch.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import fr.plopez.go4lunch.data.model.restaurant.User
import fr.plopez.go4lunch.data.model.restaurant.Workmate

class FirebaseAuthUtils {

    companion object {
        private const val EMAIL_NAME_PATTERN = "(.*)@.*"
    }
    // Firebase Auth
    private val firebaseUser = Firebase.auth.currentUser

    fun isFirebaseUserNotNull(firebaseUser: FirebaseUser?)=
        firebaseUser == null

    fun getUser() = User(
            email = firebaseUser?.email!!,
            name = firebaseUser.displayName ?: Regex(EMAIL_NAME_PATTERN).find(firebaseUser.email!!)?.groupValues?.get(1)!!,
            photoUrl = firebaseUser.photoUrl.toString()
        )

    fun getUser(displayName:String?, email:String?, photoUrl:String?)=
        User(
            email = email!!,
            name = displayName ?: Regex(EMAIL_NAME_PATTERN).find(email)?.groupValues?.get(1)!!,
            photoUrl = photoUrl.toString()
        )

    fun getWorkmate() =
        Workmate(
            email = firebaseUser?.email!!,
            name = firebaseUser.displayName ?: Regex(EMAIL_NAME_PATTERN).find(firebaseUser.email!!)?.groupValues?.get(1)!!,
            photoUrl = firebaseUser.photoUrl.toString()
        )

    fun getWorkmate(displayName:String?, email:String?, photoUrl:String?)=
        Workmate(
            email = email!!,
            name = displayName ?: Regex(EMAIL_NAME_PATTERN).find(email)?.groupValues?.get(1)!!,
            photoUrl = photoUrl.toString()
        )
}