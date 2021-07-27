package fr.plopez.go4lunch.view.main_activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.plopez.go4lunch.R

class ListWorkmatesFragment : Fragment() {

    //
    companion object {
        fun newInstance(): ListWorkmatesFragment {
            return ListWorkmatesFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_workmates, container, false)
    }
}