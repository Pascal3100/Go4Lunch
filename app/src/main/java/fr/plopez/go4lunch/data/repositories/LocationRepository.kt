package fr.plopez.go4lunch.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng

class LocationRepository {

    private var curLocationMutableLiveData : MutableLiveData<LatLng> = MutableLiveData<LatLng>(LatLng(0.0,0.0))

    fun setCurrentLocation(currentPosition: LatLng) {
        curLocationMutableLiveData.value = currentPosition
    }

    fun getCurrentLocationLiveData(): LiveData<LatLng> = curLocationMutableLiveData

}