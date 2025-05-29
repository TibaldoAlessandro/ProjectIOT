package com.example.projectiot

data class GpsData(val lat: Double, val lon: Double)
data class DoorData(val front: Boolean, val back: Boolean)
data class PresenceData(val presence: Boolean)