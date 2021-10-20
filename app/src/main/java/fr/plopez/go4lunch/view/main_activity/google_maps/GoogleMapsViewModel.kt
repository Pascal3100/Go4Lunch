package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
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
    coroutinesProvider: CoroutinesProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "Google Maps ViewModel"
    }

    // Position with zoom and restaurants flow
    private val googleMapViewStateMutableSharedFlow =
        MutableSharedFlow<GoogleMapViewState>(replay = 1)
    val googleMapViewStateLiveData = googleMapViewStateMutableSharedFlow.asLiveData(
        coroutinesProvider.ioCoroutineDispatcher
    )

    // Equivalent SingleLiveEvent with Flows
    // TODO @Nino to replace with Single Live Event?
    private val googleMapViewActionChannel = Channel<GoogleMapViewAction>(Channel.BUFFERED)
    val googleMapViewActionFlow = googleMapViewActionChannel.receiveAsFlow()

    // Map state Flow
    private val onMapReadyMutableStateFlow = MutableStateFlow(false)

    init {
        // Initialization of the flow at init of the viewModel
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {

            combine(
                onMapReadyMutableStateFlow, locationRepository.fetchUpdates()
            ) { isMapReady, positionWithZoom ->

                // Send an empty flow is map not ready
                if (!isMapReady) return@combine null

                // Send a retrofit request to fetch restaurants around received position
                restaurantsRepository.getRestaurantsAroundPosition(
                    positionWithZoom.latitude.toString(),
                    positionWithZoom.longitude.toString(),
                    context.resources.getString(R.string.default_detection_radius_value)
                    // the flow will be collected only if the response is different from previous ones
                ).distinctUntilChanged().collect {
                    when (it) {
                        is RestaurantsRepository.ResponseStatus.Success ->
                            mapData(positionWithZoom, it.data)
                        is RestaurantsRepository.ResponseStatus.NoUpdate -> Unit
                        is RestaurantsRepository.ResponseStatus.NoResponse ->
                            mapData(positionWithZoom, emptyList())
                        is RestaurantsRepository.ResponseStatus.StatusError.HttpException ->
                            mapEvent(R.string.network_error_message)
                        is RestaurantsRepository.ResponseStatus.StatusError.IOException ->
                            mapEvent(R.string.no_internet_message)
                    }
                }
            }.collect()
        }

        googleMapViewActionChannel.trySend(
            GoogleMapViewAction.MoveCamera(
                data = CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        googleMapViewState.latitude,
                        googleMapViewState.longitude
                    ),
                    googleMapViewState.zoom
                )
            )
        )
    }

    // Track Map status
    fun onMapReady() {
        onMapReadyMutableStateFlow.value = true
    }

    // Track zoom changes
    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    // Manage event message sending in flows
    private suspend fun mapEvent(
        messageResId: Int
    ) {
        googleMapViewActionChannel.send(
            GoogleMapViewAction.ResponseStatusMessage(messageResId)
        )
    }

    // Manage data sending in flows
    private fun mapData(
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurants: List<RestaurantEntity>,
    ) {
        // Send cur position and list of around restaurants
        googleMapViewStateMutableSharedFlow.tryEmit(
            mapToViewState(positionWithZoom, listRestaurants)
        )
    }

    // Mapper for the ui view state
    private fun mapToViewState(
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurants: List<RestaurantEntity>
    ): GoogleMapViewState {
        val restaurantViewStateList = listRestaurants.map {
            RestaurantViewState(
                it.latitude,
                it.longitude,
                it.name,
                it.restaurantId,
                it.rate
            )
        }

        return GoogleMapViewState(
            positionWithZoom.latitude,
            positionWithZoom.longitude,
            positionWithZoom.zoom,
            Collections.unmodifiableList(restaurantViewStateList)
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
        val name: String,
        val id: String,
        val rate: Float
    )

    sealed class GoogleMapViewAction {
        data class MoveCamera(val data: CameraUpdate) : GoogleMapViewAction()
        data class AnimateCamera(val data: CameraUpdate) : GoogleMapViewAction()
        data class ResponseStatusMessage(@StringRes val messageResId: Int) : GoogleMapViewAction()
    }

}