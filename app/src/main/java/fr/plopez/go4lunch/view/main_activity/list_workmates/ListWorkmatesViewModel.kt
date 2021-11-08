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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
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

    fun getWorkmatesUpdates(): LiveData<List<WorkmateViewState>> =
        liveData(coroutinesProvider.ioCoroutineDispatcher) {
            firestoreRepository.getWorkmatesUpdates().collect {
                emit(mapToViewState(it))
            }
        }

    private fun mapToViewState(workmatesList: List<Workmate>) =
        workmatesList.map {
            val name = it.name ?: Regex(EMAIL_NAME_PATTERN).find(it.email)?.groupValues?.get(1)

            WorkmateViewState(
                photoUrl = it.photoUrl,
                text = context.resources.getString(R.string.workmate_has_not_decided, name)
            )
        }
}