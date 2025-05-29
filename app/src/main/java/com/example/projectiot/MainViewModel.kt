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

    fun fetchData() {
        viewModelScope.launch {
            try {
                _gps.value = repo.getGps()
                _doors.value = repo.getDoors()
                _presence.value = repo.getPresence()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
