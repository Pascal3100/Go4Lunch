package fr.plopez.go4lunch.view.landing_page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityLandingPageBinding
import fr.plopez.go4lunch.view.main_activity.MainActivity

class LandingPageActivity : AppCompatActivity(R.layout.activity_landing_page) {

    private lateinit var binding: ActivityLandingPageBinding

    // Facebook callManager
    private lateinit var callbackManager : CallbackManager

    // Google
    private val RC_SIGN_IN = 1

    // Firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLandingPageBinding.inflate(layoutInflater)

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
                Toast.makeText(
                    this@LandingPageActivity,
                    "Facebook login cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(
                    this@LandingPageActivity,
                    "Facebook login failed: ${error.toString()}",
                    Toast.LENGTH_SHORT
                ).show()
            }

        })
    }

    // Google authentication
    private fun loginWithGoogle() {

        Log.d("TAG", "#### loginWithGoogle")
        // Configure Google Sign In
        val googleSignInOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient =
            GoogleSignIn.getClient(this@LandingPageActivity, googleSignInOptions)

        // Launch Google Sign In
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(account : GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        signInToFirebase(credential)
    }

    // Firebase authentication
    private fun signInToFirebase(credential: AuthCredential){
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    goMainActivity()
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Hey, you're not connected", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)

            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Toast.makeText(
                    this@LandingPageActivity,
                    "Google login failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }
}