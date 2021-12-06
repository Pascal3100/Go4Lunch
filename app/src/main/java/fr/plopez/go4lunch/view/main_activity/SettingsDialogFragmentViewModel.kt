package fr.plopez.go4lunch.view.main_activity

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.di.CoroutinesProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class SettingsDialogFragmentViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val coroutinesProvider: CoroutinesProvider
) : ViewModel() {

    val notificationsSettingStateLiveData = liveData(coroutinesProvider.ioCoroutineDispatcher){
        firestoreRepository.getNotificationsSettingsUpdates().collect { emit(it) }
    }

    private val settingsDialogFragmentViewActionMutableLiveData = MutableLiveData<SettingsDialogFragmentViewAction>()
    val settingsDialogFragmentViewActionLiveData = settingsDialogFragmentViewActionMutableLiveData as LiveData<SettingsDialogFragmentViewAction>

    fun onSwitchNotificationsSettings(isChecked: Boolean) {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            if (!firestoreRepository.setNotificationsSetting(isChecked)) {
                settingsDialogFragmentViewActionMutableLiveData.value = SettingsDialogFragmentViewAction.Failed(!isChecked)
            }
        }
    }

    sealed class SettingsDialogFragmentViewAction {
        data class Failed(val previousState:Boolean): SettingsDialogFragmentViewAction()
    }
}