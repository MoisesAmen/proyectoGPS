# Aplicación de Seguimiento GPS

Esta aplicación móvil Android permite a los usuarios rastrear y guardar rutas utilizando el GPS del dispositivo, desarrollada con Kotlin y Jetpack Compose.

## Características Principales

- Rastreo de ubicación en tiempo real
- Visualización de rutas en Google Maps
- Almacenamiento de rutas en servidor remoto
- Visualización de rutas guardadas
- Interfaz de usuario moderna con Material Design 3

## Requisitos Técnicos

- Android Studio Arctic Fox o superior
- SDK mínimo: Android 21 (5.0)
- SDK objetivo: Android 33 o superior
- Google Play Services (para Maps)
- Kotlin 1.8.0 o superior
- Jetpack Compose 1.4.0 o superior

## Dependencias Principales

```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-maps:18.1.0'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    implementation 'com.google.maps.android:maps-compose:2.11.4'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
}
```

## Configuración del Proyecto

1. Clonar el repositorio
2. Abrir el proyecto en Android Studio
3. Configurar una API Key de Google Maps en `AndroidManifest.xml`
4. Sincronizar el proyecto con Gradle
5. Ejecutar la aplicación

## Estructura del Proyecto

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/mss/proyectoGPS/
│   │   │   ├── MainActivity.kt         # Actividad principal y lógica de UI
│   │   │   ├── RetrofitClient.kt      # Cliente HTTP para API
│   │   │   ├── interfaz.kt            # Interfaces y modelos de datos
│   │   │   └── ui/theme/              # Estilos y temas de Material Design
│   │   └── res/                       # Recursos de la aplicación
```

## Funcionalidades Detalladas

### Rastreo de Ubicación
- Utiliza FusedLocationProviderClient para actualizaciones precisas
- Actualizaciones cada 5 segundos
- Distancia mínima de actualización: 5 metros

### Almacenamiento de Rutas
- Las rutas se guardan en un servidor remoto
- Cada ruta incluye:
  - Nombre único
  - Lista de coordenadas (latitud/longitud)

### Interfaz de Usuario
- Pantalla principal:
  - Campo para nombre de ruta
  - Botones de inicio/fin de rastreo
  - Mapa en tiempo real
  - Acceso a rutas guardadas
- Pantalla de rutas guardadas:
  - Lista de rutas almacenadas
  - Vista detallada de cada ruta

## API Backend

### Endpoints

- `POST /api/routes`: Guarda una nueva ruta
  - Parámetros: nombre y lista de coordenadas
- `GET /api/routes`: Obtiene todas las rutas guardadas

## Permisos Requeridos

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

## Uso de la Aplicación

1. Ingresar nombre para la nueva ruta
2. Presionar "Iniciar seguimiento de ruta"
3. La aplicación comenzará a rastrear la ubicación
4. El mapa mostrará la posición actual y la ruta
5. Presionar "Finalizar seguimiento" para guardar
6. Ver rutas guardadas en la sección correspondiente

## Contribución

1. Fork del repositorio
2. Crear rama para nueva característica
3. Commit de cambios
4. Push a la rama
5. Crear Pull Request


