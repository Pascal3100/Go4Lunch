package fr.plopez.go4lunch.view.main_activity.list_restaurants

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.data.di.CoroutinesProvider
import fr.plopez.go4lunch.data.di.BuildConfigProvider
import fr.plopez.go4lunch.utils.DateTimeUtils
import fr.plopez.go4lunch.view.main_activity.SearchUseCase
import fr.plopez.go4lunch.view.model.RestaurantItemViewState
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.round

@ExperimentalCoroutinesApi
@FlowPreview
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class ListRestaurantsViewModel @Inject constructor(
    private val restaurantsRepository: RestaurantsRepository,
    private val nearbyConstants: BuildConfigProvider,
    private val searchUseCase: SearchUseCase,
    coroutinesProvider: CoroutinesProvider,
    private val dateTimeUtils: DateTimeUtils,
    private val firestoreRepository: FirestoreRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val MAX_WIDTH = "1080"
    }

    val restaurantsItemsLiveData: LiveData<List<RestaurantItemViewState>>

    init {
        restaurantsItemsLiveData = liveData(coroutinesProvider.ioCoroutineDispatcher) {
            // Update the restaurant list and expose the view State
            restaurantsRepository.lastRequestTimeStampSharedFlow.flatMapLatest { requestTimeStamp ->
                combine(
                    firestoreRepository.getWorkmatesWithSelectedRestaurants(),
                    searchUseCase.getSearchResult()
                ) { workmatesWithSelectedRestaurantsList, searchResultStatus ->
                    val restaurantsWithOpeningPeriods =
                        restaurantsRepository.getRestaurantsWithOpeningPeriods(requestedTimeStamp = requestTimeStamp)
                    mapToViewState(
                        restaurantsWithOpeningPeriods =
                        if (searchResultStatus is SearchUseCase.SearchResultStatus.SearchResult) {
                            restaurantsWithOpeningPeriods.filter {
                                it.restaurant.restaurantId in searchResultStatus.data }
                        } else {
                            restaurantsWithOpeningPeriods
                        },
                        restaurantsQuery = restaurantsRepository.getPositionForTimestamp(timestamp = requestTimeStamp),
                        workmatesWithSelectedRestaurantsList = workmatesWithSelectedRestaurantsList
                    )
                }
            }.collect {
                emit(it)
            }
        }
    }

    private fun mapToViewState(
        restaurantsWithOpeningPeriods: List<RestaurantWithOpeningPeriods>,
        restaurantsQuery: RestaurantsQuery,
        workmatesWithSelectedRestaurantsList: List<WorkmateWithSelectedRestaurant>
    ): List<RestaurantItemViewState> {

        if (restaurantsWithOpeningPeriods.isEmpty()) emptyList<RestaurantItemViewState>()

        return restaurantsWithOpeningPeriods.map { restaurantWithOpeningPeriod ->

            val numberOfInterestedWorkmates = workmatesWithSelectedRestaurantsList.filter {
                it.selectedRestaurantId == restaurantWithOpeningPeriod.restaurant.restaurantId
            }.size.toString()

            RestaurantItemViewState(
                name = restaurantWithOpeningPeriod.restaurant.name,
                address = restaurantWithOpeningPeriod.restaurant.address,
                openingStateText = createOpeningStateText(restaurantWithOpeningPeriod.openingHours),
                distanceToUser = getDistanceToUser(
                    restaurantWithOpeningPeriod.restaurant.latitude,
                    restaurantWithOpeningPeriod.restaurant.longitude,
                    restaurantsQuery.latitude,
                    restaurantsQuery.longitude
                ),
                numberOfInterestedWorkmates = numberOfInterestedWorkmates,
                rate = restaurantWithOpeningPeriod.restaurant.rate,
                photoUrl = mapRestaurantPhotoUrl(restaurantWithOpeningPeriod.restaurant.photoUrl),
                id = restaurantWithOpeningPeriod.restaurant.restaurantId
            )
        }
    }

    // Retrieve photo Url per restaurant
    private fun mapRestaurantPhotoUrl(photoReference: String?): String {
        return if (photoReference != null && photoReference != "") {
            context.resources.getString(
                R.string.place_photo_api_url,
                MAX_WIDTH,
                photoReference,
                nearbyConstants.key
            )
        } else {
            ""
        }
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

        return context.resources.getString(R.string.distance_unit, userDistance.toString())
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
            return context.resources.getString(R.string.no_available_hours)
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
                context.resources.getString(R.string.open_at_text, opening)
                // in opening period case
            } else if (currentTime.isAfter(opening) && currentTime.isBefore(closing)) {
                context.resources.getString(R.string.open_until_text, closing)
            } else null
        }.first()
    }
}