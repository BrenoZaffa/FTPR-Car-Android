package com.example.myapitest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapitest.databinding.ActivityCarDetailBinding
import com.example.myapitest.model.CarPlace
import com.example.myapitest.model.CarValue
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CarDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailBinding
    private lateinit var item: CarValue
    private lateinit var mMap: GoogleMap
    private var selectedMarker: Marker? = null
    private lateinit var imageUri: Uri
    private var imageFile: File? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if(it.resultCode == RESULT_OK){
            binding.imageUrl.setText("Imagem Obtida")
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        loadItem()
        requestLocationPermission()
        setupGoogleMap()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationPermission() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLong = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, 15f))
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (::item.isInitialized) {
            if(item.id != ""){
                // Carregue o mapa com as informações do item
                loadItemInGoogleMap()
            } else {
                // Caso o item não tenha um ID, carregue o mapa com a localização atual
                configMapToPin()
            }
        }
    }

    private fun configMapToPin(){
        mMap.uiSettings.isZoomControlsEnabled = true
        binding.mapContent.visibility = View.VISIBLE
        getDeviceLocation()
        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Lat: ${latLng.latitude}, Lng: ${latLng.longitude}")
            )
        }
    }

    private fun getDeviceLocation() {
        // verificar a permissão de localização
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loadCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocation() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        fusedLocationClient
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupView() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.deleteCar.setOnClickListener {
            deleteItem()
        }
        binding.saveCar.setOnClickListener {
            saveCar()
        }
        binding.takePicture.setOnClickListener {
            takePicture()
        }
    }

    private fun loadItem() {
        val itemId = intent.getStringExtra(ARG_ID) ?: ""

        if (itemId == "0" || itemId.isEmpty()) {
            binding.deleteCar.visibility = View.GONE
            item = CarValue(
                id = "",
                name = "",
                year = "",
                licence = "",
                imageUrl = "",
                place = null
            )
            if (!::mMap.isInitialized) return
            else configMapToPin()

            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.carApiService.getCar(itemId) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        item = result.data.value
                        handleSuccess()
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Erro ao buscar o carro",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun handleSuccess() {
        binding.imageUrl.isEnabled = false
        binding.takePicture.visibility = View.GONE
        binding.imageUrl.visibility = View.GONE

        binding.name.setText(item.name)
        binding.year.setText(item.year)
        binding.licence.setText(item.licence)
        binding.imageUrl.setText(item.imageUrl)

        loadItemInGoogleMap()
    }

    private fun loadItemInGoogleMap() {
        if (!::mMap.isInitialized) return
        item.place?.let {
            binding.mapContent.visibility = View.VISIBLE
            val location = LatLng(it.lat, it.long)
            mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Lat: ${it.lat}, Lng: ${it.long}")
            )
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    location,
                    15f
                )
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            com.example.myapitest.CarDetailActivity.Companion.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(intent.getStringExtra(ARG_ID) == "0")
                        loadCurrentLocation()
                    else if (::item.isInitialized)
                        loadItemInGoogleMap()
                } else {
                    Toast.makeText(
                        this,
                        "Permissão de localização negada",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }
    }

    private fun deleteItem() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.carApiService.deleteCar(item.id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> handleSuccessDelete()
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Erro ao deletar o carro",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun handleSuccessDelete() {
        Toast.makeText(
            this,
            "Carro deletado com sucesso",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                com.example.myapitest.CarDetailActivity.Companion.LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageName = "JPEG_${timestamp}_"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(
            imageName,
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
    }

    private fun uploadImageToFirebase() {
        imageFile?.let {
            val storageRef = FirebaseStorage.getInstance().reference

            val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

            val baos = ByteArrayOutputStream()
            val imageBitmap = BitmapFactory.decodeFile(it.path)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            onLoadImage(true)

            imageRef.putBytes(data)
                .addOnFailureListener {
                    onLoadImage(false)
                    Toast.makeText(
                        this@CarDetailActivity,
                        "Falha ao realizar o upload da imagem",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnSuccessListener {
                    onLoadImage(false)
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        binding.imageUrl.setText(uri.toString())
                        insertCar()
                    }
                }
        }
    }

    private fun onLoadImage(isLoading: Boolean) {
        binding.loadImageProgress.visibility = if(isLoading) View.VISIBLE else View.GONE
        binding.takePicture.isEnabled = !isLoading
        binding.saveCar.isEnabled = !isLoading
    }

    private fun insertCar() {
        val name = binding.name.text.toString()
        val position = selectedMarker?.position?.let {
            CarPlace(
                it.latitude,
                it.longitude
            )
        }
        CoroutineScope(Dispatchers.IO).launch {
            val id = SecureRandom().nextInt().toString()
            val carValue = CarValue(
                id = id,
                name = name,
                licence = binding.licence.text.toString(),
                year = binding.year.text.toString(),
                imageUrl = binding.imageUrl.text.toString(),
                place = position
            )

            val result = safeApiCall { RetrofitClient.carApiService.addCar(carValue) }
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Carro inserido com sucesso",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Erro ao inserir o carro",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateCar() {
        val carro = CarValue(
            id = item.id,
            name = binding.name.text.toString(),
            licence = binding.licence.text.toString(),
            year = binding.year.text.toString(),
            imageUrl = item.imageUrl,
            place = item.place
        )
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.carApiService.updateCar(item.id, carro) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Carro atualizado com sucesso",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Erro ao atualizar o carro",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun saveCar() {
        if (!validateForm()) return

        if(intent.getStringExtra(ARG_ID) == "0"){
            uploadImageToFirebase()
        } else {
            updateCar()
        }
    }

    private fun validateForm(): Boolean {
        var hasError = false
        if (binding.name.text.isNullOrBlank()) {
            binding.name.error = getString(R.string.required_field)
            hasError = true
        }
        if (binding.licence.text.isNullOrBlank()) {
            binding.licence.error = getString(R.string.required_field)
            hasError = true
        }
        if (binding.year.text.isNullOrBlank()) {
            binding.year.error = getString(R.string.required_field)
            hasError = true
        }
        if (binding.imageUrl.text.isNullOrBlank()) {
            binding.imageUrl.error = getString(R.string.required_field)
            hasError = true
        }
        if(selectedMarker == null && item.id == ""){
            Toast.makeText(
                this,
                "Selecione um ponto no mapa",
                Toast.LENGTH_SHORT
            ).show()
            hasError = true
        }
        return !hasError
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val ARG_ID = "arg_id"

        fun newIntent(context: Context, carId: String): Intent {
            return Intent(context, CarDetailActivity::class.java).apply {
                putExtra(ARG_ID, carId)
            }
        }
    }
}
