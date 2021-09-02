package fr.plopez.go4lunch.view.main_activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import fr.plopez.go4lunch.data.model.MapLocation
import fr.plopez.go4lunch.utils.LocationUpdatesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

class GoogleMapsFragmentViewModel(private val client: FusedLocationProviderClient): ViewModel() {

    companion object {
        private val TAG = "Google Maps ViewModel"
    }

    private val curLocationMutableLiveData : MutableLiveData<LatLng>? = null

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    fun monitorUserLocation() {
        val locationUpdatesUseCase = LocationUpdatesUseCase(client)
        viewModelScope.launch {
            locationUpdatesUseCase
                .fetchUpdates()
                .collect {
                    curLocationMutableLiveData?.value = it
                }
        }
    }

    fun getCurrentLocationLiveData(): LiveData<LatLng>? = curLocationMutableLiveData
}