package com.example.projectiot

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Repository {
    private val api = Retrofit.Builder()
        .baseUrl("http://192.168.192.48:1880") //IP nodered
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    suspend fun getGps() = api.getGps()
    suspend fun getDoors() = api.getDoors()
    suspend fun getPresence() = api.getPresence()
    suspend fun controlDoors(command: String): Response<Unit> = api.controlDoors(DoorCommand(command))
}