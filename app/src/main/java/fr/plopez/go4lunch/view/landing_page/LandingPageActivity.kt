package fr.plopez.go4lunch.view.landing_page

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityLandingPageBinding
import fr.plopez.go4lunch.view.main_activity.MainActivity

class LandingPageActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLandingPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLandingPageBinding.inflate(layoutInflater)

        // Removes the notification bar
        val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        this.window.setFlags(fullScreenFlag, fullScreenFlag)

        setContentView(binding.root)

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

        // For dev purpose only
        binding.landingPageFacebookLoginButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}