package fr.plopez.go4lunch.view.landing_page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityLandingPageBinding
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.view.main_activity.MainActivity

class LandingPageActivity : AppCompatActivity(R.layout.activity_landing_page) {

    companion object {
        private val TAG = "LandingPageActivity"

        // Google
        private val GOOGLE_AUTH_REQUEST_CODE = 999

        private val IS_LOADING_STATE = "IS_LOADING_STATE"
    }

    private lateinit var snack : CustomSnackBar

    // Loading state
    private var isLoading = false

    // View binding
    private lateinit var binding: ActivityLandingPageBinding

    // Facebook callManager
    private lateinit var callbackManager : CallbackManager

    // Firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLandingPageBinding.inflate(layoutInflater)

        // Restore the previous loading state or initialize it
        if (savedInstanceState != null) {
            loading(savedInstanceState.getBoolean(IS_LOADING_STATE, false))
        } else {
            loading(false)
        }

        //Initialize custom snackBar
        snack = CustomSnackBar(findViewById(android.R.id.content), applicationContext)

        // Initialize Facebook call manager
        callbackManager = CallbackManager.Factory.create()

        // Initialize Firebase Auth
        firebaseAuth = Firebase.auth

        // Removes the notification bar
        val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        this.window.setFlags(fullScreenFlag, fullScreenFlag)

        // ------------------- Facebook authentication -------------------
        val facebookButton = findViewById<Button>(R.id.landing_page_facebook_login_button)
        facebookButton.setOnClickListener { loginWithFacebook() }
        // ---------------------------------------------------------------

        // ------------------- Google authentication -------------------
        val googleButton = findViewById<Button>(R.id.landing_page_google_login_button)
        googleButton.setOnClickListener { loginWithGoogle() }
        // ---------------------------------------------------------------

        val layout = findViewById<ConstraintLayout>(R.id.landing_page_container)
        val button = findViewById<Button>(R.id.landing_page_email_login_button_selection)
        val group =
            findViewById<androidx.constraintlayout.widget.Group>(R.id.landing_page_email_login_group)

        // ------------------- Email authentication -------------------
        // Manage login with email display
        button.setOnClickListener {
            // Transition to show login with email stuff when button clicked
            TransitionManager.beginDelayedTransition(layout)
            // Clears the selection button
            it.isVisible = false
            group.isVisible = true
        }
        // ---------------------------------------------------------------

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_LOADING_STATE, isLoading)
    }

    // Navigation utility
    private fun goMainActivity() {
        MainActivity.navigate(this)
    }

    // FaceBook authentication
    private fun loginWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, setOf("email"))
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                // Access allowed to main activity
                goMainActivity()
            }

            override fun onCancel() {
                snack.showWarningSnackBar("Facebook login cancelled")
            }

            override fun onError(error: FacebookException?) {
                snack.showErrorSnackBar("Facebook login failed: ${error.toString()}")
            }
        })
    }

    // Google authentication
    private fun loginWithGoogle() {

        // Configure Google Sign In
        val googleSignInOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient =
            GoogleSignIn.getClient(this@LandingPageActivity, googleSignInOptions)

        // Launch Google Sign In
        loading(true)
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, GOOGLE_AUTH_REQUEST_CODE)
    }

    // Google authentication on Firebase
    private fun firebaseAuthWithGoogle(account : GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        signInToFirebase(credential)
    }

    // Firebase authentication
    private fun signInToFirebase(credential: AuthCredential){
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    loading(false)
                    goMainActivity()
                } else {
                    loading(false)
                    snack.showWarningSnackBar("Hey, you're not connected")
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_AUTH_REQUEST_CODE) {

            loading(true)

            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

                if (account == null) {
                    loading(false)
                    snack.showErrorSnackBar("Google sign in failed. Account is empty.")
                } else {
                    firebaseAuthWithGoogle(account)
                }

            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                loading(false)
                snack.showErrorSnackBar("Google sign in failed.")
                Log.d(TAG, "#### onActivityResult: ${task.exception}")
            }
        }
    }

    // Display loading indicator utility
    private fun loading(isLoading: Boolean) {
        this.isLoading = isLoading

        if (isLoading) {
            binding.landingPageProgressIndicator.visibility = View.VISIBLE
        } else {
            binding.landingPageProgressIndicator.visibility = View.GONE
        }
    }

}