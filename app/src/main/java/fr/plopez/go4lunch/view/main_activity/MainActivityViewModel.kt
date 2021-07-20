package fr.plopez.go4lunch.view.main_activity

import androidx.lifecycle.ViewModel
import fr.plopez.go4lunch.data.repositories.ActivePageRepository

class MainActivityViewModel constructor(
    private val activePageRepository: ActivePageRepository
): ViewModel() {

    fun getActivePageLiveData() = activePageRepository.currentActivePageLiveData

    fun setActivePage(activePage : Int) = activePageRepository.setActivePage(activePage)

}