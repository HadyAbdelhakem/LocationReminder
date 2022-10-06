
package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment() {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var supportMapFragment : SupportMapFragment
    lateinit var map: GoogleMap
    private val TAG = SelectLocationFragment::class.java.simpleName
    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_select_location, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync { mMap ->
            map = mMap
            map.mapType = GoogleMap.MAP_TYPE_NORMAL

            map.clear()

            val googlePlex = CameraPosition.builder()
                .target(LatLng(37.4219999, -122.0862462))
                .zoom(10f)
                .bearing(0f)
                .tilt(45f)
                .build()

            map.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex), 1000, null)

            /*map.addMarker(
                MarkerOptions()
                    .position(LatLng(37.4629101, -122.2449094))
                    .title("Iron Man")
                    .snippet("His Talent : Plenty of money")
            )*/
            setMapLongClick(map)
            setPoiClick(map)
            setMapStyle(map)
            enableMyLocation()
        }


        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)



        return rootView
    }




    private fun onLocationSelected(lat: Double, lng: Double, locStr: String) {

//        Toast.makeText(activity , " $lat $lng", Toast.LENGTH_SHORT).show()
        _viewModel.latitude.value = lat
        _viewModel.longitude.value = lng
        _viewModel.reminderSelectedLocationStr.value = locStr

        findNavController().navigate(R.id.action_selectLocationFragment_to_saveReminderFragment)

        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "%1$.3f, %2$.3f",
                latLng.latitude,
                latLng.longitude
            )
            Log.i("LatLng_STRING" , snippet)
            onLocationSelected(latLng.latitude , latLng.longitude , snippet )
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(snippet)
            )
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            onLocationSelected(poi.latLng.latitude , poi.latLng.longitude , poi.name)
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                context?.let {
                    MapStyleOptions.loadRawResourceStyle(
                        it.applicationContext,
                        R.raw.map_style
                    )
                }
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun isPermissionGranted() : Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(
                it.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
        } === PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it.applicationContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED && context?.let {
                    ActivityCompat.checkSelfPermission(
                        it.applicationContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            map.setMyLocationEnabled(true)
        }
        else {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }
}