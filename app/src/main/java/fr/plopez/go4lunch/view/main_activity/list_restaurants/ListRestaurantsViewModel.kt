package fr.plopez.go4lunch.view.main_activity.list_restaurants

import android.content.Context
import android.location.Location
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.view.model.RestaurantItemViewState
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.utils.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.round

@ExperimentalCoroutinesApi
@HiltViewModel
class ListRestaurantsViewModel @Inject constructor(
    private val restaurantsRepository: RestaurantsRepository,
    private val nearbyConstants: NearbyConstants,
    coroutinesProvider: CoroutinesProvider,
    private val dateTimeUtils: DateTimeUtils,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val MAX_WIDTH = "1080"
    }

    private val restaurantsItemsMutableStateFlow =
        MutableStateFlow(emptyList<RestaurantItemViewState>())
    val restaurantsItemsLiveData = restaurantsItemsMutableStateFlow.asLiveData(
        coroutinesProvider.ioCoroutineDispatcher
    )

    init {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            // Update the restaurant list and expose the view State
            restaurantsRepository.lastRequestTimeStampSharedFlow.collect {

                restaurantsItemsMutableStateFlow.value = mapToViewState(
                    restaurantsRepository.getRestaurantsWithOpeningPeriods(it),
                    restaurantsRepository.getPositionForTimestamp(it)
                )
            }
        }
    }

    private fun mapToViewState(
        restaurantsWithOpeningPeriods: List<RestaurantWithOpeningPeriods>,
        restaurantsQuery: RestaurantsQuery
    ): List<RestaurantItemViewState> {
        if (restaurantsWithOpeningPeriods.isEmpty()) emptyList<RestaurantItemViewState>()

        return restaurantsWithOpeningPeriods.map {

            RestaurantItemViewState(
                it.restaurant.name,
                it.restaurant.address,
                createOpeningStateText(it.openingHours),
                getDistanceToUser(
                    it.restaurant.latitude,
                    it.restaurant.longitude,
                    restaurantsQuery.latitude,
                    restaurantsQuery.longitude
                ),
                getInterestedWorkmates(it.restaurant.name),
                it.restaurant.rate,
                mapRestaurantPhotoUrl(it.restaurant.photoUrl),
                it.restaurant.restaurantId
            )
        }
    }

    // Retrieve photo Url per restaurant
    private fun mapRestaurantPhotoUrl(photoReference: String?): String {
        return if (photoReference != null && photoReference != "") {
            context.resources.getString(R.string.place_photo_api_url) +
                    "maxwidth=${MAX_WIDTH}&" +
                    "photoreference=$photoReference&" +
                    "key=${nearbyConstants.key}"
        } else {
            ""
        }
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

        val userDistance = round(restaurantLocation.distanceTo(userLocation)).toInt()

        return "${userDistance}${context.resources.getString(R.string.distance_unit)}"
    }

    // Build the restaurant opening hour string
    private fun createOpeningStateText(
        openingHours: List<RestaurantOpeningPeriod>
    ): String {

        var day = dateTimeUtils.getCurrentDay()

        // Manage the difference of day index system between Place Details and Calendar
        if (day > 1) {
            day -= 1
        } else {
            day += 6
        }

        if (openingHours.isEmpty()) {
            return "-"
        }

        val currentTime = dateTimeUtils.getCurrentTime()

        // get all opening periods of the current day
        val listOfCandidates = openingHours.filter { it.dayOfWeek == day }

        if (listOfCandidates.isEmpty())
            return context.resources.getString(R.string.closed_today_text)

        // Sort them
        val sortedListOfCandidates = listOfCandidates.sortedBy { it.periodId.toLong() }

        // check for extreme last hour
        if (LocalTime.parse(sortedListOfCandidates.last().closingHour).isBefore(currentTime))
            return context.resources.getString(R.string.closed_text)

        // Find the good one for comparison
        return sortedListOfCandidates.mapNotNull {
            val opening = LocalTime.parse(it.openingHour)
            val closing = LocalTime.parse(it.closingHour)

            // before opening case
            if (currentTime.isBefore(opening)) {
                "${context.resources.getString(R.string.open_at_text)}$opening"
                // in opening period case
            } else if (currentTime.isAfter(opening) && currentTime.isBefore(closing)) {
                "${context.resources.getString(R.string.open_until_text)}$closing"
            } else null
        }.first()
    }
}