package fr.plopez.go4lunch.view.landing_page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
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
import fr.plopez.go4lunch.interfaces.OnLoginSuccessful
import fr.plopez.go4lunch.interfaces.OnPermissionsAccepted
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.utils.FragmentManager
import fr.plopez.go4lunch.view.main_activity.MainActivity

class LandingPageActivity : AppCompatActivity(R.layout.activity_landing_page), OnLoginSuccessful, OnPermissionsAccepted {

    companion object {
        private val TAG = "LandingPageActivity"
        fun navigate(activity: FragmentActivity): Intent {
            return Intent(activity, LandingPageActivity::class.java)
        }
    }

    // View binding
    private lateinit var binding: ActivityLandingPageBinding

    private lateinit var snack: CustomSnackBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Removes the notification bar
        val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        this.window.setFlags(fullScreenFlag, fullScreenFlag)

        binding = ActivityLandingPageBinding.inflate(layoutInflater)

        setContentView(binding.root)

        snack = CustomSnackBar(findViewById(android.R.id.content), this)

        FragmentManager.replace(this, R.id.landing_page_fragment_container, LoginFragment.newInstance())
    }

    // Navigation utility
    private fun goMainActivity() {
        // TODO : add here an observer to check when user is logged and permissions are accepted
        startActivity(MainActivity.navigate(this))
    }

    // On login successful go to permissions fragment
    override fun onLoginSuccessful(success: Boolean) {
        if (success) {

            // supportFragmentManager.commit()

            FragmentManager.replace(
                this,
                R.id.landing_page_fragment_container,
                PermissionsFragment.newInstance()
            )
        }
    }

    // On permissions accepted go to main activity, else back to login page
    override fun onPermissionsAccepted(accepted: Boolean) {
        if (accepted) {
            goMainActivity()
        } else {
            FragmentManager.replace(this, R.id.landing_page_fragment_container, LoginFragment.newInstance())
            snack.showWarningSnackBar("Please accept permissions to continue")
        }
    }
}