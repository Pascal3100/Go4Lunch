package fr.plopez.go4lunch.view.main_activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.ViewModelFactory
import fr.plopez.go4lunch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // Manage first time main activity is loaded
        if (savedInstanceState == null){
            setActivePage(R.id.map_view_page)
            binding.mainActivityTopAppbar.title = resources.getString(R.string.maps_view_appbar_title)
        } else {
            binding.mainActivityTopAppbar.title =savedInstanceState.getCharSequence("appBarCurTitle")
        }

        // Load the corresponding fragment to the bottom nav icon clicked
        binding.bottomNavigation.setOnItemSelectedListener{
            when (it.itemId) {
                R.id.map_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title = resources.getString(R.string.maps_view_appbar_title)
                    return@setOnItemSelectedListener true
                }
                R.id.list_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title = resources.getString(R.string.list_view_appbar_title)
                    return@setOnItemSelectedListener true
                }
                R.id.workmates_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title = resources.getString(R.string.workmates_view_appbar_title)
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

    // Set the fragment to replace in view
    private fun setActivePage(@IdRes itemId : Int){
        val container = binding.activityMainFragmentContainer.id

        when(itemId){
            R.id.map_view_page -> fragmentReplacer(container, GoogleMapsViewFragment())
            R.id.list_view_page -> fragmentReplacer(container, ListViewRestaurantFragment())
            R.id.workmates_view_page -> fragmentReplacer(container, ListWorkmatesFragment())
        }
    }

    // Manage the fragment replacement
    private fun fragmentReplacer(container:Int, fragment: Fragment){
        supportFragmentManager
            .beginTransaction()
            .replace(container, fragment)
            .commit()
    }

    // Manage to save current page title to restore it after rotation
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("appBarCurTitle", binding.mainActivityTopAppbar.title.toString())
    }
}
