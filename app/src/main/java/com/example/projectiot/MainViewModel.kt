package com.example.projectiot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repo = Repository()

    private val _gps = MutableStateFlow<GpsData?>(null)
    val gps: StateFlow<GpsData?> = _gps

    private val _doors = MutableStateFlow<DoorData?>(null)
    val doors: StateFlow<DoorData?> = _doors

    private val _presence = MutableStateFlow<PresenceData?>(null)
    val presence: StateFlow<PresenceData?> = _presence

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _doorCommandStatus = MutableStateFlow<String?>(null)
    val doorCommandStatus: StateFlow<String?> = _doorCommandStatus

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _gps.value = repo.getGps()
                _doors.value = repo.getDoors()
                _presence.value = repo.getPresence()
            } catch (e: Exception) {
                e.printStackTrace()
                _doorCommandStatus.value = "Errore nel caricamento dei dati"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendDoorCommand(command: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repo.controlDoors(command)
                if (response.isSuccessful) {
                    _doorCommandStatus.value = "Comando $command inviato con successo"
                    onComplete(true, "Comando $command inviato con successo")
                    // Aggiorna i dati delle porte dopo il comando
                    fetchData()
                } else {
                    _doorCommandStatus.value = "Errore nell'invio del comando"
                    onComplete(false, "Errore nell'invio del comando")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _doorCommandStatus.value = "Errore di connessione"
                onComplete(false, "Errore di connessione: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStatus() {
        _doorCommandStatus.value = null
    }
}