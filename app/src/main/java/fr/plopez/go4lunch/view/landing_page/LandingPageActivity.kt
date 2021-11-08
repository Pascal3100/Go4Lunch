package fr.plopez.go4lunch.view.landing_page

import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityLandingPageBinding
import fr.plopez.go4lunch.interfaces.OnLoginSuccessful
import fr.plopez.go4lunch.interfaces.OnPermissionsAccepted
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.view.main_activity.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class LandingPageActivity : AppCompatActivity(R.layout.activity_landing_page), OnLoginSuccessful,
    OnPermissionsAccepted {

    companion object {
        private val TAG = "LandingPageActivity"
        fun navigate(activity: FragmentActivity): Intent {
            return Intent(activity, LandingPageActivity::class.java)
        }
    }

    // ViewModel provided by delegate
    private val landingPageViewModel by viewModels<LandingPageViewModel>()

    // View binding
    private lateinit var binding: ActivityLandingPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Removes the notification bar
        val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        this.window.setFlags(fullScreenFlag, fullScreenFlag)

        binding = ActivityLandingPageBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // go to login
        supportFragmentManager.commit {
            replace(R.id.landing_page_fragment_container, LoginFragment.newInstance())
            setReorderingAllowed(true)
        }
    }

    // Navigation utility
    private fun goMainActivity() {
        startActivity(MainActivity.navigate(this))
    }

    // On login successful go to permissions fragment
    override fun onLoginSuccessful() {
        // Notify ViewModel user is successfully logged in
        landingPageViewModel.onLoginSuccessful(Firebase.auth.currentUser!!)

        // Go to permissions
        supportFragmentManager.commit {
            replace(R.id.landing_page_fragment_container, PermissionsFragment.newInstance())
            setReorderingAllowed(true)
        }
    }

    // On permissions accepted go to main activity, else back to login page
    override fun onPermissionsAccepted(accepted: Boolean) {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager?
        if (accepted && manager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Go to main activity if all permissions accepted
            goMainActivity()
        } else {
            // Go back to login if permissions not accepted
            Firebase.auth.signOut()
            supportFragmentManager.commit {
                replace(R.id.landing_page_fragment_container, LoginFragment.newInstance())
                setReorderingAllowed(true)
            }

            CustomSnackBar.with(binding.root)
                .setMessage(getString(R.string.accept_permissions_message))
                .setType(CustomSnackBar.Type.WARNING)
                .build()
                .show()
        }
    }

    override fun onGPSActivationRequest(intent: Intent) {
        startActivity(intent)
    }
}