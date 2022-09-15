package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.data.model.Resource
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.lang.Exception
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var googleMap: GoogleMap
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentPOI: PointOfInterest? = null
    private var currentPOIMarker: Marker? = null
    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.let {
            mapFragment.getMapAsync(this)
        }



        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        binding.saveLocation.setOnClickListener {
            locationSelected()
        }


        return binding.root
    }

    private fun locationSelected() {
        if(currentPOI==null)
            Toast.makeText(context, getString(R.string.selectpoi), Toast.LENGTH_SHORT).show()
else {
            _viewModel.savePOILocation(currentPOI)
            findNavController().navigate(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())

        }
    }

    override fun onMapReady(gmap: GoogleMap) {
        googleMap=gmap

        setPoiClick(googleMap)
        setLocationClick(googleMap)
        setMapStyle(googleMap)
        enableMyLocation()

    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            googleMap.isMyLocationEnabled = true
            zoomToCurrentLocation(true)
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                R.string.location_required_error,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(android.R.string.ok) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }.show()
        } else {
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun zoomToCurrentLocation(b: Boolean) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && b) {

                startIntentSenderForResult(
                    exception.resolution.intentSender,
                    REQUEST_TURN_DEVICE_LOCATION_ON,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    zoomToCurrentLocation(true)
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            val zoomLevel = 15f
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    currentLatLng,
                                    zoomLevel
                                )
                            )
                        }
                    }
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED


    }

    private fun setMapStyle(gmap: GoogleMap) {
try {
    val success = gmap.setMapStyle(
        MapStyleOptions.loadRawResourceStyle(
            context,
            R.raw.map_style
        )
    )
    if (!success) {
        Log.e("MapStyle","Style parsing failed.")
    }
}catch (e: Resources.NotFoundException){
    Log.e("mapStyleNotFound","${e.message}")
}
    }

    private fun setLocationClick(gmap: GoogleMap) {
gmap.setOnMapClickListener {
    val theAddress=Geocoder(context, Locale.getDefault()).getFromLocation(it.latitude, it.longitude, 1)
if (theAddress.isNotEmpty()){
    val theAddress: String = theAddress[0].getAddressLine(0)
    val theAddressPoi = PointOfInterest(it, null, theAddress)
    currentPOIMarker?.remove()
    val poiMarker = gmap.addMarker(
        MarkerOptions()
            .position(theAddressPoi.latLng)
            .title(theAddressPoi.name)
    )
    poiMarker?.showInfoWindow()
    currentPOIMarker = poiMarker
    currentPOI = theAddressPoi
}
}
    }

    private fun setPoiClick(gmap: GoogleMap) {
gmap.setOnPoiClickListener { pointOfInterest ->
    currentPOIMarker?.remove()
    val pointOfInterestMarker=gmap.addMarker(
        MarkerOptions()
            .position(pointOfInterest.latLng)
            .title(pointOfInterest.name)
    )
    pointOfInterestMarker.showInfoWindow()
    currentPOIMarker=pointOfInterestMarker
    currentPOI=pointOfInterest

}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_TURN_DEVICE_LOCATION_ON -> {
                zoomToCurrentLocation(false)
            }
        }
    }
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                googleMap.isMyLocationEnabled = true
                zoomToCurrentLocation(false)
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem)= when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)    }

    companion object {
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 1002
    }
}























//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.map_options, menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
//        R.id.normal_map -> {
//            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
//            true
//        }
//        R.id.hybrid_map -> {
//            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
//            true
//        }
//        R.id.satellite_map -> {
//            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
//            true
//        }
//        R.id.terrain_map -> {
//            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
//            true
//        }
//        else -> super.onOptionsItemSelected(item)
//    }
//
//    override fun onRequestPermissionsResult(
//            requestCode: Int,
//            permissions: Array<out String>,
//            grantResults: IntArray
//    ) {
//        if (grantResults.isEmpty() || grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED
//                || (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
//                        grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
//                        PackageManager.PERMISSION_DENIED)
//        ) {
//            Snackbar.make(
//                    binding.constraintLayout,
//                    R.string.permission_denied_explanation,
//                    Snackbar.LENGTH_INDEFINITE
//            ).setAction(R.string.settings) {
//                startActivity(Intent().apply {
//                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                })
//            }.show()
//        } else {
//            Log.e("onRequestPermissionsRes","onRequestPermissionsResult else called")
//            checkDeviceLocationSettingsAndStartGeofence()
//        }
//    }
//
//
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.e("onActivityResult","ResultCode is $resultCode and requestCode is $requestCode")
//        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
//            Log.e("onActivityResult","onActivityResult REQUEST_TURN_DEVICE_LOCATION_ON ")
//            checkDeviceLocationSettingsAndStartGeofence(false)
//        }
//    }
//
//    @TargetApi(29)
//    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
//        val foregroundLocationApproved =
//                (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
//                        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
//                ))
//        val backgroundLocationApproved =
//                if (runningQOrLater) {
//
//                    PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
//                            requireContext(),
//                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
//                    )
//                } else {
//                    true
//                }
//        return foregroundLocationApproved && backgroundLocationApproved
//    }
//
//    @SuppressLint("MissingPermission")
//    fun zoomToDeviceLocation() {
//        fusedLocationProviderClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
//            if (location != null) {
//                val userLatLng = LatLng(location.latitude, location.longitude)
//                val zoomLevel = 15f
//                googleMap.moveCamera(
//                        CameraUpdateFactory.newLatLngZoom(
//                                userLatLng,
//                                zoomLevel
//                        )
//                )
//            }
//        }
//    }
//
//    @TargetApi(29)
//    private fun foregroundAndBackgroundLocationPermission() {
//        Log.e("foregroundBackground","foregroundAndBackgroundLocationPermissionApproved is ${foregroundAndBackgroundLocationPermissionApproved()}")
//        if (foregroundAndBackgroundLocationPermissionApproved()) {
//            checkDeviceLocationSettingsAndStartGeofence()
//        }
//        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
//        Log.e("runningQOrLater","runningQOrLater is $runningQOrLater")
//        val resultCode = when {
//            runningQOrLater -> {
//                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
//                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
//            }
//            else ->
//                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
//        }
//        Log.e("resultCode","resultCode is $resultCode")
//        ActivityCompat.requestPermissions(
//                requireActivity(),
//                permissionsArray,
//                resultCode
//        )
//    }
//
//    private fun onLocationSelected(poi: PointOfInterest) {
//        val latLng = poi.latLng
//        _viewModel.reminderSelectedLocationStr.value = poi.name
//        _viewModel.latitude.value = latLng.latitude
//        _viewModel.longitude.value = latLng.longitude
//        findNavController().popBackStack()
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
//        val locationRequest = LocationRequest.create().apply {
//            priority = LocationRequest.PRIORITY_LOW_POWER
//        }
//        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
//        val settingsClient = LocationServices.getSettingsClient(requireActivity())
//        val locationSettingsResponseTask =
//                settingsClient.checkLocationSettings(builder.build())
//        locationSettingsResponseTask.addOnFailureListener { exception ->
//            if (exception is ResolvableApiException && resolve) {
//                try {
//                    Log.e("StartGeofence"," exception.startResolutionForResult")
//                    startIntentSenderForResult(
//                            exception.resolution.intentSender,
//                            REQUEST_TURN_DEVICE_LOCATION_ON,
//                            null,
//                            0,
//                            0,
//                            0,
//                            null
//                    )
//                } catch (sendEx: IntentSender.SendIntentException) {
//                    Log.e("StartGeofence","Error getting location settings resolution: ${sendEx.message}")
//                }
//            } else {
//                Log.e("StartGeofence","Error getting location settings resolution: showing snackbar")
//                Snackbar.make(
//                        binding.constraintLayout,
//                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
//                ).setAction(android.R.string.ok) {
//                    Log.e("StartGeofence","setAction oK")
//                    checkDeviceLocationSettingsAndStartGeofence()
//                }.show()
//            }
//        }
//        locationSettingsResponseTask.addOnCompleteListener {
//            if (it.isSuccessful) {
//                googleMap.isMyLocationEnabled = true
//            }
//        }
//    }
//
//    private fun addPOI(map: GoogleMap) {
//        map.setOnPoiClickListener { poi ->
//            binding.saveLocation.visibility = View.VISIBLE
//            binding.saveLocation.setOnClickListener {
//                onLocationSelected(poi)
//            }
//            val poiMarker = map.addMarker(
//                    MarkerOptions().position(poi.latLng).title(poi.name)
//            )
//            poiMarker.showInfoWindow()
//        }
//
//    }
//
//    private fun addMapClik(map: GoogleMap) {
//        map.setOnMapClickListener {
//            binding.saveLocation.visibility = View.VISIBLE
//            binding.saveLocation.setOnClickListener { view ->
//                _viewModel.latitude.value = it.latitude
//                _viewModel.longitude.value = it.longitude
//                _viewModel.reminderSelectedLocationStr.value = "Custom location used"
//                findNavController().popBackStack()
//            }
//
//            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(it, 15f)
//            map.moveCamera(cameraUpdate)
//            val poiMarker = map.addMarker(MarkerOptions().position(it))
//            poiMarker.showInfoWindow()
//        }
//
//    }
//
//    private fun setMapStyle(map: GoogleMap) {
//        try {
//            val success = map.setMapStyle(
//                    MapStyleOptions.loadRawResourceStyle(
//                            context,
//                            R.raw.map_style
//                    )
//            )
//        } catch (exc: Exception) {
//            Log.e("setMapStyle","Exception getting the file")
//        }
//    }
