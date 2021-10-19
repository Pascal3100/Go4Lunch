package fr.plopez.go4lunch.view.restaurant_details

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityRestaurantDetailsBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class RestaurantDetailsActivity : AppCompatActivity() {

    companion object {
        fun navigate(activity: FragmentActivity): Intent {
            return Intent(activity, RestaurantDetailsActivity::class.java)
        }

        private const val PLACE_ID = "PLACE_ID"
    }

    private lateinit var binding: ActivityRestaurantDetailsBinding
    private val restaurantDetailsViewModel: RestaurantDetailsViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRestaurantDetailsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.restaurantDetailsActivityToolbar)

        val placeId = intent.extras?.getString(PLACE_ID) ?: ""

        restaurantDetailsViewModel.onPlaceIdRequest(placeId)

        restaurantDetailsViewModel.restaurantDetailsViewLiveData.observe(this@RestaurantDetailsActivity) {
            // Glide section
            Glide.with(binding.root)
                .load(it.photoUrl)
                .placeholder(R.drawable.no_pic_for_item_view)
                .error(R.drawable.no_pic_for_item_view)
                .fallback(R.drawable.no_pic_for_item_view)
                .centerCrop()
                .into(binding.restaurantDetailsActivityRestaurantImage)

            binding.restaurantDetailsActivityRestaurantName.text = it.name
            binding.restaurantDetailsActivityRestaurantRatingBar.rating = it.rate
            binding.restaurantDetailsActivitySubtitle.text = it.address
        }

        binding.restaurantDetailsActivityToolbar.setNavigationOnClickListener {
            finish()
        }
    }
}