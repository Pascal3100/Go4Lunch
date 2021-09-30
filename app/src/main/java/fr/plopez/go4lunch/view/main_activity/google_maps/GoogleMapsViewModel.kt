package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import kotlinx.coroutines.Dispatchers
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
        private const val TAG = "Google Maps ViewModel"
    }

    // Position with zoom and restaurants flow
    private val googleMapViewStateMutableSharedFlow =
        MutableSharedFlow<GoogleMapViewState>(replay = 1)
    val googleMapViewStateSharedFlow = googleMapViewStateMutableSharedFlow.asSharedFlow()

    // Equivalent SingleLiveEvent with Flows
    private val googleMapViewActionChannel = Channel<GoogleMapViewAction>(Channel.BUFFERED)
    val googleMapViewActionFlow = googleMapViewActionChannel.receiveAsFlow()

    // Map state Flow
    private val onMapReadyMutableStateFlow = MutableStateFlow(false)

    init {

        // Initialization of the flow at init of the viewModel
        viewModelScope.launch(Dispatchers.IO) {

            combine(onMapReadyMutableStateFlow,locationRepository.fetchUpdates()) { isMapReady, positionWithZoom ->
                // Send an empty flow is map not ready
                if (!isMapReady) return@combine null

                // TODO : detection radius can be a setting...
                // Send a retrofit request to fetch restaurants around received position
                restaurantsRepository.getRestaurantsAroundPosition(
                    positionWithZoom.latitude.toString(),
                    positionWithZoom.longitude.toString(),
                    context.resources.getString(R.string.default_detection_radius_value)
                ).collect { restaurantResponse->

                    when (restaurantResponse) {
                        is RestaurantsRepository.ResponseStatus.Success -> mapData(positionWithZoom, restaurantResponse.data)
                        is RestaurantsRepository.ResponseStatus.NoUpdate,
                        is RestaurantsRepository.ResponseStatus.NoResponse -> mapEvent(R.string.no_response_message)
                        is RestaurantsRepository.ResponseStatus.StatusError.HttpException -> mapEvent( R.string.network_error_message)
                        is RestaurantsRepository.ResponseStatus.StatusError.IOException -> mapEvent( R.string.no_internet_message)
                    }
                }


            }.collect()
        }
    }

    // Track Map status
    fun onMapReady() {
        onMapReadyMutableStateFlow.value = true
    }

    // Track zoom changes
    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    // Manage data sending in flows
    private suspend fun mapData(
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurants: List<RestaurantEntity>
    ) {

        // Send cur position and list of around restaurants
        googleMapViewStateMutableSharedFlow.tryEmit(
            map(positionWithZoom, listRestaurants)
        )
    }

    private suspend fun mapEvent(
        messageResId: Int
    ) {
        // Send message if there is one to send
        if (messageResId != 0) {
            googleMapViewActionChannel.send(
                GoogleMapViewAction.ResponseStatusMessage(R.string.no_response_message)
            )
        }
    }

    // Mapper for the ui view state
    private fun map(
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurants: List<RestaurantEntity>
    ): GoogleMapViewState {
        val restaurantViewStateMutableList = mutableListOf<RestaurantViewState>()

        listRestaurants.forEach {
            restaurantViewStateMutableList.add(
                RestaurantViewState(
                    it.latitude,
                    it.longitude,
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
        val restaurantList: List<RestaurantViewState>
    )

    // Data Class to emit to the UI
    data class RestaurantViewState(
        val latitude: Double,
        val longitude: Double,
        val name: String
    )

    sealed class GoogleMapViewAction {
        data class ResponseStatusMessage(@StringRes val messageResId: Int) : GoogleMapViewAction()
    }

}