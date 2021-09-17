package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.data.model.restaurant.Restaurant
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class GoogleMapsViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val restaurantsRepository: RestaurantsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private val TAG = "Google Maps ViewModel"
    }

    // Position flow
    private val googleMapViewStateMutableSharedFlow =
        MutableSharedFlow<GoogleMapViewState>(replay = 1)
    val googleMapViewSharedFlow = googleMapViewStateMutableSharedFlow.asSharedFlow()

    // Restaurants list flow
    private val listRestaurantViewStateMutableStateFlow =
        MutableStateFlow(emptyList<RestaurantViewState>())
    val listRestaurantViewStateFlow = listRestaurantViewStateMutableStateFlow.asStateFlow()

    private val onMapReadyMutableStateFlow = MutableStateFlow(false)

    val responseStatusStateFlow = restaurantsRepository.responseStatusStateFlow

    init {
        viewModelScope.launch {

            onMapReadyMutableStateFlow.combine(locationRepository.fetchUpdates()){
                    isMapReady, positionWithZoom ->
                // Send an empty flow is map not ready
                if (!isMapReady) flowOf<GoogleMapViewState>()

                // Emit the current position to the view
                // Todo @Nino Map???
                googleMapViewStateMutableSharedFlow.emit(
                    GoogleMapViewState(
                        positionWithZoom.latitude,
                        positionWithZoom.longitude,
                        positionWithZoom.zoom
                    )
                )

                // Send a retrofit request to fetch restaurants around received position
                restaurantsRepository.getRestaurantsAroundPosition(
                    positionWithZoom.latitude.toString(),
                    positionWithZoom.longitude.toString(),
                    1000.toString()
                )

            }.map {
                restaurantMapper(it)
            }.collect {
                listRestaurantViewStateMutableStateFlow.value = it
            }
        }
    }

    fun onMapReady() {
        onMapReadyMutableStateFlow.value = true
    }

    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    private fun restaurantMapper(restaurantList: List<Restaurant>): List<RestaurantViewState>{
        var restaurantViewStateMutableList = mutableListOf<RestaurantViewState>()

        restaurantList.forEach {
            restaurantViewStateMutableList.add(
                RestaurantViewState(
                    it.geometry.location.lat,
                    it.geometry.location.lng,
                    it.name
                )
            )
        }

        return Collections.unmodifiableList(restaurantViewStateMutableList)
    }


    // Data Class to emit to the UI
    data class GoogleMapViewState(
        val latitude: Double,
        val longitude: Double,
        val zoom: Float,
    )

    // Data Class to emit to the UI
    data class RestaurantViewState(
        val latitude: Double,
        val longitude: Double,
        val name: String
    )
}