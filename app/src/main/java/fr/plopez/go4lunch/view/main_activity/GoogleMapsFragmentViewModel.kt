package fr.plopez.go4lunch.view.main_activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.Log
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.view.model.PositionViewState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class GoogleMapsFragmentViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private val TAG = "Google Maps ViewModel"
    }

    var currentLocationMutableStateFlow = MutableStateFlow(
        PositionViewState(
            LatLng(0.0, 0.0),
            context.resources.getString(R.string.default_zoom_value).toFloat()
        )
    )
    val currentLocationFlow: Flow<PositionViewState> = currentLocationMutableStateFlow

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    fun monitorUserLocation() {
        viewModelScope.launch {
            locationRepository.fetchUpdates().collect {
                //
                // TODO : Send a request to Retrofit with the new location value to update Restaurants list

                // Just update value to trigger the position on the UI
                currentLocationMutableStateFlow.value = it
            }
        }
    }

    fun setCurrentZoom(zoom: Float) {
        locationRepository.currentZoomMutableStateFlow.value = zoom
    }
}