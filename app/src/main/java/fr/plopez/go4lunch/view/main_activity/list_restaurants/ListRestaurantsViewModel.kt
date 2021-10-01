package fr.plopez.go4lunch.view.main_activity.list_restaurants

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.RestaurantItemViewState
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.NearbyConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class ListRestaurantsViewModel @Inject constructor(
    private val restaurantsRepository: RestaurantsRepository,
    private val locationRepository: LocationRepository,
    private val nearbyConstants: NearbyConstants,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val MAX_WIDTH = "1080"
    }

    private val restaurantsItemsMutableStateFlow =
        MutableStateFlow(emptyList<RestaurantItemViewState>())
    val restaurantsItemsStateFlow = restaurantsItemsMutableStateFlow.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                restaurantsRepository.lastRequestTimeStampSharedFlow,
                locationRepository.fetchUpdates()
            ) { timeStamp, positionWithZoom ->
                map(
                    restaurantsRepository.getRestaurantsWithOpeningPeriods(timeStamp),
                    positionWithZoom
                )
            }.collect {
                restaurantsItemsMutableStateFlow.emit(it)
            }
        }
    }

    private fun map(
        restaurantsWithOpeningPeriods: List<RestaurantWithOpeningPeriods>,
        positionWithZoom: LocationRepository.PositionWithZoom
    ): List<RestaurantItemViewState> {
        if (restaurantsWithOpeningPeriods.isEmpty()) emptyList<RestaurantItemViewState>()

        val restaurantItemViewStateList = mutableListOf<RestaurantItemViewState>()

        restaurantsWithOpeningPeriods.forEach {

            restaurantItemViewStateList.add(
                RestaurantItemViewState(
                    it.restaurant.name,
                    it.restaurant.address,
                    createOpeningStateText(it.openingHours),
                    getDistanceToUser(
                        it.restaurant.latitude,
                        it.restaurant.longitude,
                        positionWithZoom.latitude,
                        positionWithZoom.longitude
                    ),
                    getInterestedWorkmates(it.restaurant.name),
                    it.restaurant.rate,
                    mapRestaurantPhotoUrl(it.restaurant.photoUrl)
                )
            )
        }

        return restaurantItemViewStateList
    }

    // Retrieve photo Url per restaurant
    private fun mapRestaurantPhotoUrl(photoReference: String?): String {
        return (if (photoReference != null) {
            "https://maps.googleapis.com/maps/api/place/photo?" +
                    "maxwidth=${MAX_WIDTH}&" +
                    "photoreference=$photoReference&" +
                    "key=${nearbyConstants.key};"
        } else {
            ""
        })
    }

    // TODO how to do this??? surely another Flow buddy
    private fun getInterestedWorkmates(restaurantName: String): String {
        return "0"
    }

    private fun getDistanceToUser(
        restaurantLatitude: Double,
        restaurantLongitude: Double,
        userLatitude: Double,
        userLongitude: Double
    ): String {
        val restaurantLocation = Location("restaurant_position")
        restaurantLocation.latitude = restaurantLatitude
        restaurantLocation.longitude = restaurantLongitude

        val userLocation = Location("user_position")
        userLocation.latitude = userLatitude
        userLocation.longitude = userLongitude

        return "${restaurantLocation.distanceTo(userLocation)}+${context.resources.getString(R.string.distance_unit)}"
    }

    // Build the restaurant opening hour string
    private fun createOpeningStateText(
        openingHours: List<RestaurantOpeningPeriod>
    ): String {
        val currentTime = LocalTime.now()
        val listOfCandidates = mutableListOf<RestaurantOpeningPeriod>()

        // get all opening periods of the current day
        openingHours.forEach {
            when (it.dayOfWeek) {
                Calendar.DAY_OF_WEEK -> {
                    listOfCandidates.add(it)
                }
            }
        }

        // Sort them
        listOfCandidates.sortBy { it.periodId }

        // check for extreme last hour
        if (LocalTime.parse(listOfCandidates.last().closingHour).isBefore(currentTime))
            return context.resources.getString(R.string.closed_text)

        var openingPeriodText = ""

        // Find the good one for comparison
        listOfCandidates.forEach {
            val opening = LocalTime.parse(it.openingHour)

            // before opening case
            if (currentTime.isBefore(opening))
                openingPeriodText = "${context.resources.getString(R.string.open_at_text)}+$opening"

            val closing = LocalTime.parse(it.closingHour)

            // in opening period case
            if (currentTime.isAfter(opening) && currentTime.isBefore(closing))
                openingPeriodText =
                    "${context.resources.getString(R.string.open_until_text)}+$closing"
        }

        return openingPeriodText
    }
}