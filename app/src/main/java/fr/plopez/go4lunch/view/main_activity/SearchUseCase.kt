package fr.plopez.go4lunch.view.main_activity

import fr.plopez.go4lunch.data.repositories.AutoCompleteRepository
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.view.main_activity.SearchUseCase.SearchStringStatus.EmptyString
import fr.plopez.go4lunch.view.main_activity.SearchUseCase.SearchStringStatus.SearchString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@FlowPreview
@Singleton
@ExperimentalCoroutinesApi
class SearchUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val autoCompleteRepository: AutoCompleteRepository,
) {

    private val searchTextMutableStateFlow = MutableStateFlow<SearchStringStatus>(value = EmptyString)
    private val workmatesViewDisplayStateMutableStateFlow = MutableStateFlow(false)

    fun updateSearchText(searchStringStatus: SearchStringStatus) {
        searchTextMutableStateFlow.value = searchStringStatus
    }

    fun updateWorkmatesViewDisplayState(isWorkmatesViewDisplayed: Boolean) {
        workmatesViewDisplayStateMutableStateFlow.value = isWorkmatesViewDisplayed
    }

    suspend fun getSearchResult(): Flow<SearchResultStatus> =
        workmatesViewDisplayStateMutableStateFlow.flatMapLatest { workmatesViewDisplayState ->
            locationRepository.fetchUpdates().flatMapLatest { positionWithZoom ->
                searchTextMutableStateFlow.debounce(250).transform { searchStringStatus ->
                    if (searchStringStatus is EmptyString) {
                        emit(SearchResultStatus.EmptyQuery)
                    } else if (!workmatesViewDisplayState && searchStringStatus is SearchString) {
                        val response = autoCompleteRepository.getAutocompleteResults(
                            searchQuery = searchStringStatus.data,
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
                    } else if (!workmatesViewDisplayState && searchStringStatus is SearchString) {
                        emit(SearchResultStatus.SearchResult(data = listOf(searchStringStatus.data)))
                    }
                }
            }
        }

    sealed class SearchResultStatus {
        object EmptyQuery : SearchResultStatus()
        object NoResponse : SearchResultStatus()
        data class SearchResult(val data: List<String>) : SearchResultStatus()
    }

    sealed class SearchStringStatus {
        object EmptyString : SearchStringStatus()
        data class SearchString(val data: String) : SearchStringStatus()
    }
}