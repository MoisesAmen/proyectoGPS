package com.mss.proyectoGPS

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import retrofit2.Call

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Crear el LocationRequest para actualizaciones periódicas
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Cada 5 segundos
            .setMinUpdateDistanceMeters(5f) // Solo si se ha movido más de 5 metros
            .build()

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }

        // Pedir permiso al iniciar
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        val startLocationUpdates: (onLocationUpdate: (LatLng) -> Unit) -> Unit = { callback ->
            startLocationUpdates(callback)
        }

        val stopLocationUpdates: () -> Unit = {
            stopLocationUpdates()
        }

        setContent {

            // Crear NavController
            val navController = rememberNavController()

            NavHost(navController, startDestination = "home") {
                composable("home") {
                    LocationApp(
                        startLocationUpdates = startLocationUpdates,
                        stopLocationUpdates = stopLocationUpdates,
                        navController = navController
                    )
                }
                composable("savedRoutes") {
                    SavedRoutesScreen()
                }
            }

        }



    }

    private fun startLocationUpdates(onLocationUpdate: (LatLng) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Configurar el LocationCallback
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    if (location != null) {
                        // Enviar la ubicación actual al componente de la UI
                        onLocationUpdate(LatLng(location.latitude, location.longitude))
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        } else {
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

@Composable
fun LocationApp(
    startLocationUpdates: (onLocationUpdate: (LatLng) -> Unit) -> Unit,
    stopLocationUpdates: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val route = remember { mutableStateListOf<LatLng>() }
    var routeName by remember { mutableStateOf("") }
    var isTracking by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Indicador de carga

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Sección de nombre de la ruta y botones
        RouteInputSection(
            routeName = routeName,
            isTracking = isTracking,
            onNameChange = { routeName = it },
            onStartTracking = {
                if (routeName.isBlank()) {
                    Toast.makeText(context, "Ingresa un nombre para la ruta", Toast.LENGTH_SHORT).show()
                } else {
                    isTracking = true
                    isLoading = true
                    startLocationUpdates { location ->
                        userLocation = location
                        route.add(location)
                        isLoading = false // Detener indicador de carga al recibir la primera ubicación
                    }
                }
            },
            onStopTracking = {
                isTracking = false
                stopLocationUpdates()
                showModal = true

                // Guardar la ruta en la base de datos
                val coordinatesToSave = route.map { Coordinate(it.latitude, it.longitude) }
                RetrofitClient.api.saveRoute(routeName, coordinatesToSave).enqueue(object : retrofit2.Callback<Route> {
                    override fun onResponse(call: Call<Route>, response: retrofit2.Response<Route>) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Ruta guardada con éxito", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al guardar la ruta", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Route>, t: Throwable) {
                        Toast.makeText(context, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para navegar a rutas guardadas
        Button(onClick = { navController.navigate("savedRoutes") }) {
            Text(text = "Ver rutas guardadas")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar mapa o indicador de carga
        if (isTracking) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text(text = "Cargando ubicación...")
            } else {
                userLocation?.let { location ->
                    GoogleMapWithRoute(location, route)
                }
            }
        }
    }

    // Mostrar el resumen de la ruta en un modal al finalizar el seguimiento
    if (showModal) {
        RouteSummaryModal(route = route) { showModal = false }
    }
}

@Composable
fun RouteInputSection(
    routeName: String,
    isTracking: Boolean,
    onNameChange: (String) -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = routeName,
            onValueChange = { if (!isTracking) onNameChange(it) }, // Deshabilitar edición mientras se rastrea
            label = { Text("Nombre de la ruta") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = !isTracking // Campo deshabilitado durante el seguimiento
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!isTracking) {
            Button(onClick = onStartTracking) {
                Text(text = "Iniciar seguimiento de ruta")
            }
        } else {
            Button(onClick = onStopTracking) {
                Text(text = "Finalizar seguimiento")
            }
        }
    }
}



@Composable
fun GoogleMapWithRoute(currentLocation: LatLng, route: List<LatLng>) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(currentLocation)
            .zoom(15f)
            .build()
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        cameraPositionState = cameraPositionState
    ) {
        // Marcador para la ubicación actual
        Marker(
            state = MarkerState(position = currentLocation),
            title = "Mi ubicación actual"
        )

        // Dibujar la ruta
        Polyline(
            points = route,
            color = androidx.compose.ui.graphics.Color.Blue,
            width = 5f
        )
    }
}

@Composable
fun RouteSummaryModal(route: List<LatLng>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cerrar")
            }
        },
        title = { Text("Resumen de la ruta") },
        text = {
            Column {
                Text("Ruta seguida:")
                Spacer(modifier = Modifier.height(8.dp))
                GoogleMapWithRoute(route.lastOrNull() ?: LatLng(0.0, 0.0), route)
            }
        }
    )
}

@Composable
fun SavedRoutesScreen() {
    var savedRoutes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    val context = LocalContext.current

    // Cargar las rutas guardadas al iniciar
    LaunchedEffect(Unit) {
        RetrofitClient.api.getAllRoutes().enqueue(object : retrofit2.Callback<List<Route>> {
            override fun onResponse(call: Call<List<Route>>, response: retrofit2.Response<List<Route>>) {
                if (response.isSuccessful) {
                    savedRoutes = response.body() ?: emptyList()
                } else {
                    Toast.makeText(context, "Error al cargar rutas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Route>>, t: Throwable) {
                Toast.makeText(context, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Lista de seguimientos guardados
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Seguimientos Guardados", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

        // Mostrar cada ruta guardada como un elemento seleccionable
        savedRoutes.forEach { route ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        selectedRoute = route // Establecer la ruta seleccionada para abrir el modal
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = route.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    // Mostrar el modal si hay una ruta seleccionada
    selectedRoute?.let { route ->
        val lastCoordinate = route.coordinates.lastOrNull()
        val currentLocation = if (lastCoordinate != null) LatLng(lastCoordinate.latitude, lastCoordinate.longitude) else LatLng(0.0, 0.0)

        AlertDialog(
            onDismissRequest = { selectedRoute = null },
            confirmButton = {
                Button(onClick = { selectedRoute = null }) {
                    Text("Cerrar")
                }
            },
            title = { Text("Resumen de la ruta") },
            text = {
                Column {
                    Text("Ruta: ${route.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    GoogleMapWithRoute(currentLocation = currentLocation, route = route.coordinates.map { LatLng(it.latitude, it.longitude) })
                }
            }
        )
    }
}


