package fr.plopez.go4lunch.view.main_activity.list_restaurants

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.view.main_activity.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@AndroidEntryPoint
@FlowPreview
@ExperimentalCoroutinesApi
class ListViewRestaurantFragment : Fragment() {

    //
    companion object {
        fun newInstance(): ListViewRestaurantFragment {
            return ListViewRestaurantFragment()
        }
    }

    private val listRestaurantsViewModel: ListRestaurantsViewModel by viewModels()
    private lateinit var onClickRestaurantListener: OnClickRestaurantListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            onClickRestaurantListener = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement MainActivity"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_view_restaurant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ListRestaurantsAdapter(onClickRestaurantListener)
        val recyclerView = view.findViewById<RecyclerView>(R.id.list_view_restaurant_recyclerview)

        recyclerView?.adapter = adapter

        listRestaurantsViewModel.restaurantsItemsLiveData.observe(viewLifecycleOwner) {
            adapter.updateRestaurantsList(it)
        }
    }
}