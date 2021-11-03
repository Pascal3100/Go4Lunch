package fr.plopez.go4lunch.view.restaurant_details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityRestaurantDetailsBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

        restaurantDetailsViewModel.restaurantDetailsViewLiveData.observe(this) { restaurantDetailsViewState ->
            // Glide section
            Glide.with(binding.root)
                .load(restaurantDetailsViewState.photoUrl)
                .placeholder(R.drawable.no_pic_for_item_view)
                .error(R.drawable.no_pic_for_item_view)
                .fallback(R.drawable.no_pic_for_item_view)
                .centerCrop()
                .into(binding.restaurantDetailsActivityRestaurantImage)

            binding.restaurantDetailsActivityRestaurantName.text = restaurantDetailsViewState.name
            binding.restaurantDetailsActivityRestaurantRatingBar.rating = restaurantDetailsViewState.rate
            binding.restaurantDetailsActivitySubtitle.text = restaurantDetailsViewState.address

            // listener for call button
            binding.phoneButton.setOnClickListener {
                Log.d("TAG", "phoneButton")
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:" + restaurantDetailsViewState.phoneNumber)
                startActivity(dialIntent)
            }

            // listener for website button
            binding.websiteButton.setOnClickListener {
                Log.d("TAG", "websiteButton")
                val websiteIntent = Intent(Intent.ACTION_VIEW)
                websiteIntent.data = Uri.parse(restaurantDetailsViewState.website)
                startActivity(websiteIntent)
            }
        }

        // listener for like button state
        // TODO Pascal : à merger avec le viewstate (une seule LiveData d'état), et puis pas de if ! :p
        restaurantDetailsViewModel.likeStateLiveData.observe(this@RestaurantDetailsActivity) {
            if (it) {
                binding.ratingButton.setCompoundDrawables(
                    null,
                    getDrawable(R.drawable.ic_baseline_star_rate_24),
                    null,
                    null
                )
            } else {
                binding.ratingButton.setCompoundDrawables(
                    null,
                    getDrawable(R.drawable.ic_baseline_star_rate_24),
                    null,
                    null
                )
            }
        }

        // listener for like button
        binding.ratingButton.setOnClickListener {
            Log.d("TAG", "ratingButton")
            restaurantDetailsViewModel.onLike()
        }

        // listener for back navigation
        binding.restaurantDetailsActivityToolbar.setNavigationOnClickListener {
            finish()
        }
    }
}