package fr.plopez.go4lunch.view.restaurant_details

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.NotificationWorker
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.ActivityRestaurantDetailsBinding
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel.RestaurantDetailsViewAction.FirestoreFails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class RestaurantDetailsActivity : AppCompatActivity() {

    companion object {
        fun navigate(activity: FragmentActivity): Intent {
            return Intent(activity, RestaurantDetailsActivity::class.java)
        }

        private const val NOTIFICATION_ID = 666
    }

    private lateinit var binding: ActivityRestaurantDetailsBinding
    private val restaurantDetailsViewModel: RestaurantDetailsViewModel by viewModels()


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRestaurantDetailsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.restaurantDetailsActivityToolbar)

        val adapter = RestaurantDetailsAdapter(this)

        binding.listWorkmatesRecyclerview.adapter = adapter

        restaurantDetailsViewModel.restaurantDetailsViewStateLiveData.observe(this) { restaurantDetailsViewState ->

            updateViewItems(restaurantDetailsViewState, adapter)

            // modifier for FAB
            setupFab(restaurantDetailsViewState)

            // listener for call button
            setupPhoneButton(restaurantDetailsViewState)

            // modifier for like button
            setupLikeButton(restaurantDetailsViewState)

            // listener for website button
            setupWebsiteButton(restaurantDetailsViewState)

            // listener for schedule or unschedule the notification
            scheduleNotification(restaurantDetailsViewState)
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
            if (it is FirestoreFails) {
                CustomSnackBar.with(binding.root)
                    .setMessage(getString(R.string.firestore_fails_message))
                    .setType(CustomSnackBar.Type.ERROR)
                    .build()
                    .show()
            }
        }
    }

    private fun setupWebsiteButton(restaurantDetailsViewState: RestaurantDetailsViewState) {
        binding.websiteButton.setOnClickListener {
            val websiteIntent = Intent(Intent.ACTION_VIEW)
            websiteIntent.data = Uri.parse(restaurantDetailsViewState.website)
            startActivity(websiteIntent)
        }
    }

    private fun setupLikeButton(restaurantDetailsViewState: RestaurantDetailsViewState) {
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
    }

    private fun setupPhoneButton(restaurantDetailsViewState: RestaurantDetailsViewState) {
        binding.phoneButton.setOnClickListener {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:" + restaurantDetailsViewState.phoneNumber)
                startActivity(dialIntent)
            } catch (e: Exception) {
                CustomSnackBar.with(binding.root)
                    .setMessage(getString(R.string.dial_error))
                    .setType(CustomSnackBar.Type.WARNING)
                    .build()
                    .show()
            }
        }
    }

    private fun setupFab(restaurantDetailsViewState: RestaurantDetailsViewState) {
        binding.selectRestaurant.setImageDrawable(
            if (restaurantDetailsViewState.isSelected) {
                resources.getDrawable(R.drawable.selected)
            } else {
                resources.getDrawable(R.drawable.not_selected)
            }
        )
    }

    private fun updateViewItems(
        restaurantDetailsViewState: RestaurantDetailsViewState,
        adapter: RestaurantDetailsAdapter
    ) {
        // Glide section
        Glide.with(binding.root)
            .load(restaurantDetailsViewState.photoUrl)
            .placeholder(R.drawable.no_pic_for_item_view)
            .error(R.drawable.no_pic_for_item_view)
            .fallback(R.drawable.no_pic_for_item_view)
            .centerCrop()
            .into(binding.restaurantDetailsActivityRestaurantImage)

        binding.restaurantDetailsActivityRestaurantName.text = restaurantDetailsViewState.name
        binding.restaurantDetailsActivityRestaurantRatingBar.rating =
            restaurantDetailsViewState.rate
        binding.restaurantDetailsActivitySubtitle.text = restaurantDetailsViewState.address
        adapter.submitList(restaurantDetailsViewState.interestedWorkmatesList)
    }

    private fun scheduleNotification(restaurantDetailsViewState: RestaurantDetailsViewState) {

        val instanceWorkManager = WorkManager.getInstance(this)

        if (restaurantDetailsViewState.isSelected && restaurantDetailsViewState.delay > 0) {
            val data = Data.Builder()
                .putAll(
                    mapOf(
                        NotificationWorker.NOTIFICATION_ID to NOTIFICATION_ID,
                        NotificationWorker.RESTAURANT_ID to restaurantDetailsViewState.id,
                        NotificationWorker.USER_EMAIL to restaurantDetailsViewState.currentUserEmail
                    )
                )
                .build()

            val notificationWork = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                .setInitialDelay(
                    restaurantDetailsViewState.delay,
                    TimeUnit.MILLISECONDS
                ).setInputData(data).build()

            instanceWorkManager.beginUniqueWork(
                NotificationWorker.NOTIFICATION_WORK,
                ExistingWorkPolicy.REPLACE,
                notificationWork
            ).enqueue()
        } else {
            instanceWorkManager.cancelUniqueWork(NotificationWorker.NOTIFICATION_WORK)
        }
    }
}