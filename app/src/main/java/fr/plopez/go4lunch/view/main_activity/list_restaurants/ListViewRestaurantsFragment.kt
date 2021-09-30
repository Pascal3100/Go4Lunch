package fr.plopez.go4lunch.view.main_activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R

@AndroidEntryPoint
class ListViewRestaurantFragment : Fragment() {

    //
    companion object {
        fun newInstance(): ListViewRestaurantFragment {
            return ListViewRestaurantFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_view_restaurant, container, false)
    }
}