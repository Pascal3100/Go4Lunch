package fr.plopez.go4lunch.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class FragmentManager {
    companion object{
        fun replace(activity: AppCompatActivity, container:Int, fragment: Fragment){
            activity.supportFragmentManager
                .beginTransaction()
                .replace(container, fragment)
                .commit()
        }
    }

}