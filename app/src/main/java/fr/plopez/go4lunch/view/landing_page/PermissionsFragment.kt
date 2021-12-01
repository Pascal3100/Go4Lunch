package fr.plopez.go4lunch.view.landing_page

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.databinding.FragmentPermissionsBinding
import fr.plopez.go4lunch.interfaces.OnPermissionsAccepted

@AndroidEntryPoint
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

        // Go check GPS
        checkGps()

        // Go check permissions
        checkPermissions()
    }

    private fun checkGps() {
        val activity = requireActivity()
        val manager = activity.getSystemService(LOCATION_SERVICE) as LocationManager?
        if (!manager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
    }

    // Check External Storage Permissions
    @SuppressLint("WrongConstant")
    private fun hasWriteExternalStoragePermissions()=
        checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    // Check Location Permissions
    @SuppressLint("WrongConstant")
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

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes",
                DialogInterface.OnClickListener {
                    dialog, id -> onPermissionsAccepted.onGPSActivationRequest(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                })
            .setNegativeButton("No",
                DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
                }
            )
        val alert: AlertDialog = builder.create()
        alert.show()
    }

}