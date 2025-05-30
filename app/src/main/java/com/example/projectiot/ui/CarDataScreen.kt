package com.example.projectiot.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectiot.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*

@SuppressLint("UnrememberedMutableState", "DefaultLocale")
@Composable
fun CarDataScreen(viewModel: MainViewModel = viewModel()) {
    val gps by viewModel.gps.collectAsState()
    val doors by viewModel.doors.collectAsState()
    val presence by viewModel.presence.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val initialLatLng = LatLng(45.07, 7.69) // posizione di default

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
    }

    // Flag per sapere quando la mappa è pronta
    var mapLoaded by remember { mutableStateOf(false) }

    // Aggiorna la camera solo dopo che la mappa è caricata e gps è disponibile
    LaunchedEffect(gps, mapLoaded) {
        if (mapLoaded && gps != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(gps!!.lat, gps!!.lon), 15f)
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "🚗 Controllo Veicolo IoT",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📍 Posizione GPS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (gps != null) {
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            cameraPositionState = cameraPositionState,
                            onMapLoaded = { mapLoaded = true }
                        ) {
                            Marker(
                                state = MarkerState(position = LatLng(gps!!.lat, gps!!.lon)),
                                title = "Auto",
                                snippet = "Posizione attuale"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Coordinate: ${String.format("%.6f", gps!!.lat)}, ${String.format("%.6f", gps!!.lon)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator()
                            } else {
                                Text("🕒 Caricamento posizione...")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Informazioni veicolo
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📊 Stato Veicolo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🚪 Porta Anteriore:")
                            Text(
                                if (doors?.front == false) "🔓 Aperta" else "🔒 Chiusa",
                                color = if (doors?.front == false) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("🚪 Porta Posteriore:")
                            Text(
                                if (doors?.back == false) "🔓 Aperta" else "🔒 Chiusa",
                                color = if (doors?.back == false) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("👤 Presenza Passeggero:")
                    Text(
                        if (presence?.presence == true) "✅ Presente" else "❌ Assente",
                        color = if (presence?.presence == true) Color.Green else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controlli
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🎮 Controlli",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bottone Blocca
                        Button(
                            onClick = {
                                viewModel.sendDoorCommand("lock") { _, _ ->
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("🔒 Blocca")
                            }
                        }

                        // Bottone Sblocca
                        Button(
                            onClick = {
                                viewModel.sendDoorCommand("unlock") { _, _ ->
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("🔓 Sblocca")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottone Aggiorna
                    Button(
                        onClick = { viewModel.fetchData() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("🔄 ${if (isLoading) "Caricamento..." else "Aggiorna Dati"}")
                    }
                }
            }
        }
    }
}