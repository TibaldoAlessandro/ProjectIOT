package com.example.projectiot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MqttRepository(application.applicationContext)

    // StateFlow per i dati (provengono dal repository/servizio MQTT)
    val gps: StateFlow<GpsData?> = repo.gpsData
    val doors: StateFlow<DoorData?> = repo.doorData
    val presence: StateFlow<PresenceData?> = repo.presenceData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _doorCommandStatus = MutableStateFlow<String?>(null)

    private val _isInitialized = MutableStateFlow(false)

    init {
        // Connessione automatica all'avvio
        connectToMqtt()
    }

    private fun connectToMqtt() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val connected = repo.connect()
                if (connected) {
                    _doorCommandStatus.value = "Connesso al broker MQTT"
                    _isInitialized.value = true
                    // Richiedi i dati iniziali
                    repo.requestDataUpdate()
                } else {
                    _doorCommandStatus.value = "Errore di connessione al broker MQTT"
                }
            } catch (e: Exception) {
                _doorCommandStatus.value = "Errore di connessione: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reconnectToMqtt() {
        if (!repo.isConnected()) {
            connectToMqtt()
        }
    }

    fun fetchData() {
        if (repo.isConnected()) {
            repo.requestDataUpdate()
            _doorCommandStatus.value = "Richiesta aggiornamento dati inviata"
        } else {
            _doorCommandStatus.value = "Non connesso al broker MQTT"
            connectToMqtt()
        }
    }

    fun sendDoorCommand(command: String, onComplete: (Boolean, String) -> Unit) {
        if (!repo.isConnected()) {
            onComplete(false, "Non connesso al broker MQTT")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repo.sendDoorCommand(command)
                if (success) {
                    val message = "Comando $command inviato con successo"
                    _doorCommandStatus.value = message
                    onComplete(true, message)
                } else {
                    val message = "Errore nell'invio del comando $command"
                    _doorCommandStatus.value = message
                    onComplete(false, message)
                }
            } catch (e: Exception) {
                val message = "Errore di comunicazione: ${e.message}"
                _doorCommandStatus.value = message
                onComplete(false, message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.disconnect()
    }
}