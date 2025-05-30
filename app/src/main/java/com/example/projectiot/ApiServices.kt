package com.example.projectiot

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("/api/gps")
    suspend fun getGps(): GpsData

    @GET("/api/doors")
    suspend fun getDoors(): DoorData

    @GET("/api/presence")
    suspend fun getPresence(): PresenceData

    @POST("/api/doors")
    suspend fun controlDoors(@Body command: DoorCommand): Response<Unit>
}

data class DoorCommand(val command: String)