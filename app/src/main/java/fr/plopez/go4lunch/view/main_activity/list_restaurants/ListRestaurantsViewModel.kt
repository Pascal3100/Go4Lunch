package fr.plopez.go4lunch.view.main_activity.list_restaurants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.model.restaurant.RestaurantItemViewState
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ListRestaurantsViewModel @Inject constructor(
    private val restaurantsRepository: RestaurantsRepository,

    ) : ViewModel() {

    init {
        viewModelScope.launch {
            restaurantsRepository.lastRequestTimeStampSharedFlow.map {
                map(restaurantsRepository.getRestaurantsWithOpeningPeriods(it))
            }.collect {

            }
        }
    }

    private fun map(
        restaurantsWithOpeningPeriods: List<RestaurantWithOpeningPeriods>
    ): List<RestaurantItemViewState> {
        if (restaurantsWithOpeningPeriods.isEmpty()) emptyList<RestaurantItemViewState>()

        val restaurantItemViewStateList = mutableListOf<RestaurantItemViewState>()

        restaurantsWithOpeningPeriods.forEach{

            restaurantItemViewStateList.add(
                RestaurantItemViewState(
                    it.restaurant.name,
                    it.restaurant.address,
                    createOpeningStateText(it.openingHours),
                    getDistanceToUser(it.restaurant.latitude, it.restaurant.longitude),
                    getInterestedWormates(it.restaurant.name),
                    it.restaurant.rate.toString(),
                    it.restaurant.photoUrl.toString()
                )
            )
        }

        return restaurantItemViewStateList
    }

    // Build the restaurant opening hour string
    private fun createOpeningStateText(openingHours: List<RestaurantOpeningPeriod>): String {
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val currentDay = Calendar.DAY_OF_WEEK
        openingHours.forEach{
            when (it.dayOfWeek){
                Calendar.DAY_OF_WEEK -> {
                    if (LocalTime.parse(it.openingHour).isBefore(currentTime) && )
                }
            }
        }
    }


}