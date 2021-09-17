package fr.plopez.go4lunch.view.landing_page

import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityLandingPageBinding
import fr.plopez.go4lunch.interfaces.OnLoginSuccessful
import fr.plopez.go4lunch.interfaces.OnPermissionsAccepted
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.view.main_activity.MainActivity

@AndroidEntryPoint
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
    override fun onLoginSuccessful(success: Boolean) {
        if (success) {
            // supportFragmentManager.commit()
            supportFragmentManager.commit {
                replace(R.id.landing_page_fragment_container, PermissionsFragment.newInstance())
                setReorderingAllowed(true)
            }
        }
    }

    // On permissions accepted go to main activity, else back to login page
    override fun onPermissionsAccepted(accepted: Boolean) {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager?
        if (accepted && manager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            goMainActivity()
        } else {
            supportFragmentManager.commit {
                replace(R.id.landing_page_fragment_container, LoginFragment.newInstance())
                setReorderingAllowed(true)
            }
            snack.showWarningSnackBar(getString(R.string.accept_permissions_message))
        }
    }

    override fun onGPSActivationRequest(intent: Intent) {
        startActivity(intent)
    }
}