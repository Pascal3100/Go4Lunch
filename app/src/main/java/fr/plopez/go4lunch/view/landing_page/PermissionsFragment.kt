package fr.plopez.go4lunch.view.landing_page

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.PermissionChecker.checkSelfPermission
import fr.plopez.go4lunch.databinding.FragmentPermissionsBinding
import fr.plopez.go4lunch.interfaces.OnPermissionsAccepted
import fr.plopez.go4lunch.utils.CustomSnackBar
import java.lang.ClassCastException

class PermissionsFragment : Fragment() {

    companion object{
        val REQUEST_CODE = 0

        fun newInstance(): PermissionsFragment {
            return PermissionsFragment()
        }
    }

    // View binding
    private lateinit var binding: FragmentPermissionsBinding

    private lateinit var onPermissionsAccepted: OnPermissionsAccepted


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPermissionsAccepted) {
            onPermissionsAccepted = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement OnPermissionsAccepted"
            )
        }
    }

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

        // Go check permissions
        checkPermissions()
    }

    // Check External Storage Permissions
    private fun hasWriteExternalStoragePermissions()=
        checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    // Check Location Permissions
    private fun hasLocationForegroundPermissions()=
        checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Check permissions
    private fun checkPermissions(){
        val permissionsToRequest = mutableListOf<String>()
        if (!hasWriteExternalStoragePermissions()){
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!hasLocationForegroundPermissions()){
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()){
            requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_CODE)
        } else {
            onPermissionsAccepted.onPermissionsAccepted(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty()){
            var acceptedStatus = true
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    acceptedStatus = false
                    break
                }
            }
            onPermissionsAccepted.onPermissionsAccepted(acceptedStatus)
        }
    }
}