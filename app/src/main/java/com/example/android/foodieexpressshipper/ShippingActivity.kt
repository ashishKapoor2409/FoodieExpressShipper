package com.example.android.foodieexpressshipper

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.example.android.foodieexpressshipper.common.Common
import com.example.android.foodieexpressshipper.common.Common.SHIPPING_DATA

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.android.foodieexpressshipper.databinding.ActivityShippingBinding
import com.example.android.foodieexpressshipper.model.ShipperOrderModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import com.google.gson.reflect.TypeToken


class ShippingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityShippingBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var txt_name: TextView
    private lateinit var txt_address: TextView
    private lateinit var txt_order_number: TextView
    private lateinit var txt_date: TextView
    private lateinit var img_food_image: ImageView

    private var shipperMarker: Marker? = null
    private var shippingOrderModel: ShipperOrderModel? = null

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_shipping)
        val itemView = LayoutInflater.from(this).inflate(R.layout.activity_shipping, null)
        setContentView(itemView)
        txt_name = itemView.findViewById(R.id.txt_name)
        txt_address = itemView.findViewById(R.id.txt_address)
        txt_order_number = itemView.findViewById(R.id.txt_order_number)
        txt_date = itemView.findViewById(R.id.txt_date)
        img_food_image = itemView.findViewById(R.id.img_food_image)
//        binding = ActivityShippingBinding.inflate(layoutInflater)
//        setContentView(binding.root)

        buildLocationRequest()
        buildLocationCallback()
        setShippingOrderModel()

        Dexter.withActivity(this)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    val mapFragment = supportFragmentManager
                        .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this@ShippingActivity)

                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(this@ShippingActivity)
                    if (ContextCompat.checkSelfPermission(
                            this@ShippingActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED &&

                        ContextCompat.checkSelfPermission(
                            this@ShippingActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationProviderClient.requestLocationUpdates(
                            locationRequest, locationCallback,
                            Looper.myLooper()!!
                        )
                    } else {

                        // You can directly ask for the permission.
                        // The registered ActivityResultCallback gets the result of this request.
                        requestPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        this@ShippingActivity,
                        "You must enable this permission",
                        Toast.LENGTH_SHORT
                    ).show()

                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                }

            }).check()


    }

    private fun setShippingOrderModel() {
        Paper.init(this)
        val data = Paper.book().read<String>(Common.SHIPPING_DATA)
        if (!TextUtils.isEmpty(data)) {

            shippingOrderModel = Gson()
                .fromJson<ShipperOrderModel>(data, object : TypeToken<ShipperOrderModel>() {}.type)

            if (shippingOrderModel != null) {
                Common.setSpanStringColor(
                    "Name:",
                    shippingOrderModel!!.orderModel!!.userName,
                    txt_name,
                    Color.parseColor("#333639")
                )

                Common.setSpanStringColor(
                    "Address:",
                    shippingOrderModel!!.orderModel!!.shippingAddress,
                    txt_address,
                    Color.parseColor("#673ab7")
                )

                Common.setSpanStringColor(
                    "No.",
                    shippingOrderModel!!.orderModel!!.key,
                    txt_address,
                    Color.parseColor("#795548")
                )

                txt_date!!.text = StringBuilder().append(
                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                        .format(shippingOrderModel!!.orderModel!!.createDate)
                )
                Glide.with(this)
                    .load(shippingOrderModel!!.orderModel!!.cartItemList?.get(0)!!.foodImage)
                    .into(img_food_image)

            }

        } else {
            Toast.makeText(this, "Shipping Order Model is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                val locationShipper = LatLng(
                    p0!!.lastLocation.latitude,
                    p0!!.lastLocation.longitude
                )
                if (shipperMarker == null) {
                    val height = 80
                    val width = 80
                    val bitmapDrawable = ContextCompat.getDrawable(
                        this@ShippingActivity,
                        R.drawable.shipper
                    )
                    val b = bitmapDrawable!!.toBitmap()
                    val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
                    shipperMarker = mMap!!.addMarker(
                        MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                            .position(locationShipper)
                            .title("You")
                    )!!
                } else {
                    shipperMarker!!.position = locationShipper

                }
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 15f))
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.setInterval(15000)
        locationRequest.setFastestInterval(10000)
        locationRequest.setSmallestDisplacement(20f)

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap!!.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.uber_light_with_label
                )
            )
            if (!success)
                Log.d("Foodie Express", "Failed to load map style")
        } catch (ex: Resources.NotFoundException) {
            Log.d("Foodie Express", "Not found json string for map style")
        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}