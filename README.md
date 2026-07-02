🐕 Lomito Seguro

**Integrantes del equipo:**
- Camarillo Olaez Juana Jaqueline (1223100400)
- Guerrero Sánchez Princes Rocio (1223100399)
- Rios Rios Carol Guadalupe (1223100430)

**Grupo:** GIDS6092-E
**Fecha de entrega:** 1/Julio/2026
**Profesor:** Anastacio Rodríguez García
**Asignatura:** Desarrollo para Dispositivos Inteligentes

---

## 📌 Objetivo

**Lomito Seguro** es un ecosistema tecnológico diseñado para la búsqueda y reporte de mascotas perdidas en comunidades como Dolores Hidalgo, Guanajuato. Conecta de forma nativa **dos tipos de dispositivos** —móvil y wearable (Wear OS)— a través de una API REST propia, permitiendo a dueños y ciudadanos colaborar en tiempo real para localizar mascotas extraviadas.

> 📺 El módulo de **Smart TV** está contemplado en el diseño original del ecosistema pero **aún no se ha desarrollado**, ya que hasta el momento la entrega solicitada abarca únicamente los módulos móvil y wear.

---

## 🚀 Funcionalidades Principales

### 📱 Aplicación Móvil (Android — módulo `mobile`)
| Pantalla / Componente | Funcionalidad |
|------------------------|---------------|
| **MainActivity** | Actividad principal, host de navegación (Navigation Component) y punto de comunicación con el smartwatch (`WatchReportListener`). |
| **LoginFragment / RegisterFragment** | Autenticación de usuarios contra el backend (`AuthViewModel`). |
| **HomeFragment** | Dashboard principal con tarjetas de mascotas registradas (`MascotaCardAdapter`). |
| **CrearMascotaFragment** | Formulario para registrar una nueva mascota. |
| **MascotaDetailFragment** | Detalle de una mascota específica, con opciones de edición/eliminación (`MascotaViewModel`). |
| **MuralFragment** | Mural comunitario de mascotas perdidas (`MascotaPerdidaAdapter`), recibe actualizaciones vía broadcast (`mascotaPerdidaReceiver`). |
| **AlertasFragment** | Historial de alertas, con opción de marcar como leídas (`AlertasViewModel`, `AlertasRepository`). |
| **RefugiosFragment** | Listado de refugios (`RefugioAdapter`, `RefugiosViewModel`). |
| **SimulatorFragment** | Simula el envío de datos (distancia/ubicación) hacia el smartwatch a través de la Wearable Data Layer. |

### ⌚ Aplicación Wear OS (módulo `wear`, Jetpack Compose)
| Pantalla / Componente | Funcionalidad |
|------------------------|---------------|
| **WearMainActivity** | Pantalla principal del reloj, escucha eventos BLE simulados (`bleReceiver`). |
| **DashboardActivity** | Menú principal en grid con accesos rápidos (`MenuItem`). |
| **SelectionActivity** | Selección de la mascota a monitorear. |
| **MascotaListActivity / MascotaDetailActivity** | Listado y detalle de mascotas sincronizadas desde el móvil. |
| **AddMascotaActivity** | Registro rápido de una mascota desde el reloj. |
| **MarcarPerdidaActivity** | Marca una mascota propia como perdida. |
| **ReportActivity / ReportarAvistamientoActivity / AgregarMascotaPerdidaActivity** | Flujo para reportar el avistamiento de una mascota perdida por parte de cualquier usuario. |
| **AlertActivity** | Pantalla de alerta (activada por distancia/BLE simulado). |
| **SettingsActivity** | Configuración de mascotas y preferencias del reloj (`WatchPreferences`). |
| **PollingService / WearMessageService** | Servicios encargados de sincronizar datos con el móvil vía Wearable Data Layer (Message API). |

---

## 🛠️ Tecnologías utilizadas

| Componente | Tecnología | Justificación |
|------------|------------|---------------|
| **Lenguaje** | Kotlin | Desarrollo nativo para Android y Wear OS. |
| **IDE** | Android Studio | Entorno oficial para desarrollo de aplicaciones Android. |
| **Build system** | Gradle (Kotlin DSL, `libs.versions.toml`) | Gestión de dependencias por módulo (`mobile`, `wear`). |
| **UI Móvil** | Views + Material Design + View/Data Binding | Fragmentos con `ItemAlertaBinding`, `ItemRefugioBinding`, etc. |
| **Navegación (Móvil)** | Navigation Component + Safe Args | `nav_graph.xml` y clases `*Directions` / `*Args` generadas. |
| **UI Wear OS** | Jetpack Compose para Wear OS | Pantallas construidas con composables (`*Kt.class` generados). |
| **Comunicación móvil ↔ wear** | Wearable Data Layer API (MessageClient) | `WatchReportListener` (móvil) y `WearMessageService` / `PollingService` (wear). |
| **Simulación de datos** | `BleState`, `SimulatorFragment` | Simulación de señal BLE / distancia entre dueño y mascota. |
| **Networking** | Retrofit | `LomitoApi`, `RetrofitClient` consumen la API REST del backend. |
| **Backend** | Node.js (API REST propia) | Autenticación, mascotas, reportes y alertas expuestos vía endpoints REST. |
| **Control de versiones** | Git y GitHub | Gestión y seguimiento del código fuente. |

---

## 📋 Instrucciones para ejecutar el proyecto

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/carolross1/lomito-front.git
   cd lomito-front
   ```

2. **Abre el proyecto en Android Studio**

   - Ve a `File > Open` y selecciona la carpeta raíz del proyecto (contiene `settings.gradle.kts`).
   - Espera a que se sincronicen las dependencias (Gradle). El proyecto tiene dos módulos: `mobile` y `wear`.

3. **Configura los dispositivos**

   - **Móvil:** Crea un AVD con API nivel 29+.
   - **Smartwatch:** Crea un AVD de Wear OS (pantalla circular, API 30+).

4. **Configura la conexión al backend (Node.js)**

   - Asegúrate de tener el servidor Node.js corriendo (local o desplegado).
   - Verifica que la URL base configurada en `RetrofitClient` apunte a la dirección correcta del backend.

5. **Simula la comunicación móvil → wear**

   - La app móvil envía datos simulados (distancia, GPS) al smartwatch a través de la Wearable Data Layer (`WatchReportListener`).
   - Asegúrate de que ambos dispositivos (emuladores o físicos) estén vinculados entre sí (Wear OS emparejado con el móvil).

6. **Ejecuta la aplicación**

   - Selecciona la configuración de ejecución `mobile` o `wear` según el dispositivo deseado.
   - Haz clic en el botón verde Run (▶) o presiona `Shift + F10`.

7. **Genera el APK (para entrega)**

   - Ve a `Build > Build Bundle(s) / APK(s) > Build APK(s)` sobre el módulo correspondiente (`mobile` o `wear`).
   - El archivo se generará en `mobile/build/outputs/apk/debug/` o `wear/build/outputs/apk/debug/`.

---

## 📁 Estructura del repositorio

```
lomito-front/
├── build.gradle.kts               # Configuración raíz de Gradle
├── settings.gradle.kts            # Declaración de módulos (mobile, wear)
├── gradle.properties
├── gradle/
│   └── libs.versions.toml         # Catálogo de versiones de dependencias
│
├── mobile/                        # Módulo de la app Android (móvil)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lomito/seguro/
│       │   ├── MainActivity.kt
│       │   ├── WatchReportListener.kt
│       │   ├── data/
│       │   │   ├── api/            # LomitoApi, RetrofitClient
│       │   │   ├── model/          # Mascota, Alerta, Refugio, Usuario, etc.
│       │   │   └── repository/     # LomitoRepository, AlertasRepository
│       │   ├── ui/
│       │   │   ├── alertas/
│       │   │   ├── auth/
│       │   │   ├── home/
│       │   │   ├── mascota/
│       │   │   ├── mural/
│       │   │   ├── refugios/
│       │   │   └── simulator/
│       │   └── util/               # Extensions, SessionManager
│       └── res/                    # layouts, drawables, navigation, menu, values
│
├── wear/                          # Módulo de la app Wear OS
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lomito/seguro/wear/
│       │   ├── data/               # BleState, PollingService, WatchPreferences,
│       │   │                       # WatchViewModel, WearMessageService
│       │   └── ui/
│       │       ├── alert/
│       │       ├── dashboard/
│       │       ├── home/
│       │       ├── mascota/
│       │       ├── report/
│       │       ├── selection/
│       │       └── settings/
│       └── res/                    # values, network_security_config.xml
│
└── README.md
```

---

## 🔄 Flujo de Comunicación entre Dispositivos (con simulación)

1. **Registro de mascota:** El dueño registra a su mascota desde la app móvil (`CrearMascotaFragment`). Los datos se envían a la API REST (Node.js) vía `LomitoRepository`/`LomitoApi`.

2. **Simulación de distancia (BLE):** La app móvil simula la distancia entre la mascota y el dueño desde `SimulatorFragment` (en lugar de usar un collar BLE real). Estos datos se envían al smartwatch a través de la Wearable Data Layer.

3. **Alerta de distancia:** Cuando el reloj recibe una distancia simulada que supera el umbral configurado, se lanza `AlertActivity`, que vibra y notifica al usuario.

4. **Reporte de avistamiento:** Desde el reloj, cualquier persona puede iniciar el flujo `ReportActivity` → `ReportarAvistamientoActivity` para reportar un avistamiento, el cual se envía a la API REST con las coordenadas correspondientes.

5. **Actualización del mural:** La app móvil (`MuralFragment`) consulta periódicamente el backend y se actualiza mediante un `BroadcastReceiver` (`mascotaPerdidaReceiver`) al detectar cambios en mascotas perdidas/encontradas.

6. **Sincronización móvil ↔ wear:** `WatchReportListener` (móvil) y `WearMessageService` / `PollingService` (wear) mantienen sincronizados los datos entre ambos dispositivos usando la Wearable Data Layer API.

---

## 🗄️ Backend (Node.js — API REST)

El backend expone endpoints REST consumidos por Retrofit desde la app móvil y, de forma indirecta, desde el wear a través del móvil. Entre los recursos principales gestionados por la API se encuentran:

| Recurso | Descripción |
|---------|-------------|
| **Auth** | Login y registro de usuarios (`LoginRequest`, `RegisterRequest`, `Usuario`). |
| **Mascotas** | Alta, edición, consulta y eliminación de mascotas (`Mascota`, `CreateMascotaRequest`). |
| **Reportes de vista** | Registro de avistamientos reportados por la comunidad (`ReporteRequest`, `ReporteVista`). |
| **Alertas** | Alertas generadas por distancia o avistamientos (`Alerta`). |
| **Refugios** | Información de refugios locales (`Refugio`). |
| **Ubicación** | Envío de coordenadas simuladas (`UbicacionRequest`). |

> Nota: la documentación específica de endpoints (rutas, métodos y payloads) depende del repositorio del backend Node.js, que es un proyecto independiente de este repositorio frontend.

---

## 👥 Roles del Sistema

- **Rol 1 — Dueño de mascota:** Registra mascotas, monitorea distancia simulada, recibe alertas y busca si se pierde.
- **Rol 2 — Ciudadano / reportador:** Cualquier persona que encuentra una mascota perdida y reporta su ubicación usando el smartwatch o la app móvil.

> El rol de **Comunidad / espectador vía Smart TV** está planeado dentro del ecosistema completo de Lomito Seguro, pero su desarrollo aún no ha iniciado.

---

## 🗺️ Roadmap

- [x] App móvil (autenticación, registro de mascotas, mural, alertas, refugios, simulador).
- [x] App Wear OS (dashboard, selección de mascota, alertas, reporte de avistamientos, ajustes).
- [ ] Módulo Smart TV (mural comunitario, streaming de refugios) — pendiente, no solicitado aún en la entrega actual.

---


## Imágenes


## 📄 Licencia

Este proyecto fue desarrollado como parte de la asignatura Desarrollo para Dispositivos Inteligentes en la Universidad Tecnológica del Norte de Guanajuato, bajo la supervisión del profesor Anastacio Rodríguez García.
Uso académico exclusivamente.
