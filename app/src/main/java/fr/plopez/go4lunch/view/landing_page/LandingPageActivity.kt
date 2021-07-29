package fr.plopez.go4lunch.view.landing_page

import android.content.Intent
import android.content.IntentSender
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
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityLandingPageBinding
import fr.plopez.go4lunch.view.main_activity.MainActivity

class LandingPageActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLandingPageBinding

    // Facebook callManager
    private lateinit var callbackManager : CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLandingPageBinding.inflate(layoutInflater)

        // Removes the notification bar
        val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        this.window.setFlags(fullScreenFlag, fullScreenFlag)

        setContentView(binding.root)

        // ------------------- Facebook authentication -------------------
        binding.landingPageFacebookLoginButton.setOnClickListener { loginWithFacebook() }
        // ---------------------------------------------------------------

        val layout = findViewById<ConstraintLayout>(R.id.landing_page_container)
        val button = findViewById<Button>(R.id.landing_page_email_login_button_selection)
        val group = findViewById<androidx.constraintlayout.widget.Group>(R.id.landing_page_email_login_group)

        // Manage login with email display
        button.setOnClickListener {
            // Transition to show login with email stuff when button clicked
            TransitionManager.beginDelayedTransition(layout)
            // Clears the selection button
            it.isVisible = false
            group.isVisible = true
        }
    }


    // FaceBook authentication
    private fun loginWithFacebook(){
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().logInWithReadPermissions(this, setOf("email"))
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                // Access allowed to main activity
                val intent = Intent(this@LandingPageActivity, MainActivity::class.java)
                startActivity(intent)
            }

            override fun onCancel() {
                Toast.makeText(this@LandingPageActivity, "Facebook login cancelled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@LandingPageActivity, "Facebook login failed: ${error.toString()}", Toast.LENGTH_SHORT).show()
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}