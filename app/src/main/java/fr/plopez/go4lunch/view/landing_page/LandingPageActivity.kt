package fr.plopez.go4lunch.view.landing_page

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityLandingPageBinding

class LandingPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Removes the notification bar
        val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        this.window.setFlags(fullScreenFlag, fullScreenFlag)

        // Removes the titleBar
        supportActionBar?.hide()

        setContentView(R.layout.activity_landing_page)

        val layout = findViewById<ConstraintLayout>(R.id.landing_page_container)
        val button = findViewById<Button>(R.id.landing_page_email_login_button_selection)
        val group = findViewById<androidx.constraintlayout.widget.Group>(R.id.landing_page_email_login_group)

        // Manage loggin with email display
        button.setOnClickListener {

            Log.d("TAG", "##### onCreate() called")
            // Transition to show login with email stuff when button clicked
            TransitionManager.beginDelayedTransition(layout)
            // Clears the selection button
            it.isVisible = false
            group.isVisible = true
        }
    }
}