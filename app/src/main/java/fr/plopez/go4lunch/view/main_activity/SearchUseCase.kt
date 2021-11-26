package fr.plopez.go4lunch.view.main_activity

import android.util.Log
import fr.plopez.go4lunch.data.repositories.AutoCompleteRepository
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
class SearchUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val autoCompleteRepository: AutoCompleteRepository,
    private val coroutinesProvider: CoroutinesProvider
) {

    private val searchTextMutableStateFlow = MutableStateFlow("")
    private val workmatesViewDisplayStateMutableStateFlow = MutableStateFlow(false)

    fun updateSearchText(newSearchText: String) {
        searchTextMutableStateFlow.value = newSearchText
        Log.d("TAG", "#### searchTextMutableStateFlow = ${searchTextMutableStateFlow.value}")
    }

    fun updateWorkmatesViewDisplayState(isWorkmatesViewDisplayed: Boolean) {
        workmatesViewDisplayStateMutableStateFlow.value = isWorkmatesViewDisplayed
        Log.d("TAG", "#### workmatesViewDisplayState = ${workmatesViewDisplayStateMutableStateFlow.value}")
    }

    suspend fun getSearchResult():Flow<SearchResultStatus> =
            workmatesViewDisplayStateMutableStateFlow.flatMapLatest { workmatesViewDisplayState ->
                Log.d("TAG", "#### workmatesViewDisplayState: $workmatesViewDisplayState")
                locationRepository.fetchUpdates().flatMapLatest { positionWithZoom ->
                    Log.d("TAG", "#### fetchUpdates: $positionWithZoom")
                    searchTextMutableStateFlow.debounce(250).transform { searchQuery ->
                            Log.d("TAG", "#### searchQuery = #$searchQuery#")
                            if (searchQuery == "") {
                                emit(SearchResultStatus.EmptyQuery)
                            } else if (!workmatesViewDisplayState) {
                                val response = autoCompleteRepository.getAutocompleteResults(
                                    searchQuery = searchQuery,
                                    latitude = positionWithZoom.latitude,
                                    longitude = positionWithZoom.longitude
                                )
                                val responseBody = response.body()
                                Log.d("TAG", "#### responseBody: $responseBody")

                                if (response.isSuccessful && responseBody != null && responseBody.predictions != null) {
                                    Log.d("TAG", "#### getSearchResult: pass 1")
                                    emit(SearchResultStatus.SearchResult(
                                        data = responseBody.predictions.mapNotNull { it.place_id }
                                    ))
                                } else {
                                    Log.d("TAG", "#### getSearchResult: pass 2")
                                    emit(SearchResultStatus.NoResponse)
                                }
                            } else {
                                Log.d("TAG", "#### getSearchResult: pass 3")
                                emit(SearchResultStatus.SearchResult(data = listOf(searchQuery)))
                            }
                        }
                }
            }

    sealed class SearchResultStatus {
        object EmptyQuery : SearchResultStatus()
        object NoResponse : SearchResultStatus()
        data class SearchResult(val data: List<String>) : SearchResultStatus()
    }

}