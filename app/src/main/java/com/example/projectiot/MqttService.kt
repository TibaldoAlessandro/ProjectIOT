package com.example.projectiot

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MqttService() {

    private var mqttClient: MqttClient? = null
    private val gson = Gson()

    // StateFlows per i dati
    private val _gpsData = MutableStateFlow<GpsData?>(null)
    val gpsData: StateFlow<GpsData?> = _gpsData

    private val _doorData = MutableStateFlow<DoorData?>(null)
    val doorData: StateFlow<DoorData?> = _doorData

    private val _presenceData = MutableStateFlow<PresenceData?>(null)
    val presenceData: StateFlow<PresenceData?> = _presenceData

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus

    // Configurazione MQTT
    companion object {
        private const val TAG = "MqttService"
        private const val BROKER_URL = "tcp://192.168.178.198:1883" // Indirizzo del broker Mosquitto
        private const val CLIENT_ID = "AndroidCarApp"

        // Topics MQTT
        private const val TOPIC_GPS = "car/gps"
        private const val TOPIC_DOORS = "car/doors"
        private const val TOPIC_PRESENCE = "car/presence"
        private const val TOPIC_DOOR_COMMAND = "car/doors/command"
        private const val TOPIC_GPS_RESPONSE = "car/gps/response"
        private const val TOPIC_DOORS_RESPONSE = "car/doors/response"
        private const val TOPIC_PRESENCE_RESPONSE = "car/presence/response"
    }

    suspend fun connect(): Boolean = suspendCoroutine { continuation ->
        try {
            val persistence = MemoryPersistence()
            mqttClient = MqttClient(BROKER_URL, CLIENT_ID, persistence)

            val connOpts = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                isAutomaticReconnect = true
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "Connessione MQTT persa", cause)
                    _connectionStatus.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    handleIncomingMessage(topic, message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Messaggio consegnato")
                }
            })

            try {
                mqttClient?.connect(connOpts)
                Log.d(TAG, "Connessione MQTT riuscita")
                _connectionStatus.value = true
                subscribeToTopics()
                continuation.resume(true)
            } catch (e: MqttException) {
                Log.e(TAG, "Connessione MQTT fallita", e)
                _connectionStatus.value = false
                continuation.resume(false)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore durante la connessione MQTT", e)
            _connectionStatus.value = false
            continuation.resume(false)
        }
    }

    private fun subscribeToTopics() {
        try {
            // Sottoscrizione ai topic per ricevere i dati (richieste dirette)
            mqttClient?.subscribe(TOPIC_GPS, 1)
            mqttClient?.subscribe(TOPIC_DOORS, 1)
            mqttClient?.subscribe(TOPIC_PRESENCE, 1)

            // Sottoscrizione ai topic di risposta (da Node-RED)
            mqttClient?.subscribe(TOPIC_GPS_RESPONSE, 1)
            mqttClient?.subscribe(TOPIC_DOORS_RESPONSE, 1)
            mqttClient?.subscribe(TOPIC_PRESENCE_RESPONSE, 1)

            Log.d(TAG, "Sottoscritto ai topic MQTT")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella sottoscrizione ai topic", e)
        }
    }

    private fun handleIncomingMessage(topic: String?, message: MqttMessage?) {
        try {
            val payload = message?.toString() ?: return
            Log.d(TAG, "Messaggio ricevuto - Topic: $topic, Payload: $payload")

            when (topic) {
                TOPIC_GPS_RESPONSE -> {
                    val gpsData = gson.fromJson(payload, GpsData::class.java)
                    _gpsData.value = gpsData
                }
                TOPIC_DOORS_RESPONSE -> {
                    val doorData = gson.fromJson(payload, DoorData::class.java)
                    _doorData.value = doorData
                }
                TOPIC_PRESENCE_RESPONSE -> {
                    val presenceData = gson.fromJson(payload, PresenceData::class.java)
                    _presenceData.value = presenceData
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel parsing del messaggio MQTT", e)
        }
    }

    suspend fun sendDoorCommand(command: String): Boolean = suspendCoroutine { continuation ->
        try {
            val doorCommand = DoorCommand(command)
            val payload = gson.toJson(doorCommand)
            val message = MqttMessage(payload.toByteArray())
            message.qos = 1

            try {
                mqttClient?.publish(TOPIC_DOOR_COMMAND, message)
                Log.d(TAG, "Comando porta inviato: $command")
                continuation.resume(true)
                requestDataUpdate()
            } catch (e: MqttException) {
                Log.e(TAG, "Errore nell'invio del comando porta", e)
                continuation.resume(false)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'invio del comando MQTT", e)
            continuation.resume(false)
        }
    }

    fun requestDataUpdate() {
        try {
            val message = MqttMessage("update".toByteArray())
            message.qos = 1

            mqttClient?.publish("car/request/update", message)
            Log.d(TAG, "Richiesta aggiornamento dati inviata")

        } catch (e: Exception) {
            Log.e(TAG, "Errore nella richiesta di aggiornamento", e)
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            _connectionStatus.value = false
            Log.d(TAG, "Disconnessione MQTT completata")
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante la disconnessione MQTT", e)
        }
    }

    fun isConnected(): Boolean {
        return mqttClient?.isConnected == true
    }
}

data class DoorCommand(val command: String)