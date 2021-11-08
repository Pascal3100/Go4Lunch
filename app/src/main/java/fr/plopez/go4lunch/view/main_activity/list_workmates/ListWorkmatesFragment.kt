package fr.plopez.go4lunch.view.main_activity.list_workmates

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class ListWorkmatesFragment : Fragment() {

    companion object {
        fun newInstance(): ListWorkmatesFragment {
            return ListWorkmatesFragment()
        }
    }

    private val listWorkmatesViewModel: ListWorkmatesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_workmates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ListWorkmatesAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.list_workmates_recyclerview)

        recyclerView.adapter = adapter

        // Listener for workmates list
        listWorkmatesViewModel.getWorkmatesUpdates().observe(viewLifecycleOwner){
            adapter.submitList(it)
        }
    }
}