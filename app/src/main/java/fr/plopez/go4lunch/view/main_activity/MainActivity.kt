package fr.plopez.go4lunch.view.main_activity

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
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
        val viewModel = ViewModelProvider(this,ViewModelFactory.INSTANCE)
            .get(MainActivityViewModel::class.java)

        setContentView(binding.root)

        if (savedInstanceState == null) {
            setActivePage(R.id.map_view_page)
        }

        // Removes the titleBar
        setSupportActionBar(binding.mainActivityTopAppbar)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.top_app_bar_menu, menu)

        val menuItemSearch = menu.findItem(R.id.search)

        val searchButton = menuItemSearch.actionView.findViewById<AppCompatImageView>(R.id.search_button)

        searchButton.imageTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.light_grey, null))

        // TODO RAF

        return true
    }

    private fun setActivePage(@IdRes itemId: Int) {
        when(itemId){
            R.id.map_view_page -> fragmentReplacer(binding.activityMainFragmentContainer.id, GoogleMapsViewFragment())
            R.id.list_view_page -> fragmentReplacer(binding.activityMainFragmentContainer.id, ListViewRestaurantFragment())
            R.id.workmates_view_page -> fragmentReplacer(binding.activityMainFragmentContainer.id, ListWorkmatesFragment())
        }
    }

    private fun fragmentReplacer(container:Int, fragment: Fragment){
        supportFragmentManager
            .beginTransaction()
            .replace(container, fragment)
            .commit()
    }
}
