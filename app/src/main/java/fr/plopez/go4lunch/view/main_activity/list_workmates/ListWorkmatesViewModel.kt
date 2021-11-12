package fr.plopez.go4lunch.view.main_activity.list_workmates

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.Workmate
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.view.model.WorkmateViewState
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class ListWorkmatesViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val coroutinesProvider: CoroutinesProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val EMAIL_NAME_PATTERN = "(.*)@.*"
    }

    // Listener to update workmates list and their choice of restaurant
    fun getWorkmatesUpdates(): LiveData<List<WorkmateViewState>> =
        liveData(coroutinesProvider.ioCoroutineDispatcher) {
            combine(
                firestoreRepository.getWorkmatesUpdates(),
                firestoreRepository.getWorkmatesWithSelectedRestaurants()){
                workmatesList, workmatesWithSelectedRestaurantList ->
                mapToViewState(workmatesList, workmatesWithSelectedRestaurantList)
            }.collect {
                emit(it)
            }
        }

    //
    private fun mapToViewState(
        workmatesList: List<Workmate>,
        workmatesWithSelectedRestaurantList: List<WorkmateWithSelectedRestaurant>) =
        workmatesList.map {workmate ->
            val name = workmate.name ?: Regex(EMAIL_NAME_PATTERN).find(workmate.email)?.groupValues?.get(1)

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
                    R.style.workmateNotDecidedItemGhostGreyItalicNormalTextAppearance
                } else {
                    R.style.workmateDecidedItemGhostGreyItalicNormalTextAppearance
                }
            )
        }
}