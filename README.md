# **🐕 Lomito Seguro**

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


### 📺 Aplicación Smart TV (módulo `tv`, Android TV)
| Pantalla / Componente | Funcionalidad |
|------------------------|---------------|
| **LomitoTvApp** | Clase de aplicación, punto de entrada del módulo TV. |
| **DashboardActivity** | Pantalla principal del módulo TV, con `DashboardViewModel` gestionando el estado del menú/inicio. |
| **MascotaDetalleActivity** | Detalle de una mascota (perdida o en seguimiento), con `MascotaDetalleViewModel` y `MapaView` para mostrar su ubicación en un mapa. |
| **MascotaPerfilActivity** | Perfil de la mascota (datos generales), gestionado por `MascotaPerfilViewModel`. |
| **RefugioDifusionActivity** | Pantalla de difusión de refugios en pantalla grande, con `RefugioDifusionViewModel`. |
| **LomitoTvApi / RetrofitClient** | Interfaz Retrofit y cliente HTTP configurados para las peticiones del módulo TV. |
| **Models** | Modelos de datos (mascotas, refugios, etc.) usados en el módulo TV. |
| **LomitoTvRepository** | Repositorio que conecta la API con los ViewModels de las pantallas. |
| **Theme** | Definición del tema visual (colores, tipografía) para las pantallas de TV. |
---

## Validación del Proyecto y aprobación del beneficiario (video)

Link del video: https://drive.google.com/file/d/1bBsdhZP3Ac88JmY6P5wFixVLnhpm_m3E/view?usp=drive_link 

## Carta de validación del proyecto y aprobación del beneficiario

<table>
   <tr>
      <td><img width="1204" height="1600" alt="imagen1" src="https://github.com/user-attachments/assets/1d1686ca-62dd-4dac-af31-deb5358d8f67" /></td>
   </tr>
   <tr>
      <td><img width="1204" height="1600" alt="imagen2" src="https://github.com/user-attachments/assets/07781686-5761-474b-a9e8-977eedf25ef4" /></td>
   </tr>
   <tr>
      <td><img width="1204" height="1600" alt="imagen3" src="https://github.com/user-attachments/assets/8fb817a7-b0c1-4d7f-ac03-68c05ab729ab" /></td>
   </tr>
</table>

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
- [x] Módulo Smart TV (mural comunitario, streaming de refugios) — pendiente, no solicitado aún en la entrega actual.

---


## Imágenes
### Capturas de pantalla — Móvil

<table>
  <tr>
    <td><img width="260" alt="image1" src="https://github.com/user-attachments/assets/eb5ac2eb-6b00-48a3-9b05-0eee0126764d" /></td>
    <td><img width="260" alt="image2" src="https://github.com/user-attachments/assets/dbaf60ce-897e-4e55-b8f3-d89066d36238" /></td>
    <td><img width="260" alt="image3" src="https://github.com/user-attachments/assets/46217ccf-a21a-4ba5-82ab-a541f0bc16bb" /></td>
  </tr>
</table>

### Capturas de pantalla — watch

<table>
  <tr>
    <td><img width="260" alt="image4" src="https://github.com/user-attachments/assets/23b0fd1b-e414-4c0a-9205-53265bdf618c" /></td>
    <td><img width="260" alt="image5" src="https://github.com/user-attachments/assets/06e2c699-0dcf-4e7d-9047-56838fe8e200" /></td>
    <td><img width="260" alt="image6" src="https://github.com/user-attachments/assets/43607ea8-344c-4011-95b0-ec2ac16e609d" /></td>
  </tr>
  <tr>
    <td><img width="260" alt="image7" src="https://github.com/user-attachments/assets/676e5026-06d0-4b4f-abe9-872d320c6d4a" /></td>
    <td><img width="260" alt="image8" src="https://github.com/user-attachments/assets/1868a020-ff59-45a3-87ec-e2cad9e69c2b" /></td>
    <td><img width="260" alt="image9" src="https://github.com/user-attachments/assets/f544a992-7246-4dc6-b121-62f86af7861d" /></td>
  </tr>
  <tr>
    <td><img width="260" alt="image10" src="https://github.com/user-attachments/assets/8912382c-c51c-4195-b70c-0c29ce408279" /></td>
    <td><img width="260" alt="image11" src="https://github.com/user-attachments/assets/cf8cf682-2d21-49f3-8494-e8f28906f9c8" /></td>
    <td><img width="260" alt="image12" src="https://github.com/user-attachments/assets/7057c242-0558-4109-88c4-a1c64a2760bb" /></td>
    <td> <img width="436" alt="image13" src="https://github.com/user-attachments/assets/1149910a-1f25-437f-b55f-1d344ecc3bc7" /></td>

  </tr>
</table>

### Capturas de pantalla - Smart TV

<table>
   <tr>
      <td><img width="1206" height="684" alt="image14" src="https://github.com/user-attachments/assets/f7cf1f75-2236-409e-96e6-0413f2f7e043" /></td>
      <td> <img width="1185" height="649" alt="image15" src="https://github.com/user-attachments/assets/19c7cb32-8318-4b76-91a3-0ac009f12e6f" /></td>
     
   </tr>
   <tr>
      <td><img width="1189" height="649" alt="image16" src="https://github.com/user-attachments/assets/ac858cbd-8b07-48e8-a472-0aaf1aa50fe6" /></td>
      <td><img width="1195" height="670" alt="image17" src="https://github.com/user-attachments/assets/08e7034b-1f39-4ad3-8bb9-1e3f63fcb236" /></td>
   </tr>
</table>




## 📄 Licencia

Este proyecto fue desarrollado como parte de la asignatura Desarrollo para Dispositivos Inteligentes en la Universidad Tecnológica del Norte de Guanajuato, bajo la supervisión del profesor Anastacio Rodríguez García.
Uso académico exclusivamente.
