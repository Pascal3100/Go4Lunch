package fr.plopez.go4lunch.view.landing_page

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import fr.plopez.go4lunch.R

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
        val manualButton = findViewById<Button>(R.id.landing_page_manual_login_button)
        val manualGroup = findViewById<Group>(R.id.landing_page_manual_login_group)

        manualButton.setOnClickListener {
            TransitionManager.beginDelayedTransition(layout)

            manualGroup.isVisible = true
            it.isVisible = false
        }
    }
}