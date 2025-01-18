package com.mss.proyectoGPS

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

data class Route(
    val name: String,
    val coordinates: List<Coordinate>
)

interface RouteApi {
    @POST("/api/routes")
    fun saveRoute(
        @Query("name") name: String,
        @Body coordinates: List<Coordinate>
    ): Call<Route>

    @GET("/api/routes")
    fun getAllRoutes(): Call<List<Route>>
}

