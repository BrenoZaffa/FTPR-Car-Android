package com.example.myapitest.model

data class Car(
    val id: String,
    val value: CarValue
)

data class CarValue(
    val id: String,
    val name: String,
    val year: String,
    val licence: String,
    val imageUrl: String,
    val place: CarPlace?
)

data class CarPlace(
    val lat: Double,
    val long: Double
)