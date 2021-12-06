package fr.plopez.go4lunch.view.main_activity.list_workmates

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.Workmate
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.view.main_activity.SearchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
@FlowPreview
@ExperimentalCoroutinesApi
class ListWorkmatesViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val coroutinesProvider: CoroutinesProvider,
    private val searchUseCase: SearchUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Listener to update workmates list and their choice of restaurant
    fun getWorkmatesUpdates(): LiveData<List<WorkmateViewState>> =
        liveData(coroutinesProvider.ioCoroutineDispatcher) {
            combine(
                firestoreRepository.getWorkmatesUpdates(),
                firestoreRepository.getWorkmatesWithSelectedRestaurants(),
            searchUseCase.getSearchResult()
            ){
                workmatesList, workmatesWithSelectedRestaurantList, searchResult ->
                mapToViewState(
                    workmatesList = if (searchResult is SearchUseCase.SearchResultStatus.SearchResult) {
                        workmatesList.filter { it.name.lowercase().contains(searchResult.data.first().lowercase(), ignoreCase = false)}
                    } else {
                        workmatesList
                    },
                    workmatesWithSelectedRestaurantList = workmatesWithSelectedRestaurantList
                )}.collect {
                emit(it)
            }
        }

    //
    private fun mapToViewState(
        workmatesList: List<Workmate>,
        workmatesWithSelectedRestaurantList: List<WorkmateWithSelectedRestaurant>) =
        workmatesList.map {workmate ->
            val name = workmate.name
            val restaurantIsEatingAt = workmatesWithSelectedRestaurantList.firstOrNull{ workmateWithSelectedRestaurant ->
                workmateWithSelectedRestaurant.workmateEmail == workmate.email
            }

            WorkmateViewState(
                photoUrl = workmate.photoUrl,
                text = if (restaurantIsEatingAt == null) {
                    context.resources.getString(R.string.workmate_has_not_decided, name)
                } else {
                    context.resources.getString(
                        R.string.workmate_has_decided,
                        name,
                        restaurantIsEatingAt.selectedRestaurantName)
                },
                style = if (restaurantIsEatingAt == null) {
                    R.style.workmateItemNormalGhostGreyItalicTextAppearance
                } else {
                    R.style.workmateItemNormalBlackBoldTextAppearance
                }
            )
        }
}