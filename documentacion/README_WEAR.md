# Guía Paso a Paso: Construyendo el Módulo Wear de Lomito Seguro

Esta guía documenta la construcción del módulo **Wear (Smartwatch / Wear OS)** de **Lomito Seguro**, enfocado en proveer acciones rápidas desde la muñeca para interactuar con la aplicación principal.

---

## Objetivo de Esta Guía

Al estudiar y seguir esta guía, comprenderás:

1. Cómo configurar un proyecto para **Wear OS** usando `Wear Compose`.
2. Cómo crear listas escalables (`ScalingLazyColumn`) adaptadas a pantallas circulares.
3. Patrones de comunicación e interfaces en relojes inteligentes.

---

## FASE 1: Dependencias del Módulo Wear

El módulo Wear usa una variante especial de Compose diseñada para pantallas pequeñas y redondas.

### Paso 1.1: `build.gradle.kts` (Módulo Wear)

```kotlin
dependencies {
    // Wear Compose Material
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation("androidx.wear.compose:compose-foundation:1.2.1")
    implementation("androidx.wear.compose:compose-navigation:1.2.1")
    
    // Play Services Wearable (Comunicación con el teléfono)
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
}
```

---

## FASE 2: Interfaz Wearable (UI)

### Paso 2.1: `ScalingLazyColumn`

A diferencia del `LazyColumn` normal, Wear OS usa `ScalingLazyColumn` para encoger los elementos que se acercan a los bordes curvos de la pantalla, dando un efecto tridimensional.

```kotlin
@Composable
fun MenuWearScreen(onActionClick: (String) -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item {
            Chip(
                onClick = { onActionClick("REPORTE_RAPIDO") },
                label = { Text("Reporte Rápido") },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        
        item {
            Chip(
                onClick = { onActionClick("VER_REFUGIOS") },
                label = { Text("Refugios") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
```

---

## FASE 3: Comunicación Wear-Mobile (RPC)

Para enviar comandos desde el Smartwatch al Teléfono (ej. presionar "Reporte Rápido" en el reloj y que se abra el GPS en el teléfono), se utiliza **Data Layer API** (WearableListenerService).

### Paso 3.1: Enviar Mensaje (MessageClient)

```kotlin
fun enviarMensajeAlTelefono(context: Context, path: String, payload: String) {
    val messageClient = Wearable.getMessageClient(context)
    val nodesClient = Wearable.getNodeClient(context)
    
    nodesClient.connectedNodes.addOnSuccessListener { nodes ->
        for (node in nodes) {
            messageClient.sendMessage(node.id, path, payload.toByteArray())
        }
    }
}
```
*En Lomito Seguro, esto permite crear reportes SOS de mascotas perdidas instantáneamente desde el reloj.*
