package com.example.projectiot

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

class MqttRepository(context: Context) {

    private val mqttService = MqttService()

    // Esponi i StateFlow del servizio MQTT
    val gpsData: StateFlow<GpsData?> = mqttService.gpsData
    val doorData: StateFlow<DoorData?> = mqttService.doorData
    val presenceData: StateFlow<PresenceData?> = mqttService.presenceData
    val connectionStatus: StateFlow<Boolean> = mqttService.connectionStatus

    suspend fun connect(): Boolean {
        return mqttService.connect()
    }

    suspend fun sendDoorCommand(command: String): Boolean {
        return mqttService.sendDoorCommand(command)
    }

    fun requestDataUpdate() {
        mqttService.requestDataUpdate()
    }

    fun disconnect() {
        mqttService.disconnect()
    }

    fun isConnected(): Boolean {
        return mqttService.isConnected()
    }
}