package com.example.android.foodieexpressshipper.remote

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface IGoogleApi {

    @GET("maps/api/directions/json")
    fun getDirections(
        @Query("mode") mode: String?,
        @Query("transit_routing_preferences") transit_routing:String?,
        @Query("origin") origin: String?,
        @Query("destination") destination: String?,
        @Query("key") key: String?):Observable<String?>?
}