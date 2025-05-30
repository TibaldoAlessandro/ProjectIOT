package com.example.projectiot

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "car_distance_channel"
        private const val DISTANCE_THRESHOLD_METERS = 50.0 // 50 metri di distanza massima
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 secondi
        private const val FASTEST_LOCATION_INTERVAL = 5000L // 5 secondi

        // StateFlow per condividere lo stato del servizio
        private val _isMonitoring = MutableStateFlow(false)
        val isMonitoring: StateFlow<Boolean> = _isMonitoring

        private val _currentDistance = MutableStateFlow<Double?>(null)
        val currentDistance: StateFlow<Double?> = _currentDistance
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var carLocation: Location? = null
    private var isCarUnlocked = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
        Log.d(TAG, "LocationService creato")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MONITORING" -> {
                val lat = intent.getDoubleExtra("car_lat", 0.0)
                val lon = intent.getDoubleExtra("car_lon", 0.0)
                val unlocked = intent.getBooleanExtra("car_unlocked", false)
                startMonitoring(lat, lon, unlocked)
            }
            "STOP_MONITORING" -> {
                stopMonitoring()
            }
            "UPDATE_CAR_STATUS" -> {
                val unlocked = intent.getBooleanExtra("car_unlocked", false)
                updateCarStatus(unlocked)
            }
        }
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                locationResult.lastLocation?.let { userLocation ->
                    carLocation?.let { carLoc ->
                        val distance = calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            carLoc.latitude, carLoc.longitude
                        )

                        _currentDistance.value = distance

                        Log.d(TAG, "Distanza dall'auto: ${distance.toInt()}m - Auto sbloccata: $isCarUnlocked")

                        // Invia notifica solo se l'auto è sbloccata e la distanza supera la soglia
                        if (isCarUnlocked && distance > DISTANCE_THRESHOLD_METERS) {
                            sendDistanceNotification(distance)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startMonitoring(carLat: Double, carLon: Double, carUnlocked: Boolean) {
        carLocation = Location("car").apply {
            latitude = carLat
            longitude = carLon
        }
        isCarUnlocked = carUnlocked

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permessi di localizzazione non concessi")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
            setWaitForAccurateLocation(false)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        _isMonitoring.value = true
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        Log.d(TAG, "Monitoraggio iniziato per posizione: $carLat, $carLon")
    }

    private fun stopMonitoring() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isMonitoring.value = false
        _currentDistance.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Monitoraggio fermato")
    }

    private fun updateCarStatus(unlocked: Boolean) {
        isCarUnlocked = unlocked
        Log.d(TAG, "Stato auto aggiornato - Sbloccata: $unlocked")
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Raggio della Terra in metri

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun sendDistanceNotification(distance: Double) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 Attenzione!")
            .setContentText("Ti sei allontanato troppo dall'auto sbloccata (${distance.toInt()}m)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Ti sei allontanato di ${distance.toInt()} metri dall'auto che è ancora sbloccata. Considera di bloccare le portiere per sicurezza."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Monitoraggio Auto")
            .setContentText("Sto monitorando la distanza dall'auto")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoraggio Distanza Auto",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per avvisi quando ti allontani dall'auto sbloccata"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "LocationService distrutto")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}