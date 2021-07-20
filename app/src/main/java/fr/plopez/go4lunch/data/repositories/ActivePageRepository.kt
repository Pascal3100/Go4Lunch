package fr.plopez.go4lunch.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ActivePageRepository {
    private val _currentActivePageMutableLiveData = MutableLiveData(0)
    val currentActivePageLiveData:LiveData<Int> = _currentActivePageMutableLiveData

    fun setActivePage(activePage:Int){
        _currentActivePageMutableLiveData.value = activePage
    }
}