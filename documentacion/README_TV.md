# Guía Paso a Paso: Construyendo el Módulo TV de Lomito Seguro

Esta guía documenta la construcción del módulo **TV (Android TV/Smart TV)** de **Lomito Seguro**, enfocado en la difusión de refugios locales y streaming en vivo para promover la adopción de mascotas desde la sala de estar.

---

## Objetivo de Esta Guía

Al estudiar y seguir esta guía, comprenderás:

1. Cómo configurar un proyecto para **Android TV** usando Jetpack Compose para TV (`androidx.tv.compose`).
2. Cómo implementar navegación optimizada para **D-Pad** (controles remotos) usando `LazyRow`.
3. Cómo integrar **ExoPlayer (`androidx.media3`)** para la reproducción en vivo de cámaras de seguridad/streaming de los refugios.

---

## FASE 1: Dependencias del Módulo TV

El módulo TV requiere dependencias específicas para la interfaz y la reproducción de media.

### Paso 1.1: `build.gradle.kts` (Módulo TV)

```kotlin
dependencies {
    // Compose para TV (Optimizaciones para D-Pad y Leanback)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    implementation("androidx.tv:tv-material:1.0.0-alpha10")
    
    // ExoPlayer para Streaming en Vivo (Media3)
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    
    // Retrofit (Reutilizado del cliente base)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}
```

---

## FASE 2: Interfaz del Dashboard (TV)

### Paso 2.1: `DashboardActivity.kt` y `LazyRow`

La pantalla principal de la TV muestra una cinta (`LazyRow`) horizontal con los refugios disponibles. Es crucial manejar el `onClick` para que funcione con el botón **Enter** del control remoto.

```kotlin
@Composable
fun DashboardTvScreen(refugios: List<Refugio>, onRefugioClick: (Refugio) -> Unit) {
    TvLazyRow(
        contentPadding = PaddingValues(32.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(refugios) { refugio ->
            Card(
                onClick = { onRefugioClick(refugio) },
                modifier = Modifier.size(width = 250.dp, height = 150.dp)
            ) {
                // Contenido de la tarjeta (Nombre del refugio, etc)
                Text(text = refugio.nombre)
            }
        }
    }
}
```

---

## FASE 3: Reproductor de Streaming en Vivo (ExoPlayer)

### Paso 3.1: `RefugioDifusionActivity.kt`

Al seleccionar un refugio, se abre una actividad que muestra el streaming de video del lugar, junto con la información de contacto a la derecha.

```kotlin
@Composable
fun VideoPlayerScreen(videoUrl: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // Es un live stream para TV, ocultamos controles
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}
```

**Flujo de Usuario:**
1. El usuario abre la App en su Smart TV.
2. Ve la lista de Refugios Locales (obtenidos del backend Node.js mediante Retrofit).
3. Selecciona un Refugio usando las flechas del control y da **Enter**.
4. La TV reproduce la transmisión en vivo de los perritos gracias a ExoPlayer.
