package fr.plopez.go4lunch.view.main_activity

import fr.plopez.go4lunch.data.repositories.AutoCompleteRepository
import fr.plopez.go4lunch.data.repositories.LocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ExperimentalCoroutinesApi
class SearchUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val autoCompleteRepository: AutoCompleteRepository,
) {

    private val searchTextMutableStateFlow = MutableStateFlow("")
    private val workmatesViewDisplayStateMutableStateFlow = MutableStateFlow(false)

    fun updateSearchText(newSearchText: String) {
        searchTextMutableStateFlow.value = newSearchText
    }

    fun updateWorkmatesViewDisplayState(isWorkmatesViewDisplayed: Boolean) {
        workmatesViewDisplayStateMutableStateFlow.value = isWorkmatesViewDisplayed
    }

    suspend fun getSearchResult(): Flow<SearchResultStatus> =
        workmatesViewDisplayStateMutableStateFlow.flatMapLatest { workmatesViewDisplayState ->
            locationRepository.fetchUpdates().flatMapLatest { positionWithZoom ->
                searchTextMutableStateFlow.debounce(250).transform { searchQuery ->
                    if (searchQuery == "") {
                        emit(SearchResultStatus.EmptyQuery)
                    } else if (!workmatesViewDisplayState) {
                        val response = autoCompleteRepository.getAutocompleteResults(
                            searchQuery = searchQuery,
                            latitude = positionWithZoom.latitude,
                            longitude = positionWithZoom.longitude
                        )
                        val responseBody = response.body()

                        if (response.isSuccessful && responseBody != null && responseBody.predictions != null) {
                            emit(SearchResultStatus.SearchResult(
                                data = responseBody.predictions.mapNotNull { it.place_id }
                            ))
                        } else {
                            emit(SearchResultStatus.NoResponse)
                        }
                    } else {
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