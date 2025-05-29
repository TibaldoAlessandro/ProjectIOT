package com.example.projectiot

import retrofit2.http.GET

interface ApiService {
    @GET("/api/gps")
    suspend fun getGps(): GpsData

    @GET("/api/doors")
    suspend fun getDoors(): DoorData

    @GET("/api/presence")
    suspend fun getPresence(): PresenceData
}
