package fr.plopez.go4lunch.data.repositories

import fr.plopez.go4lunch.data.model.restaurant.AutoCompleteQueryResult
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.retrofit.RestaurantService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ExperimentalCoroutinesApi
class AutoCompleteRepository @Inject constructor(
    private val restaurantService: RestaurantService,
    private val nearbyConstants: NearbyConstants
) {
    // get search result from autocomplete Google web API
    suspend fun getAutocompleteResults(
        searchQuery: String,
        latitude: Double,
        longitude: Double
    ) : Response<AutoCompleteQueryResult> {
        return restaurantService.getRestaurantsFromSearch(
            key = nearbyConstants.key,
            input = searchQuery,
            location = "$latitude,$longitude"
        )
    }
}
