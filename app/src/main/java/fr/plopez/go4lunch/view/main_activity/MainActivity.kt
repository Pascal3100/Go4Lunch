package fr.plopez.go4lunch.view.main_activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.ViewModelFactory
import fr.plopez.go4lunch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        val container :Int = R.id.activity_main_fragment_container
        val viewModel = ViewModelProvider(this,ViewModelFactory.INSTANCE)
            .get(MainActivityViewModel::class.java)

        setContentView(binding.root)

        // Removes the titleBar
        supportActionBar?.hide()

        println("##### At init item id = "+binding.bottomNavigation.selectedItemId)
        // Manage to always make corresponding selected icon and view after rotation or reload
        viewModel.getActivePageLiveData().observe(this, Observer {
            when(it){
                0 -> {
                    binding.bottomNavigation.selectedItemId = R.id.map_view_page
                    fragmentReplacer(container, GoogleMapsViewFragment())
                }
                R.id.map_view_page -> fragmentReplacer(container, GoogleMapsViewFragment())
                R.id.list_view_page -> fragmentReplacer(container, ListViewRestaurantFragment())
                R.id.workmates_view_page -> fragmentReplacer(container, ListWorkmatesFragment())
            }
        })

        // Load the corresponding fragment to the bottom nav icon clicked
        binding.bottomNavigation.setOnItemSelectedListener{
            when (it.itemId) {
                R.id.map_view_page -> {
//                    fragmentReplacer(container, GoogleMapsViewFragment())
                    viewModel.setActivePage(it.itemId)
                    return@setOnItemSelectedListener true
                }
                R.id.list_view_page -> {
//                    fragmentReplacer(container, ListViewRestaurantFragment())
                    viewModel.setActivePage(it.itemId)
                    return@setOnItemSelectedListener true
                }
                R.id.workmates_view_page -> {
//                    fragmentReplacer(container, ListWorkmatesFragment())
                    viewModel.setActivePage(it.itemId)
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

    private fun fragmentReplacer(container:Int, fragment: Fragment){
        supportFragmentManager
            .beginTransaction()
            .replace(container, fragment)
            .commit()
    }
}
