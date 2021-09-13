package fr.plopez.go4lunch.view.restaurant_details

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R

@AndroidEntryPoint
class RestaurantDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_details)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.restaurant_details_container, RestaurantDetailsFragment.newInstance("NOWHERE"))
            .commit()
    }
}