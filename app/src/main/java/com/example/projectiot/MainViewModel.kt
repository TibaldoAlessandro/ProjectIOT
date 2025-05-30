package com.example.projectiot

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
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

    // StateFlow per il monitoraggio della distanza
    private val _isLocationMonitoringEnabled = MutableStateFlow(false)
    val isLocationMonitoringEnabled: StateFlow<Boolean> = _isLocationMonitoringEnabled

    // StateFlow per la distanza corrente dall'auto
    val currentDistance: StateFlow<Double?> = LocationService.currentDistance
    val isMonitoringActive: StateFlow<Boolean> = LocationService.isMonitoring

    // Combina i dati per determinare se l'auto è sbloccata
    private val isCarUnlocked = combine(doors) { doorsArray ->
        val doors = doorsArray[0] // prendi il primo elemento dell'array
        doors?.let { doorData ->
            !(doorData.front && doorData.back)
        } ?: false
    }

    init {
        // Connessione automatica all'avvio
        connectToMqtt()

        // Monitora lo stato delle porte e aggiorna il servizio di localizzazione
        viewModelScope.launch {
            combine(gps, isCarUnlocked, _isLocationMonitoringEnabled) { gps, unlocked, monitoringEnabled ->
                Triple(gps, unlocked, monitoringEnabled)
            }.collect { (gps, unlocked, monitoringEnabled) ->
                if (monitoringEnabled && gps != null) {
                    updateLocationService(gps.lat, gps.lon, unlocked)
                }
            }
        }
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleLocationMonitoring() {
        _isLocationMonitoringEnabled.value = !_isLocationMonitoringEnabled.value

        if (!_isLocationMonitoringEnabled.value) {
            stopLocationService()
        } else {
            // Se abbiamo i dati GPS, avvia subito il servizio
            gps.value?.let { gpsData ->
                viewModelScope.launch {
                    isCarUnlocked.collect { unlocked ->
                        updateLocationService(gpsData.lat, gpsData.lon, unlocked)
                        return@collect
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateLocationService(lat: Double, lon: Double, unlocked: Boolean) {
        val context = getApplication<Application>().applicationContext

        if (_isLocationMonitoringEnabled.value) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = "START_MONITORING"
                putExtra("car_lat", lat)
                putExtra("car_lon", lon)
                putExtra("car_unlocked", unlocked)
            }
            context.startForegroundService(intent)
        } else {
            // Aggiorna solo lo stato dell'auto se il servizio è già attivo
            if (LocationService.isMonitoring.value) {
                val intent = Intent(context, LocationService::class.java).apply {
                    action = "UPDATE_CAR_STATUS"
                    putExtra("car_unlocked", unlocked)
                }
                context.startService(intent)
            }
        }
    }

    private fun stopLocationService() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, LocationService::class.java).apply {
            action = "STOP_MONITORING"
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        repo.disconnect()
        stopLocationService()
    }
}