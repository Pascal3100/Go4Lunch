package fr.plopez.go4lunch.view.landing_page

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import fr.plopez.go4lunch.databinding.FragmentPermissionsBinding
import fr.plopez.go4lunch.utils.CustomSnackBar

class PermissionsFragment : Fragment() {

    companion object{
        val REQUEST_CODE = 0

        fun newInstance(): PermissionsFragment {
            return PermissionsFragment()
        }
    }

    // View binding
    private lateinit var binding: FragmentPermissionsBinding
    private lateinit var snack: CustomSnackBar

    override fun onCreateView(
        layoutInflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPermissionsBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        snack = CustomSnackBar(requireView(), requireContext())
        checkPermissions()
    }

    // Check External Storage Permissions
    private fun hasWriteExternalStoragePermissions()=
        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    // Check Location Permissions
    private fun hasLocationForegroundPermissions()=
        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Check permissions
    private fun checkPermissions(){
        var permissionsToRequest = mutableListOf<String>()
        if (!hasWriteExternalStoragePermissions()){
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!hasLocationForegroundPermissions()){
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()){
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty()){
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    snack.showWarningSnackBar("Please accept permissions to continue")
                }
            }
        }
    }


}