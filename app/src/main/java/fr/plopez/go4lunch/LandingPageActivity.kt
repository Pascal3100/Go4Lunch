package fr.plopez.go4lunch

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class LandingPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Removes the notification bar
        val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        this.window.setFlags(fullScreenFlag, fullScreenFlag)

        // Removes the titleBar
        supportActionBar?.hide()

        setContentView(R.layout.activity_landing_page)
    }
}