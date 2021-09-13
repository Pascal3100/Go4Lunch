package fr.plopez.go4lunch.view.restaurant_details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R

@AndroidEntryPoint
class RestaurantDetailsFragment : Fragment() {

    // Argument passed to the fragment is the Restaurant id to retrieve it from ViewModel
    companion object {
        private const val ARGS_PLACE_ID = "ARGS_PLACE_ID"

        fun newInstance(placeId: String): RestaurantDetailsFragment {
            val args = Bundle()
            args.putString(ARGS_PLACE_ID, placeId)
            val fragment = RestaurantDetailsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val placeId = requireArguments().getString(ARGS_PLACE_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_restaurant_details, container, false)
    }
}