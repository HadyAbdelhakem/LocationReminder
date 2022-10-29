package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1


class SaveReminderFragment : BaseFragment() {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var reminder: ReminderDataItem

    private lateinit var geoClient: GeofencingClient
    private val GEOFENCE_RADIUS = 100f
    private val gadgetQ = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

//    private val geofenceList = ArrayList<Geofence>()


    private val geofenceIntent: PendingIntent by lazy {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /*private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 3
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 4
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 5*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_save_reminder, container, false
            )

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections
                    .actionSaveReminderFragmentToSelectLocationFragment())
        }

        geoClient = LocationServices.getGeofencingClient(requireActivity())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminder = ReminderDataItem(title, description, location, latitude, longitude)
            val validaSave = _viewModel.validateEnteredData(reminder)

            if (validaSave){
                checkPermissionsAndStartGeofencing()
            }
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                checkDeviceLocationSettingsAndStartGeofence()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForReminder()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder() {
        if (this::reminder.isInitialized) {
            val currentGeofenceData = reminder

            val geofence = Geofence.Builder()
                .setRequestId(currentGeofenceData.id)
                .setCircularRegion(
                    currentGeofenceData.latitude!!,
                    currentGeofenceData.longitude!!,
                    GEOFENCE_RADIUS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            val intent =
                Intent(requireContext().applicationContext, GeofenceBroadcastReceiver::class.java)
                    .apply { action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT }

            val geofencePendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            geoClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.i("Geofence Message", "Add Geofence " + geofence.requestId)
                    _viewModel.saveReminder(reminder)
                }
                .addOnFailureListener {
                    if (it.message != null) {
                        Toast.makeText(
                            requireContext(),
                            "Failed, Try again",
                            Toast.LENGTH_SHORT
                        ).show()
                        checkDeviceLocationSettingsAndStartGeofence()
                    }
                }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (gadgetQ) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            gadgetQ -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d("foregroundCheck", "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("requestPermission Message", "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                addGeofenceForReminder()
            } else {
                checkDeviceLocationSettingsAndStartGeofence(false)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }

    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                Toast.makeText(activity , "Please select location" , Toast.LENGTH_SHORT ).show()
            }else{
                if (title == null || description == null){
                    Toast.makeText(activity , "Please set title and description." , Toast.LENGTH_SHORT ).show()
                }else {
                    reminder = ReminderDataItem(title , description , location , latitude , longitude)

                    Toast.makeText(activity , "$title $description" , Toast.LENGTH_SHORT ).show()
                    _viewModel.validateAndSaveReminder(reminder)
                }
            }


//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
        }
    }
*/
    /*override fun onStart() {
        super.onStart()
    }*/

    /*override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
        removeGeofence()
    }*/

    /*private fun examinePermisionAndinitiatGeofence() {
        if (authorizedLocation()) {
            validateGadgetAreaInitiateGeofence()
        } else {
            askLocationPermission()
        }
    }*/

    /*override fun onRequestPermissionsResult(
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
    }*/

    /*private fun seekGeofencing(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }*/

    /*@TargetApi(29)
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
    }*/

    /*@TargetApi(29)
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
    }*/

    /*@TargetApi(29)
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
    }*/

    /*private fun validateGadgetAreaInitiateGeofence(resolve: Boolean = true) {

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
                *//*addGeofence()*//*
            }
        }
    }*/

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        validateGadgetAreaInitiateGeofence(false)
    }*/

    /*private fun addGeofence(){
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
    }*/

    /*private fun removeGeofence(){
        geoClient?.removeGeofences(geofenceIntent)?.run {
            addOnSuccessListener {
                Log.i("Removing geofence" , "Geofence removed")

            }
            addOnFailureListener {
                Log.i("Removing geofence" , "Failed to remove geofence")
            }
        }
    }*/

}
