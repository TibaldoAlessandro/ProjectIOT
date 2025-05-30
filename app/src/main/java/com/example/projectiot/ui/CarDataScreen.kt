package com.example.projectiot.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.projectiot.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnrememberedMutableState", "DefaultLocale")
@Composable
fun CarDataScreen(viewModel: MainViewModel = viewModel()) {
    val gps by viewModel.gps.collectAsState()
    val doors by viewModel.doors.collectAsState()
    val presence by viewModel.presence.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLocationMonitoringEnabled by viewModel.isLocationMonitoringEnabled.collectAsState()
    val isMonitoringActive by viewModel.isMonitoringActive.collectAsState()
    val currentDistance by viewModel.currentDistance.collectAsState()

    val context = LocalContext.current
    val initialLatLng = LatLng(45.07, 7.69) // posizione di default

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
    }

    // Flag per sapere quando la mappa è pronta
    var mapLoaded by remember { mutableStateOf(false) }

    // Gestione permessi di localizzazione
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.toggleLocationMonitoring()
        }
    }

    // Controlla se i permessi sono già concessi
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

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

            // Card per il monitoraggio della distanza
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocationMonitoringEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🔔 Notifiche Distanza",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Avviso se ti allontani dall'auto sbloccata",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isLocationMonitoringEnabled,
                            onCheckedChange = {
                                if (hasLocationPermission) {
                                    viewModel.toggleLocationMonitoring()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        )
                    }

                    if (isLocationMonitoringEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isMonitoringActive) "🟢 Attivo" else "🟡 In avvio...",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isMonitoringActive) Color.Green else Color.Yellow,
                                fontWeight = FontWeight.Bold
                            )

                            currentDistance?.let { distance ->
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    "📏 Distanza: ${distance.toInt()}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (distance > 50) Color.Red else Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!hasLocationPermission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "⚠️ Permessi di localizzazione richiesti",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

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