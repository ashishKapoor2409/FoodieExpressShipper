package com.example.android.foodieexpressshipper

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.icu.number.IntegerWidth
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.Button
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
import com.example.android.foodieexpressshipper.common.LatLngInterpolator
import com.example.android.foodieexpressshipper.common.MarkerAnimation
import io.reactivex.android.schedulers.AndroidSchedulers

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.android.foodieexpressshipper.databinding.ActivityShippingBinding
import com.example.android.foodieexpressshipper.model.ShipperOrderModel
import com.example.android.foodieexpressshipper.remote.IGoogleApi
import com.example.android.foodieexpressshipper.remote.RetroFitClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


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
    private lateinit var btn_start_trip: Button

    private var shipperMarker: Marker? = null
    private var shippingOrderModel: ShipperOrderModel? = null


    var isInit = false
    var previousLocation:Location? = null

    private var handler: Handler? = null
    private var index: Int = -1
    private var next:Int = 0
    private var startPosition:LatLng? = LatLng(0.0,0.0)
    private var endPosition:LatLng? = LatLng(0.0,0.0)
    private var v:Float = 0f
    private var lat:Double = -1.0
    private var lng:Double = -1.0
    
    private var blackPolyline:Polyline? = null
    private var greyPolyline:Polyline? = null
    private var polylineOptions:PolylineOptions? = null
    private var blackPolylineOptions:PolylineOptions? = null
    private var redPolyline:Polyline? = null


    private var polylineList:List<LatLng> = ArrayList<LatLng>()
    private var iGoogleApi:IGoogleApi? = null
    private var compositeDisposable = CompositeDisposable()

    private lateinit var places_fragment:AutocompleteSupportFragment
    private lateinit var placesClient:PlacesClient
    private val placeFields = Arrays.asList(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG
    )



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
        setContentView(R.layout.activity_shipping)


        iGoogleApi = RetroFitClient.instance!!.create(IGoogleApi::class.java)
        txt_name = findViewById(R.id.txt_name)
        txt_address = findViewById(R.id.txt_address)
        txt_order_number = findViewById(R.id.txt_order_number)
        txt_date = findViewById(R.id.txt_date)
        img_food_image = findViewById(R.id.img_food_image)
        btn_start_trip = findViewById(R.id.btn_start_trip)
//        binding = ActivityShippingBinding.inflate(layoutInflater)
//        setContentView(binding.root)

        initPlaces()
        setupPlaceAutocomplete()
        buildLocationRequest()
        buildLocationCallback()

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

        initViews()


    }

    private fun setupPlaceAutocomplete() {
        places_fragment = supportFragmentManager.findFragmentById(R.id.places_autocomplete_fragment)
        as AutocompleteSupportFragment
        places_fragment.setPlaceFields(placeFields)
        places_fragment.setOnPlaceSelectedListener(object :PlaceSelectionListener{
            override fun onError(p0: Status) {
                Toast.makeText(this@ShippingActivity,""+p0.statusMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(p0: Place) {
                Toast.makeText(this@ShippingActivity,StringBuilder(p0.name!!)
                    .append("-")
                    .append(p0.latLng).toString(), Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun initPlaces() {
        Places.initialize(this,getString(R.string.google_maps_key))
        placesClient  = Places.createClient(this)
    }

    private fun initViews() {
        btn_start_trip.setOnClickListener {
            val data = Paper.book().read<String>(Common.SHIPPING_DATA)
            Paper.book().write(Common.TRIP_START,data)
            btn_start_trip.isEnabled = false
        }
    }

    private fun setShippingOrderModel() {
        Paper.init(this)
        var data :String? = null
        if(TextUtils.isEmpty(Paper.book().read(Common.TRIP_START))) {
            data = Paper.book().read<String>(Common.SHIPPING_DATA)
            btn_start_trip.isEnabled = true
        }
        else {
            data = Paper.book().read<String>(Common.SHIPPING_DATA)
            btn_start_trip.isEnabled = true
        }
        if (!TextUtils.isEmpty(data)) {
            drawRoutes(data)
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

    private fun drawRoutes(data: String?) {
        val shippingOrderModel = Gson()
            .fromJson<ShipperOrderModel>(data,object:TypeToken<ShipperOrderModel>(){}.type)

        mMap.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
            .title(shippingOrderModel.orderModel!!.userName)
            .snippet(shippingOrderModel.orderModel!!.shippingAddress)
            .position(LatLng(shippingOrderModel.orderModel!!.lat,shippingOrderModel.orderModel!!.lng)))

        if (ContextCompat.checkSelfPermission(
                this@ShippingActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&

            ContextCompat.checkSelfPermission(
                this@ShippingActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation
                .addOnFailureListener { e -> Toast.makeText(this@ShippingActivity,""+e.message,
                    Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener { location ->
                    val to = StringBuilder().append(shippingOrderModel.orderModel!!.lat)
                        .append(",")
                        .append(shippingOrderModel.orderModel!!.lng).toString()
                    val from = StringBuilder().append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString()

                    compositeDisposable.add(iGoogleApi!!.getDirections("driving","less_driving",
                        from,to,getString(R.string.google_maps_key))!!
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ s ->
                            try {

                                val jsonObject = JSONObject(s)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                for (i in 0 until jsonArray.length()) {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyline = poly.getString("points")
                                    polylineList = Common.decodePoly(polyline)

                                }
                                polylineOptions = PolylineOptions()
                                polylineOptions!!.color(Color.RED)
                                polylineOptions!!.width(12.0f)
                                polylineOptions!!.startCap(SquareCap())
                                polylineOptions!!.endCap(SquareCap())
                                polylineOptions!!.jointType(JointType.ROUND)
                                polylineOptions!!.addAll(polylineList)
                                redPolyline = mMap.addPolyline(polylineOptions!!)


                            } catch (e: Exception) {
                                Log.d("DEBUG", e.message.toString())
                            }
                        }, { throwable ->
                            Toast.makeText(this@ShippingActivity,""+throwable.message,Toast.LENGTH_SHORT).show()
                        }
                        ))
                }
        } else {

            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
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
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18f))
                }
                if(isInit && previousLocation != null) {

                    val from = StringBuilder()
                        .append(previousLocation!!.latitude)
                        .append(",")
                        .append(previousLocation!!.longitude)

                    val to = StringBuilder()
                        .append(locationShipper!!.latitude)
                        .append(",")
                        .append(locationShipper!!.longitude)

                    moveMarkerAnimation(shipperMarker!!,from,to)

//                    val previousLocationLatLng = LatLng(previousLocation!!.latitude,previousLocation!!.longitude)
//                    MarkerAnimation.animateMarkerToGB(shipperMarker!!,locationShipper,LatLngInterpolator.Spherical())
//                    shipperMarker!!.rotation = Common.getBearing(previousLocationLatLng,locationShipper)
//                    mMap!!.animateCamera(CameraUpdateFactory.newLatLng(locationShipper))
                    previousLocation = p0.lastLocation

                }

                if (!isInit) {
                    isInit = true
                    previousLocation = p0.lastLocation
                }

            }
        }
    }

    private fun moveMarkerAnimation(marker: Marker, from: StringBuilder, to: StringBuilder) {
        compositeDisposable!!.addAll(iGoogleApi!!.getDirections("driving",
            "less_driving",
        from.toString(),
        to.toString(),
        getString(R.string.google_maps_key))!!
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ s ->
                Log.d("DEBUG", s.toString())
                try {

                    val jsonObject = JSONObject(s)
                    val jsonArray = jsonObject.getJSONArray("routes")
                    for (i in 0 until jsonArray.length()) {
                        val route = jsonArray.getJSONObject(i)
                        val poly = route.getJSONObject("overview_polyline")
                        val polyline = poly.getString("points")
                        polylineList = Common.decodePoly(polyline)

                    }
                    polylineOptions = PolylineOptions()
                    polylineOptions!!.color(Color.GRAY)
                    polylineOptions!!.width(5.0f)
                    polylineOptions!!.startCap(SquareCap())
                    polylineOptions!!.endCap(SquareCap())
                    polylineOptions!!.jointType(JointType.ROUND)
                    polylineOptions!!.addAll(polylineList)
                    greyPolyline = mMap.addPolyline(polylineOptions!!)

                    blackPolylineOptions = PolylineOptions()
                    blackPolylineOptions!!.color(Color.GRAY)
                    blackPolylineOptions!!.width(5.0f)
                    blackPolylineOptions!!.startCap(SquareCap())
                    blackPolylineOptions!!.endCap(SquareCap())
                    blackPolylineOptions!!.jointType(JointType.ROUND)
                    blackPolylineOptions!!.addAll(polylineList)
                    blackPolyline = mMap.addPolyline(blackPolylineOptions!!)

                    //Animator
                    val polyLineAnimator = ValueAnimator.ofInt(0,100)
                    polyLineAnimator.setDuration(2000)
                    polyLineAnimator.setInterpolator(LinearInterpolator())
                    polyLineAnimator.addUpdateListener { valueAnimator ->
                        val points  = greyPolyline!!.points
                        val percentValue = Integer.parseInt(valueAnimator.animatedValue.toString())
                        val size = points.size
                        val newPoints = (size *(percentValue/100.0f)).toInt()
                        val  p =points.subList(0,newPoints)
                        blackPolyline!!.points = p

                    }
                    polyLineAnimator.start()
                    index = -1
                    next = 1
                    val r =object : Runnable {
                        override fun run() {
                            if(index<polylineList.size -1) {
                                index++
                                next = index+1
                                startPosition = polylineList[index]
                                endPosition = polylineList[next]
                            }

                            val valueAnimator = ValueAnimator.ofInt(0,1)
                            valueAnimator.setDuration(1500)
                            valueAnimator.setInterpolator(LinearInterpolator())
                            valueAnimator.addUpdateListener { valueAnimator ->
                                v = valueAnimator.animatedFraction
                                lat = v * endPosition!!.latitude + (1-v)*startPosition!!.latitude
                                lng = v* endPosition!!.longitude +(1-v)*startPosition!!.longitude

                                val newPos = LatLng(lat,lng)
                                marker.position = newPos
                                marker.setAnchor(0.5f,0.5f)
                                marker.rotation = Common.getBearing(startPosition!!,newPos)

                                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                            }

                            valueAnimator.start()
                            if(index<polylineList.size -2)
                                handler!!.postDelayed(this,1500)
                        }

                    }


                    handler = Handler()
//                    index = -1
//                    next = 1
                    handler!!.postDelayed(r,1500)



                } catch (e: Exception) {
                    Log.d("DEBUG", e.message.toString())
                }
            }, { throwable ->
                Toast.makeText(this@ShippingActivity,""+throwable.message,Toast.LENGTH_SHORT).show()
            }
            ))
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
        setShippingOrderModel()
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
        compositeDisposable.clear()
        super.onDestroy()
    }
}