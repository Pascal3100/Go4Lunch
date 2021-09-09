package fr.plopez.go4lunch.view.main_activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityMainBinding
import fr.plopez.go4lunch.utils.FragmentManager
import fr.plopez.go4lunch.view.landing_page.LandingPageActivity

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        fun navigate(activity: FragmentActivity): Intent {
            return Intent(activity, MainActivity::class.java)
        }
    }

    private lateinit var binding: ActivityMainBinding

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val firebaseAuthListener = FirebaseAuth.AuthStateListener {
        if (it.currentUser == null) {
            startActivity(LandingPageActivity.navigate(this))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // Identify the top app bar as the action bar to allow its customization
        setSupportActionBar(binding.mainActivityTopAppbar)

        // Manage first time main activity is loaded
        if (savedInstanceState == null) {
            setActivePage(R.id.map_view_page)
            binding.mainActivityTopAppbar.title =
                resources.getString(R.string.maps_view_appbar_title)
        } else {
            binding.mainActivityTopAppbar.title =
                savedInstanceState.getCharSequence("appBarCurTitle")
        }

        // Load the corresponding fragment to the bottom nav icon clicked
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.map_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title =
                        resources.getString(R.string.maps_view_appbar_title)
                    return@setOnItemSelectedListener true
                }
                R.id.list_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title =
                        resources.getString(R.string.list_view_appbar_title)
                    return@setOnItemSelectedListener true
                }
                R.id.workmates_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title =
                        resources.getString(R.string.workmates_view_appbar_title)
                    return@setOnItemSelectedListener true
                }
            }
            false
        }

        binding.mainActivityNavigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.my_selected_restaurant -> {
                    // TODO : implementation to add
                    true
                }
                R.id.my_settings -> {
                    // TODO : implementation to add
                    true
                }
                R.id.logout -> {
                    // Logout
                    firebaseAuth.signOut()
                    firebaseAuth.currentUser
                    true
                }
                else -> {
                    super.onOptionsItemSelected(item)
                }
            }
        }

        binding.mainActivityTopAppbar.setNavigationOnClickListener {
            binding.mainActivityDrawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if user is not logged, go to login page
        firebaseAuth.addAuthStateListener(firebaseAuthListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(firebaseAuthListener)
    }

    // Manage search menu customization
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu manually
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.top_app_bar_menu, menu)

        val menuItemSearch = menu?.findItem(R.id.search)

        val lightGreyColor = ResourcesCompat.getColor(resources, R.color.light_grey, null)

        // Set the search icon to light grey
        val searchButton =
            menuItemSearch?.actionView?.findViewById<AppCompatImageView>(R.id.search_button)
        searchButton?.setColorFilter(lightGreyColor)

        // customize search view
        val searchBar =
            menuItemSearch?.actionView?.findViewById<AutoCompleteTextView>(R.id.search_src_text)
        searchBar?.setTextColor(lightGreyColor)
        searchBar?.textSize = 14F

        // TODO : find a way to tint search icon of search view to lightGrey Color - this does not work...
        val searchIcon =
            menuItemSearch?.actionView?.findViewById<AppCompatImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon?.setColorFilter(lightGreyColor)

        // Recolor the close btn
        val searchPlate = menuItemSearch?.actionView?.findViewById<LinearLayout>(R.id.search_plate)
        val closeIcon = searchPlate?.findViewById<AppCompatImageView>(R.id.search_close_btn)
        closeIcon?.setColorFilter(lightGreyColor)

        return true
    }

    // Set the fragment to replace in view
    private fun setActivePage(@IdRes itemId: Int) {
        val container = binding.activityMainFragmentContainer.id

        when (itemId) {
            R.id.map_view_page -> FragmentManager.replace(
                this,
                container,
                GoogleMapsFragment.newInstance()
            )
            R.id.list_view_page -> FragmentManager.replace(
                this,
                container,
                ListViewRestaurantFragment.newInstance()
            )
            R.id.workmates_view_page -> FragmentManager.replace(
                this,
                container,
                ListWorkmatesFragment.newInstance()
            )
        }
    }

    // Manage to save current page title to restore it after rotation
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("appBarCurTitle", binding.mainActivityTopAppbar.title.toString())
    }
}
