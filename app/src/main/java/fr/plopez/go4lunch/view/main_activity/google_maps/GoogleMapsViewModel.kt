package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.Restaurant
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
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
    private val googleMapViewStateMutableSharedFlow = MutableSharedFlow<GoogleMapViewState>(replay = 1)
    val googleMapViewStateSharedFlow = googleMapViewStateMutableSharedFlow.asSharedFlow()

    private val googleMapViewActionChannel = Channel<GoogleMapViewAction>(Channel.BUFFERED)
    val googleMapViewActionFlow = googleMapViewActionChannel.receiveAsFlow()


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

    init {
        viewModelScope.launch {

            combine(onMapReadyMutableStateFlow, locationRepository.fetchUpdates()) { isMapReady, positionWithZoom ->
                // Send an empty flow is map not ready
                if (!isMapReady) return@combine null

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
                val restaurantResponse = restaurantsRepository.getRestaurantsAroundPosition(
                    positionWithZoom.latitude.toString(),
                    positionWithZoom.longitude.toString(),
                    context.resources.getString(R.string.default_detection_radius_value)
                )

                when (restaurantResponse) {
                    is RestaurantsRepository.ResponseStatus.Success -> googleMapViewStateMutableSharedFlow.tryEmit(
                        map(positionWithZoom, restaurantResponse.data)
                    )
                    is RestaurantsRepository.ResponseStatus.NoResponse -> googleMapViewActionChannel.send(
                        GoogleMapViewAction.Toast(R.string.no_response_message)
                    )
                    is RestaurantsRepository.ResponseStatus.StatusError.HttpException -> googleMapViewActionChannel.send(
                        GoogleMapViewAction.Toast(R.string.network_error_message)
                    )
                    is RestaurantsRepository.ResponseStatus.StatusError.IOException -> googleMapViewActionChannel.send(
                        GoogleMapViewAction.Toast(R.string.no_internet_message)
                    )
                }
            }.collect()
        }
    }

    fun onMapReady() {
        onMapReadyMutableStateFlow.value = true
    }

    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    private fun map(
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurant: List<Restaurant>
    ): GoogleMapViewState {
        val restaurantViewStateMutableList = mutableListOf<GoogleMapItemViewState>()

        listRestaurant.forEach {
            restaurantViewStateMutableList.add(
                GoogleMapItemViewState(
                    it.geometry.location.lat,
                    it.geometry.location.lng,
                    it.name
                )
            )
        }

        return GoogleMapViewState(
            positionWithZoom.latitude,
            positionWithZoom.longitude,
            positionWithZoom.zoom,
            Collections.unmodifiableList(restaurantViewStateMutableList)
        )
    }

    // Data Class to emit to the UI
    data class GoogleMapViewState(
        val latitude: Double,
        val longitude: Double,
        val zoom: Float,
        val items: List<GoogleMapItemViewState>
    )

    // Data Class to emit to the UI
    data class GoogleMapItemViewState(
        val latitude: Double,
        val longitude: Double,
        val name: String
    )

    sealed class GoogleMapViewAction {
        data class Toast(@StringRes val messageResId : Int) : GoogleMapViewAction()
    }
}