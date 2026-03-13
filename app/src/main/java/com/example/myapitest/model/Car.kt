package com.example.myapitest.model

data class CarValue(
    val id: String,
    val name: String,
    val year: String,
    val licence: String,
    val imageUrl: String,
    val place: CarPlace?
)

data class CarPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double
)