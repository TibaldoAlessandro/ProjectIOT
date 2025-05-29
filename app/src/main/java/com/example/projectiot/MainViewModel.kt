package com.example.projectiot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repo = Repository()
    private val client = HttpClient()

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

//    fun sendDoorCommand(command: String, onComplete: () -> Unit) {
//        viewModelScope.launch {
//            try {
//                val BASE_URL = "http://192.168.178.198:1880"
//                val response: HttpResponse = client.post("$BASE_URL/api/doors") {
//                    contentType(ContentType.Application.Json)
//                    setBody("""{"command": "$command"}""")
//                }
//                if (response.status.isSuccess()) {
//                    onComplete()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        client.close() // Chiude il client per evitare leak di risorse
//    }
}
