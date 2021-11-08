package fr.plopez.go4lunch.view.landing_page

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.view.landing_page.LandingPageViewModel.LandingPageViewAction.GoToMainActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LandingPageViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val coroutinesProvider: CoroutinesProvider
) : ViewModel() {

    private val viewActionMutableLiveData = MutableLiveData<LandingPageViewAction>()
    val viewActionLiveData : LiveData<LandingPageViewAction> = viewActionMutableLiveData


    fun onLoginSuccessful(user: FirebaseUser) {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher){
            firestoreRepository.addOrUpdateUserOnLogin(user)

            viewActionMutableLiveData.value = GoToMainActivity
        }
    }

    sealed class LandingPageViewAction {
        object GoToMainActivity : LandingPageViewAction()
    }
}