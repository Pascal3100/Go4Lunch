package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.Restaurant
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
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
    val googleMapViewStateSharedFlow = googleMapViewStateMutableSharedFlow.asSharedFlow()

    // Restaurants list flow
//    private val listRestaurantViewStateMutableStateFlow =
//        MutableStateFlow(
//            GoogleMapViewState(
//                context.resources.getString(R.string.default_lat_value).toDouble(),
//                context.resources.getString(R.string.default_long_value).toDouble(),
//                context.resources.getString(R.string.default_zoom_value).toFloat(),
//                emptyList()
//            )
//        )
//    val listRestaurantViewStateFlow = listRestaurantViewStateMutableStateFlow.asStateFlow()

    private val onMapReadyMutableStateFlow = MutableStateFlow(false)

    val responseStatusStateFlow = restaurantsRepository.responseStatusStateFlow

    init {
        viewModelScope.launch {

            onMapReadyMutableStateFlow.combine(locationRepository.fetchUpdates()) { isMapReady, positionWithZoom ->
                // Send an empty flow is map not ready
                if (!isMapReady) flowOf<GoogleMapViewState>()

                // Emit the current position to the view
                // Todo @Nino Map???
//                googleMapViewStateMutableSharedFlow.emit(
//                    GoogleMapViewState(
//                        positionWithZoom.latitude,
//                        positionWithZoom.longitude,
//                        positionWithZoom.zoom
//                    )
//                )

                // Send a retrofit request to fetch restaurants around received position
                val listRestaurant = restaurantsRepository.getRestaurantsAroundPosition(
                    positionWithZoom.latitude.toString(),
                    positionWithZoom.longitude.toString(),
                    context.resources.getString(R.string.default_detection_radius_value)
                )

                GoogleMapData(
                    positionWithZoom, listRestaurant
                )

            }.map {
                restaurantMapper(it)
            }.collect {
                googleMapViewStateMutableSharedFlow.tryEmit(it)
            }
        }
    }

    fun onMapReady() {
        onMapReadyMutableStateFlow.value = true
    }

    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    private fun restaurantMapper(googleMapData: GoogleMapData): GoogleMapViewState {
        val restaurantViewStateMutableList = mutableListOf<RestaurantViewState>()

        googleMapData.listRestaurant.forEach {
            restaurantViewStateMutableList.add(
                RestaurantViewState(
                    it.geometry.location.lat,
                    it.geometry.location.lng,
                    it.name
                )
            )
        }

        return GoogleMapViewState(
            googleMapData.positionWithZoom.latitude,
            googleMapData.positionWithZoom.longitude,
            googleMapData.positionWithZoom.zoom,
            Collections.unmodifiableList(restaurantViewStateMutableList)
        )
    }

    data class GoogleMapData(
        val positionWithZoom: LocationRepository.PositionWithZoom,
        val listRestaurant: List<Restaurant>
    )

    // Data Class to emit to the UI
    data class GoogleMapViewState(
        val latitude: Double,
        val longitude: Double,
        val zoom: Float,
        val restaurantList: List<RestaurantViewState>
    )

    // Data Class to emit to the UI
    data class RestaurantViewState(
        val latitude: Double,
        val longitude: Double,
        val name: String
    )
}