package fr.plopez.go4lunch.main_activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.OnMapReadyCallback
import fr.plopez.go4lunch.R

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Removes the titleBar
        supportActionBar?.hide()

        // Load the fragment
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.fragment_main_activity_container, MainActivityFragment.ne)
    }

}