package fr.plopez.go4lunch.view.main_activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityMainBinding
import fr.plopez.go4lunch.view.interfaces.OnClickRestaurantListener
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.utils.FirebaseAuthUtils
import fr.plopez.go4lunch.utils.exhaustive
import fr.plopez.go4lunch.view.landing_page.LandingPageActivity
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel.MainActivityViewAction.NoRestaurantSelected
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel.MainActivityViewAction.SelectedRestaurant
import fr.plopez.go4lunch.view.main_activity.google_maps.GoogleMapsFragment
import fr.plopez.go4lunch.view.main_activity.list_restaurants.ListViewRestaurantFragment
import fr.plopez.go4lunch.view.main_activity.list_workmates.ListWorkmatesFragment
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@AndroidEntryPoint
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(R.layout.activity_main), OnClickRestaurantListener {

    companion object {
        fun navigate(activity: FragmentActivity): Intent {
            return Intent(activity, MainActivity::class.java)
        }

        private const val PLACE_ID = "PLACE_ID"
        private const val APP_BAR_CUR_TITLE = "appBarCurTitle"
    }

    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private val firebaseAuthUtils = FirebaseAuthUtils()

    private val firebaseAuth = Firebase.auth

    private val firebaseAuthListener = FirebaseAuth.AuthStateListener {
        if (firebaseAuthUtils.isFirebaseUserNotNull(it.currentUser)) {
            startActivity(LandingPageActivity.navigate(this))
        }
    }

    private val user = firebaseAuthUtils.getUser()

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
        setupBottomNavigationMenu()

        // Update of side menu header items
        updateSideMenuItems()

        // Side menu listener
        setupSideMenu()

        // Listener for my_lunch
        setupMyLunchAction()
    }

    private fun setupMyLunchAction() {
        mainActivityViewModel.selectedRestaurantIdLiveData.observe(this) {
            if (binding.mainActivityDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                when (it) {
                    is NoRestaurantSelected ->
                        CustomSnackBar.with(binding.root)
                            .setMessage(getString(R.string.restaurant_not_yet_selected))
                            .setType(CustomSnackBar.Type.WARNING)
                            .build()
                            .show()
                    is SelectedRestaurant ->
                        onClickRestaurant(it.id)
                }.exhaustive
            }
        }
    }

    private fun setupSideMenu() {
        binding.mainActivityNavigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.my_selected_restaurant -> {
                    mainActivityViewModel.onWantToSeeMyRestaurant()
                    true
                }
                R.id.my_settings -> {
                    SettingsDialogFragment.newInstance()
                        .show(supportFragmentManager, "")

                    true
                }
                R.id.logout -> {
                    // Logout
                    // TODO @Nino : qd delog et relog avec un nouvel utilisateur, le user ne change pas...
                    firebaseAuth.signOut()
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

    private fun updateSideMenuItems() {
        val headerView = binding.mainActivityNavigationView.getHeaderView(0)
        val avatar = headerView.findViewById<ImageView>(R.id.drawer_user_avatar)
        val name = headerView.findViewById<TextView>(R.id.drawer_user_name)
        val email = headerView.findViewById<TextView>(R.id.drawer_user_email)

        name.text = user.name
        email.text = user.email
        Glide.with(this)
            .load(user.photoUrl)
            .placeholder(R.drawable.ic_no_profile_photo_available)
            .error(R.drawable.ic_no_profile_photo_available)
            .fallback(R.drawable.ic_no_profile_photo_available)
            .circleCrop()
            .into(avatar)
    }

    private fun setupBottomNavigationMenu() {
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.map_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title =
                        resources.getString(R.string.maps_view_appbar_title)
                    mainActivityViewModel.isWorkmatesView(false)
                    return@setOnItemSelectedListener true
                }
                R.id.list_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title =
                        resources.getString(R.string.list_view_appbar_title)
                    mainActivityViewModel.isWorkmatesView(false)
                    return@setOnItemSelectedListener true
                }
                R.id.workmates_view_page -> {
                    setActivePage(it.itemId)
                    binding.mainActivityTopAppbar.title =
                        resources.getString(R.string.workmates_view_appbar_title)
                    mainActivityViewModel.isWorkmatesView(true)
                    return@setOnItemSelectedListener true
                }
            }
            false
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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu manually
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.top_app_bar_menu, menu)

        val menuItemSearch = menu.findItem(R.id.search)

        val lightGreyColor = ResourcesCompat.getColor(resources, R.color.light_grey, null)

        // Set the search icon to light grey
        val searchButton =
            menuItemSearch.actionView.findViewById<AppCompatImageView>(R.id.search_button)
        searchButton.setColorFilter(lightGreyColor)

        // customize search view
        val searchBar =
            menuItemSearch.actionView.findViewById<AutoCompleteTextView>(R.id.search_src_text)
        searchBar.setTextColor(lightGreyColor)
        searchBar.textSize = 14F

        val searchIcon =
            menuItemSearch.actionView.findViewById<AppCompatImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(lightGreyColor)

        // Recolor the close btn
        val searchPlate = menuItemSearch.actionView.findViewById<LinearLayout>(R.id.search_plate)
        val closeIcon = searchPlate.findViewById<AppCompatImageView>(R.id.search_close_btn)
        closeIcon.setColorFilter(lightGreyColor)

        // setup listener for text query
        val searchView = menuItemSearch.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newSearchText: String?): Boolean {
                // start dealing at 3 letters
                mainActivityViewModel.onSearchTextChange(newSearchText)
                return true
            }
        }
        )

        return true
    }

    // Set the fragment to replace in view
    private fun setActivePage(@IdRes itemId: Int) {
        val container = binding.activityMainFragmentContainer.id

        when (itemId) {
            R.id.map_view_page -> supportFragmentManager.commit {
                replace(container, GoogleMapsFragment.newInstance())
            }
            R.id.list_view_page -> supportFragmentManager.commit {
                replace(container, ListViewRestaurantFragment.newInstance())
            }
            R.id.workmates_view_page -> supportFragmentManager.commit {
                replace(container, ListWorkmatesFragment.newInstance())
            }
        }
    }

    // Manage to save current page title to restore it after rotation
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(APP_BAR_CUR_TITLE, binding.mainActivityTopAppbar.title.toString())
    }

    override fun onClickRestaurant(placeId: String) {
        val intent = RestaurantDetailsActivity.navigate(this)
        intent.putExtra(PLACE_ID, placeId)
        startActivity(intent)
    }
}
