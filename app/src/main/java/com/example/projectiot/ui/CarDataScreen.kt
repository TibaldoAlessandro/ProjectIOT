package com.example.projectiot.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projectiot.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*

@SuppressLint("UnrememberedMutableState")
@Composable
fun CarDataScreen(viewModel: MainViewModel = viewModel()) {
    val gps by viewModel.gps.collectAsState()
    val doors by viewModel.doors.collectAsState()
    val presence by viewModel.presence.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        gps?.let {
            position = CameraPosition.fromLatLngZoom(LatLng(it.lat, it.lon), 15f)
        }
    }

    LaunchedEffect(gps) {
        gps?.let {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(it.lat, it.lon), 15f))
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            Text("🚗 Dati veicolo", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(8.dp))

            if (gps != null) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = LatLng(gps!!.lat, gps!!.lon)),
                        title = "Auto",
                        snippet = "Posizione attuale"
                    )
                }
            } else {
                Text("🕒 Caricamento posizione...")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("📍 GPS: ${gps?.lat ?: "--"}, ${gps?.lon ?: "--"}")
            Text("🚪 Porte: Frontale: ${doors?.front ?: "--"}, Posteriore: ${doors?.back ?: "--"}")
            Text("👤 Presenza passeggero: ${presence?.driverPresent ?: "--"}")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { viewModel.fetchData() }) {
                Text("🔄 Aggiorna dati")
            }
        }
    }
}