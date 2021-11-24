package fr.plopez.go4lunch.view.main_activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.FragmentDialogSettingsBinding
import fr.plopez.go4lunch.utils.CustomSnackBar
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SettingsDialogFragment : DialogFragment() {

    private val settingsDialogFragmentViewModel: SettingsDialogFragmentViewModel by viewModels()

    private lateinit var binding: FragmentDialogSettingsBinding

    companion object {
        fun newInstance() = SettingsDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDialogSettingsBinding.inflate(layoutInflater)

        settingsDialogFragmentViewModel.notificationsSettingStateLiveData.observe(this) {
            binding.notificationsSettingSwitch.isChecked = it
        }

        binding.notificationsSettingSwitch.setOnClickListener {
            settingsDialogFragmentViewModel.onSwitchNotificationsSettings(binding.notificationsSettingSwitch.isChecked)
        }

        settingsDialogFragmentViewModel.settingsDialogFragmentViewActionLiveData.observe(this){ settingsDialogFragmentViewAction ->
            if (settingsDialogFragmentViewAction is SettingsDialogFragmentViewModel.SettingsDialogFragmentViewAction.Failed) {
                binding.notificationsSettingSwitch.isChecked = settingsDialogFragmentViewAction.previousState
            }
            CustomSnackBar.with(binding.root)
                .setMessage(getString(R.string.firestore_fails_message))
                .setType(CustomSnackBar.Type.ERROR)
                .build()
                .show()
        }

        return binding.root
    }

}