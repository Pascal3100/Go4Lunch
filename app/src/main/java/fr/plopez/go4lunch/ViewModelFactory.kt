package fr.plopez.go4lunch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.utils.Go4LunchApp
import fr.plopez.go4lunch.view.main_activity.GoogleMapsFragmentViewModel
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel
import javax.inject.Inject

@Suppress("UNCHECKED_CAST")
class ViewModelFactory:ViewModelProvider.Factory {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(Go4LunchApp.application!!)
    @Inject val locationRepository: LocationRepository = LocationRepository()


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)){
            return MainActivityViewModel() as T
        }
        if (modelClass.isAssignableFrom(GoogleMapsFragmentViewModel::class.java)){
            return GoogleMapsFragmentViewModel(fusedLocationClient, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class : $modelClass")
    }

    companion object {
        // Dependency injection
        val INSTANCE = ViewModelFactory()
    }
}