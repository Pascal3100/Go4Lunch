package fr.plopez.go4lunch.view.restaurant_details

import android.annotation.SuppressLint
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
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel.RestaurantDetailsViewAction.FirestoreFails
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


    @SuppressLint("UseCompatLoadingForDrawables")
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
            binding.restaurantDetailsActivityRestaurantRatingBar.rating =restaurantDetailsViewState.rate
            binding.restaurantDetailsActivitySubtitle.text = restaurantDetailsViewState.address

            // modifier for FAB
            binding.selectRestaurant.setImageDrawable(
                if (restaurantDetailsViewState.isSelected) {
                    resources.getDrawable(R.drawable.selected)
                } else {
                    resources.getDrawable(R.drawable.not_selected)
                }
            )

            // listener for call button
            binding.phoneButton.setOnClickListener {
                Log.d("TAG", "phoneButton")
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:" + restaurantDetailsViewState.phoneNumber)
                startActivity(dialIntent)
            }

            // modifier for like button
            binding.ratingButton.setCompoundDrawablesWithIntrinsicBounds(
                null,
                resources.getDrawable(
                    if (restaurantDetailsViewState.isFavorite) {
                        R.drawable.ic_baseline_star_rate_24
                    } else {
                        R.drawable.ic_baseline_empty_star_24
                    }
                ),
                null,
                null
            )

            // listener for website button
            binding.websiteButton.setOnClickListener {
                Log.d("TAG", "websiteButton")
                val websiteIntent = Intent(Intent.ACTION_VIEW)
                websiteIntent.data = Uri.parse(restaurantDetailsViewState.website)
                startActivity(websiteIntent)
            }
        }

        // listener for like button
        binding.ratingButton.setOnClickListener {
            restaurantDetailsViewModel.onLike()
        }

        // listener for FAB
        binding.selectRestaurant.setOnClickListener {
            restaurantDetailsViewModel.onSelectRestaurant()
        }

        // listener for back navigation
        binding.restaurantDetailsActivityToolbar.setNavigationOnClickListener {
            finish()
        }

        // listener for messages
        restaurantDetailsViewModel.firestoreStateLiveData.observe(this) {
            when (it) {
                is FirestoreFails -> CustomSnackBar.with(binding.root)
                    .setMessage(getString(R.string.firestore_fails_message))
                    .setType(CustomSnackBar.Type.ERROR)
                    .build()
                    .show()
            }
        }


    }
}