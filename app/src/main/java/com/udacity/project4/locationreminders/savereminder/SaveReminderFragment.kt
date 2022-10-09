package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.createChannel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var reminder : ReminderDataItem

    private lateinit var geoClient: GeofencingClient

    private val geofenceList = ArrayList<Geofence>()

    private val gadgetQ = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private val geofenceIntent: PendingIntent by lazy {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 3
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 4
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 5

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        createChannel(requireActivity())

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        geoClient = activity?.let { LocationServices.getGeofencingClient(it) }!!

        val radius = 100f

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            geofenceList.add(Geofence.Builder()
                .setRequestId("entry.key")
                .setCircularRegion(latitude!!,longitude!!,radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build())




            examinePermisionAndinitiatGeofence()
//            Toast.makeText(activity , "$latitude $longitude" , Toast.LENGTH_SHORT ).show()

            if (location == null){
//                Toast.makeText(activity , "Please select location." , Toast.LENGTH_SHORT ).show()
            }else{
                if (title == null || description == null){
//                    Toast.makeText(activity , "Please set title and description." , Toast.LENGTH_SHORT ).show()
                }else {
                    reminder = ReminderDataItem(title , description , location , latitude , longitude)

//                    Toast.makeText(activity , "$title $description" , Toast.LENGTH_SHORT ).show()
                    _viewModel.validateAndSaveReminder(reminder)
                }
            }


//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
        removeGeofence()
    }

    private fun examinePermisionAndinitiatGeofence() {
        if (authorizedLocation()) {
            validateGadgetAreaInitiateGeofence()
        } else {
            askLocationPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE ||
            requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                validateGadgetAreaInitiateGeofence()
            }
        }
    }

    private fun seekGeofencing(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    @TargetApi(29)
    private fun approveForegroundAndBackgroundLocation(): Boolean {
        val foregroundLocationApproved = (
                PERMISSION_GRANTED == activity?.let {
                    ActivityCompat.checkSelfPermission(
                        it, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                })
        val backgroundPermissionApproved =
            if (gadgetQ) {
                PERMISSION_GRANTED == activity?.let {
                    ActivityCompat.checkSelfPermission(
                        it, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun authorizedLocation(): Boolean {
        val formalizeForeground = (
                PERMISSION_GRANTED == activity?.let {
                    ActivityCompat.checkSelfPermission(
                        it, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                })
        val formalizeBackground =
            if (gadgetQ) {
                PERMISSION_GRANTED == activity?.let {
                    ActivityCompat.checkSelfPermission(
                        it, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
            } else {
                true
            }
        return formalizeForeground && formalizeBackground
    }

    @TargetApi(29)
    private fun askLocationPermission() {
        if (authorizedLocation())
            return
        var grantingPermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val customResult = when {
            gadgetQ -> {
                grantingPermission += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "askLocationPermission")

        activity?.let {
            ActivityCompat.requestPermissions(
                it, grantingPermission, customResult
            )
        }
    }

    private fun validateGadgetAreaInitiateGeofence(resolve: Boolean = true) {

        // create a location request that request for the quality of service to update the location
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        // check if the client location settings are satisfied
        val client = activity?.let { LocationServices.getSettingsClient(it) }

        // create a location response that acts as a listener for the device location if enabled
        val locationResponses = client?.checkLocationSettings(builder.build())

        locationResponses?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    activity?.let {
                        exception.startResolutionForResult(
                            it, REQUEST_TURN_DEVICE_LOCATION_ON
                        )
                    }
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: ${sendEx.message}")
                }
            } else {
//                Toast.makeText(activity, "Enable your location", Toast.LENGTH_SHORT).show()
            }
        }

        locationResponses?.addOnCompleteListener { it ->
            if (it.isSuccessful) {
                addGeofence()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        validateGadgetAreaInitiateGeofence(false)
    }

    private fun addGeofence(){
        if (activity?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geoClient?.addGeofences(seekGeofencing(), geofenceIntent)?.run {
            addOnSuccessListener {
                Log.i("Adding geofence" , "Geofence added")
            }
            addOnFailureListener {
                Log.i("Adding geofence" , "Failed to add geofences")

            }
        }
    }

    private fun removeGeofence(){
        geoClient?.removeGeofences(geofenceIntent)?.run {
            addOnSuccessListener {
                Log.i("Removing geofence" , "Geofence removed")

            }
            addOnFailureListener {
                Log.i("Removing geofence" , "Failed to remove geofence")
            }
        }
    }

}
