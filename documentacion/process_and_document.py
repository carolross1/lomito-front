#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de documentación exhaustiva para el proyecto Lomito Seguro.
Lee todos los archivos .kt, los comenta línea por línea y genera
los README de cada módulo con todo el código incluido.
"""

import os
import re

# Ruta base del proyecto
BASE_DIR = r"c:\Users\ANDY\Documents\UTNG\INGENIERIA\9no cuatrimestre\Desarrollo para Dispositivos Inteligentes\Unidad II\Proyecto Lomito\lomito-front"

# ===========================
# MAPA DE COMENTARIOS POR ARCHIVO
# Cada archivo tiene un diccionario con sus comentarios
# ===========================

def comment_kotlin_code(filename, code):
    """
    Añade comentarios exhaustivos a un archivo Kotlin dado su nombre y contenido.
    Devuelve el código con comentarios añadidos.
    """
    lines = code.split('\n')
    result = []
    
    for i, line in enumerate(lines):
        stripped = line.strip()
        
        # Línea vacía
        if not stripped:
            result.append(line)
            continue
        
        # Ya tiene comentario
        if stripped.startswith('//') or stripped.startswith('/*') or stripped.startswith('*') or stripped.startswith('/**'):
            result.append(line)
            continue
        
        # Generar comentario basado en el contenido de la línea
        comment = generate_comment(stripped, filename)
        
        if comment:
            # Obtener la indentación de la línea original
            indent = len(line) - len(line.lstrip())
            result.append(' ' * indent + '// ' + comment)
        
        result.append(line)
    
    return '\n'.join(result)


def generate_comment(line, filename):
    """
    Genera un comentario descriptivo basado en el contenido de la línea.
    """
    # Declaración de paquete
    if line.startswith('package '):
        pkg = line.replace('package ', '')
        return f'Paquete: {pkg}'
    
    # Importaciones
    if line.startswith('import '):
        imp = line.replace('import ', '')
        if 'Retrofit' in imp: return 'Importa el cliente Retrofit para peticiones HTTP'
        if 'ViewModel' in imp: return 'Importa la clase base ViewModel del ciclo de vida'
        if 'LiveData' in imp or 'StateFlow' in imp: return 'Importa el observable de datos reactivos'
        if 'Coroutine' in imp or 'coroutine' in imp: return 'Importa soporte para corrutinas de Kotlin'
        if 'Compose' in imp or 'compose' in imp: return 'Importa componente de Jetpack Compose'
        if 'Navigation' in imp or 'navigation' in imp: return 'Importa componente de navegación'
        if 'Fragment' in imp: return 'Importa la clase Fragment de AndroidX'
        if 'Bundle' in imp: return 'Importa el contenedor de datos Bundle'
        if 'Intent' in imp: return 'Importa la clase Intent para navegación entre componentes'
        if 'Context' in imp: return 'Importa el contexto de Android'
        if 'View' in imp: return 'Importa componentes de la interfaz gráfica'
        if 'Notification' in imp: return 'Importa las clases para manejo de notificaciones'
        if 'Wearable' in imp or 'wearable' in imp: return 'Importa la API de comunicación con Wear OS'
        if 'Glide' in imp or 'Coil' in imp: return 'Importa la librería de carga de imágenes'
        if 'Gson' in imp or 'json' in imp.lower(): return 'Importa el parser JSON'
        if 'OkHttp' in imp: return 'Importa el cliente HTTP OkHttp'
        if 'Room' in imp: return 'Importa componentes de la base de datos Room'
        if 'SharedPreferences' in imp: return 'Importa SharedPreferences para persistencia local'
        if 'Log' in imp: return 'Importa la clase de logging de Android'
        if 'ExoPlayer' in imp or 'Media3' in imp or 'media3' in imp: return 'Importa el reproductor multimedia ExoPlayer'
        if 'RecyclerView' in imp: return 'Importa el componente de lista reciclable'
        if 'Binding' in imp or 'binding' in imp: return 'Importa el sistema de View Binding'
        if 'Hilt' in imp or 'Dagger' in imp: return 'Importa el framework de inyección de dependencias'
        if 'Wear' in imp: return 'Importa clases específicas de Wear OS'
        if 'Maps' in imp or 'maps' in imp: return 'Importa las APIs de Google Maps'
        return f'Importa la dependencia necesaria: {imp.split(".")[-1]}'
    
    # Declaración de clase
    if re.match(r'(class|object|interface|enum class|data class|abstract class|open class|sealed class)\s+', line):
        name_match = re.match(r'(?:class|object|interface|enum class|data class|abstract class|open class|sealed class)\s+(\w+)', line)
        name = name_match.group(1) if name_match else 'desconocida'
        if 'ViewModel' in line: return f'ViewModel {name}: gestiona el estado y la lógica de negocio de la pantalla'
        if 'Fragment' in line: return f'Fragment {name}: componente de UI que representa una sección de la pantalla'
        if 'Activity' in line: return f'Activity {name}: pantalla principal que gestiona el ciclo de vida'
        if 'Repository' in line: return f'Repositorio {name}: capa de datos que abstrae las fuentes de información'
        if 'Adapter' in line: return f'Adaptador {name}: conecta los datos con la vista del RecyclerView'
        if 'Service' in line: return f'Servicio {name}: componente en background para tareas de larga duración'
        if 'data class' in line: return f'Clase de datos {name}: modelo inmutable con propiedades de dominio'
        if 'object' in line: return f'Singleton {name}: instancia única compartida en toda la aplicación'
        if 'interface' in line: return f'Interfaz {name}: contrato que deben cumplir las implementaciones'
        return f'Declaración de la clase {name}'
    
    # Funciones
    if re.match(r'(override\s+)?fun\s+\w+', line):
        fun_match = re.match(r'(?:override\s+)?(?:suspend\s+)?(?:private\s+)?(?:protected\s+)?(?:internal\s+)?fun\s+(\w+)', line)
        name = fun_match.group(1) if fun_match else 'desconocida'
        if name == 'onCreate': return 'Método del ciclo de vida: inicializa la actividad y configura la UI'
        if name == 'onStart': return 'Método del ciclo de vida: la actividad se vuelve visible'
        if name == 'onResume': return 'Método del ciclo de vida: la actividad está en primer plano e interactiva'
        if name == 'onPause': return 'Método del ciclo de vida: la actividad pierde el foco'
        if name == 'onStop': return 'Método del ciclo de vida: la actividad ya no es visible'
        if name == 'onDestroy': return 'Método del ciclo de vida: se limpia la actividad antes de destruirse'
        if name == 'onCreateViewHolder': return 'Infla el layout y crea el ViewHolder para el RecyclerView'
        if name == 'onBindViewHolder': return 'Vincula los datos del modelo con las vistas del ViewHolder'
        if name == 'getItemCount': return 'Retorna el número total de elementos en la lista'
        if name == 'onViewCreated': return 'Se llama cuando la vista del Fragment está lista; se inicializa la UI'
        if name == 'onCreateView': return 'Infla el layout del Fragment y retorna la vista raíz'
        if name == 'onMessageReceived': return 'Callback que se ejecuta cuando llega un mensaje desde el dispositivo Wear'
        if 'suspend' in line: return f'Función suspendida {name}: se ejecuta dentro de una corrutina para operaciones asíncronas'
        if 'override' in line: return f'Sobreescribe la función {name} de la clase padre'
        return f'Función {name}: define la lógica de esta operación'
    
    # Variables y propiedades
    if re.match(r'(private\s+|protected\s+|internal\s+)?(val|var)\s+', line):
        var_match = re.match(r'(?:private\s+|protected\s+|internal\s+)?(?:lateinit\s+)?(?:val|var)\s+(\w+)', line)
        name = var_match.group(1) if var_match else 'variable'
        if 'lateinit' in line: return f'Propiedad {name}: se inicializará más tarde antes de usarse (lateinit)'
        if 'val' in line.split(name)[0]: return f'Constante {name}: valor inmutable que no cambia tras su asignación'
        return f'Variable {name}: almacena el estado mutable de este componente'
    
    # Constantes companion object
    if 'const val' in line:
        const_match = re.search(r'const val\s+(\w+)', line)
        name = const_match.group(1) if const_match else 'constante'
        return f'Constante {name}: valor fijo definido en tiempo de compilación'
    
    # Coroutines
    if 'launch {' in line or 'launch{' in line:
        return 'Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono'
    if 'withContext(' in line:
        return 'Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)'
    if 'viewModelScope' in line:
        return 'Scope ligado al ciclo de vida del ViewModel; se cancela al destruirse'
    if 'lifecycleScope' in line:
        return 'Scope ligado al ciclo de vida del componente; se cancela automáticamente'
    
    # Jetpack Compose
    if '@Composable' in line:
        return 'Anotación que marca esta función como una función de composición de UI'
    if 'setContent {' in line or 'setContent{' in line:
        return 'Define el árbol de UI con Jetpack Compose como contenido de la Activity'
    
    # Log
    if 'Log.d(' in line or 'Log.e(' in line or 'Log.w(' in line:
        return 'Registro de evento en el log de Android para depuración'
    
    # Retrofit calls
    if '.enqueue(' in line:
        return 'Realiza la petición HTTP de forma asíncrona con callback'
    if 'RetrofitClient' in line:
        return 'Accede al cliente Retrofit singleton para realizar peticiones de red'
    
    # Navigation
    if 'navigate(' in line:
        return 'Navega hacia el destino especificado en el grafo de navegación'
    if 'navController' in line:
        return 'Controlador de navegación para moverse entre fragments'
    
    # RecyclerView
    if 'adapter =' in line or '.adapter =' in line:
        return 'Asigna el adaptador al RecyclerView para mostrar la lista de datos'
    if 'layoutManager' in line:
        return 'Define cómo se organizan visualmente los elementos del RecyclerView'
    
    # Binding
    if 'binding.' in line and ('inflate' not in line):
        if '=' in line:
            return 'Actualiza el componente de UI a través del View Binding'
        return 'Accede a un componente de UI a través del View Binding type-safe'
    if 'inflate(layoutInflater)' in line:
        return 'Infla el layout de la Activity usando View Binding'
    
    # Toast / Snackbar
    if 'Toast.makeText' in line:
        return 'Muestra un mensaje emergente breve al usuario'
    if 'Snackbar.make' in line:
        return 'Muestra un Snackbar con un mensaje de retroalimentación al usuario'
    
    # SharedPreferences
    if 'SharedPreferences' in line or 'getSharedPreferences' in line:
        return 'Accede al almacenamiento clave-valor persistente de la aplicación'
    if 'prefs.edit()' in line:
        return 'Inicia el editor para modificar los SharedPreferences'
    if '.apply()' in line:
        return 'Aplica los cambios de forma asíncrona en el hilo principal'
    
    # Wearable
    if 'Wearable.' in line:
        return 'Usa la API de Wearable para comunicación con dispositivos Wear OS'
    if 'sendMessage(' in line:
        return 'Envía un mensaje al dispositivo Wear OS conectado'
    if '.connectedNodes' in line:
        return 'Obtiene la lista de dispositivos Wear OS conectados'
    
    # Notificaciones
    if 'NotificationCompat.Builder' in line:
        return 'Construye la notificación con sus propiedades visuales y de comportamiento'
    if 'NotificationChannel(' in line:
        return 'Crea el canal de notificación requerido en Android 8.0+'
    if '.notify(' in line:
        return 'Muestra la notificación al usuario en la barra de estado'
    
    # ExoPlayer / Media
    if 'ExoPlayer' in line or 'SimpleExoPlayer' in line or 'MediaItem' in line:
        return 'Configura el reproductor multimedia ExoPlayer para streaming de video'
    if '.prepare()' in line:
        return 'Prepara el reproductor para iniciar la carga del media'
    if '.play()' in line:
        return 'Inicia la reproducción del contenido multimedia'
    if '.release()' in line:
        return 'Libera los recursos del reproductor multimedia'
    
    # Wear OS específico
    if 'WearableRecyclerView' in line:
        return 'Lista circular optimizada para la pantalla redonda del smartwatch'
    if 'AmbientMode' in line or 'AmbientCallback' in line:
        return 'Maneja el modo ambiente de bajo consumo del smartwatch'
    if 'VibratorCompat' in line or 'vibrate(' in line:
        return 'Activa la vibración háptica del smartwatch para retroalimentación táctil'
    
    # Condiciones y control de flujo
    if line.startswith('if ') or line.startswith('if('):
        return 'Condición: evalúa si se cumplen los requisitos para ejecutar el bloque'
    if line.startswith('when ') or line.startswith('when(') or line == 'when {':
        return 'Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)'
    if line.startswith('for (') or line.startswith('for('):
        return 'Itera sobre la colección para procesar cada elemento'
    if line.startswith('forEach {') or '.forEach {' in line:
        return 'Itera sobre cada elemento de la colección y ejecuta el bloque'
    if line.startswith('try {') or line == 'try {':
        return 'Bloque try-catch: maneja posibles excepciones en el código crítico'
    if line.startswith('catch (') or line.startswith('catch('):
        return 'Captura y maneja la excepción para evitar que la app se cierre inesperadamente'
    if line.startswith('return ') or line == 'return':
        return 'Retorna el valor al llamador de la función'
    
    # Llamadas al super
    if line.startswith('super.'):
        return 'Invoca la implementación del método en la clase padre'
    
    # Corchetes de cierre
    if line in ['}', '},', '})']:
        return None  # No comentar líneas de cierre
    
    return None  # No añadir comentario si no se reconoce el patrón


def read_file_utf8(filepath):
    """Lee un archivo con múltiples encodings posibles, limpiando BOM."""
    # Intentar leer el archivo con varios encodings comunes
    for encoding in ['utf-8-sig', 'utf-8', 'utf-16', 'utf-16-le', 'utf-16-be', 'latin-1', 'cp1252']:
        try:
            with open(filepath, 'r', encoding=encoding) as f:
                content = f.read()
            # Limpiar el BOM de UTF-16 si quedara al principio
            content = content.lstrip('\ufeff')
            # Si el contenido contiene caracteres raros de BOM (como ÿþ), descartar y probar siguiente
            if '\x00' in content[:50]:  # UTF-16 sin manejar correctamente da nulos
                continue
            return content
        except (UnicodeDecodeError, Exception):
            continue
    return ""


def write_file_utf8(filepath, content):
    """Escribe un archivo en UTF-8."""
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)


def get_module_files(module_name):
    """Obtiene todos los archivos .kt de un módulo."""
    src_path = os.path.join(BASE_DIR, module_name, "src", "main", "java")
    kt_files = []
    for root, dirs, files in os.walk(src_path):
        for file in sorted(files):
            if file.endswith(".kt"):
                kt_files.append(os.path.join(root, file))
    return sorted(kt_files)


def generate_readme(module_name, module_title, module_description, output_path):
    """Genera el README completo para un módulo."""
    kt_files = get_module_files(module_name)
    src_base = os.path.join(BASE_DIR, module_name, "src", "main", "java")
    
    lines = []
    lines.append(f"# Guía Paso a Paso: Construyendo el Módulo {module_title} de Lomito Seguro\n")
    lines.append(f"\nEsta guía documenta y desglosa paso a paso la arquitectura, configuración y construcción completa del módulo **{module_title}** de **Lomito Seguro**, explicando las decisiones técnicas, patrones de diseño y bloques de código esenciales para un proyecto profesional en **Kotlin** y **Jetpack Compose**.\n")
    lines.append("\n---\n")
    lines.append(f"\n{module_description}\n")
    lines.append("\n---\n")
    
    # Agrupar por paquetes/carpetas
    from collections import defaultdict
    groups = defaultdict(list)
    for kt in kt_files:
        rel = os.path.relpath(kt, src_base)
        parts = rel.replace('\\', '/').split('/')
        if len(parts) > 1:
            group = '/'.join(parts[:-1])
        else:
            group = '(raíz)'
        groups[group].append(kt)
    
    fase = 1
    for group_name in sorted(groups.keys()):
        lines.append(f"\n## FASE {fase}: `{group_name}`\n")
        
        paso = 1
        for kt_file in sorted(groups[group_name]):
            file_name = os.path.basename(kt_file)
            lines.append(f"\n### Paso {fase}.{paso}: `{file_name}`\n")
            
            # Breve descripción basada en el nombre
            desc = get_file_description(file_name, group_name)
            lines.append(f"\n{desc}\n")
            
            # Leer el código del archivo (ya fue comentado en el PASO 1)
            code = read_file_utf8(kt_file)
            if code and code.strip():
                # Usar el código tal cual (ya tiene comentarios del PASO 1)
                # NO llamar comment_kotlin_code de nuevo para evitar duplicados
                lines.append(f"\n```kotlin\n{code.rstrip()}\n```\n")
            else:
                lines.append("\n> ⚠️ No se pudo leer el archivo.\n")
            
            paso += 1
        
        fase += 1
    
    content = ''.join(lines)
    write_file_utf8(output_path, content)
    print(f"✅ README generado: {output_path} ({len(kt_files)} archivos)")


def get_file_description(filename, group_name):
    """Genera una descripción breve según el nombre del archivo."""
    f = filename.replace('.kt', '')
    
    descriptions = {
        'MainActivity': '**Actividad principal** de la aplicación móvil. Gestiona la navegación central mediante Navigation Component, configura el toolbar, el menú de logout y coordina el polling de notificaciones de avistamientos. También maneja la comunicación bidireccional con el smartwatch a través de la API de Wearable.',
        'WatchReportListener': '**Servicio de escucha Wearable** que se ejecuta en background. Recibe mensajes desde el smartwatch (alertas de proximidad, reportes de avistamiento y notificaciones de mascotas perdidas) y los procesa mostrando notificaciones locales en el teléfono.',
        'AppConfig': '**Configuración centralizada** de la aplicación. Contiene las constantes globales como la URL base del backend y los timeouts de red.',
        'Constants': '**Constantes globales** del módulo de datos. Define los valores fijos utilizados en toda la capa de datos como códigos de error, rutas de API y claves de configuración.',
        'LomitoApi': '**Interfaz de la API REST** definida con Retrofit. Declara todos los endpoints del backend de Lomito Seguro con sus métodos HTTP, rutas, parámetros y tipos de respuesta.',
        'RetrofitClient': '**Cliente Retrofit singleton** para la capa de red. Configura el cliente OkHttp con interceptores, establece la URL base del servidor y construye la instancia de Retrofit con el conversor Gson para serialización JSON.',
        'Alerta': '**Modelo de datos Alerta**. Clase de datos (data class) que representa una notificación de avistamiento o evento relacionado con una mascota.',
        'Models': '**Modelos de datos** del dominio. Define las data classes que representan las entidades principales: Mascota, Usuario, Reporte, Refugio, etc.',
        'AlertasRepository': '**Repositorio de alertas**. Capa de abstracción que centraliza el acceso a los datos de alertas, coordinando las peticiones al backend y el manejo de errores.',
        'LomitoRepository': '**Repositorio principal**. Capa de abstracción que centraliza el acceso a todos los datos de la aplicación, coordinando las peticiones al backend REST.',
        'NetworkInterceptor': '**Interceptor de red OkHttp**. Añade cabeceras de autenticación, maneja errores de conectividad y registra las peticiones/respuestas HTTP en el log.',
        'UserRepository': '**Repositorio de usuario**. Gestiona las operaciones de autenticación y perfil de usuario, coordinando entre el backend y el almacenamiento local.',
        'BaseActivity': '**Actividad base abstracta**. Clase padre de todas las Activities del módulo, proporciona funcionalidades comunes como manejo de loading, errores y navegación.',
        'AlertasAdapter': '**Adaptador del RecyclerView de alertas**. Conecta la lista de alertas con la vista, inflando los layouts de cada ítem y vinculando los datos.',
        'AlertasFragment': '**Fragment de alertas**. Muestra la lista de notificaciones de avistamientos recibidas. Observa el ViewModel y actualiza la UI de forma reactiva.',
        'AlertasViewModel': '**ViewModel de alertas**. Gestiona el estado de la pantalla de alertas, expone los datos con StateFlow/LiveData y coordina las llamadas al repositorio.',
        'AuthViewModel': '**ViewModel de autenticación**. Maneja la lógica de login y registro, valida las credenciales, realiza las peticiones al backend y gestiona el estado de la sesión.',
        'LoginFragment': '**Fragment de inicio de sesión**. Presenta el formulario de login, valida las entradas del usuario y delega la autenticación al AuthViewModel.',
        'RegisterFragment': '**Fragment de registro**. Muestra el formulario de creación de cuenta nueva, valida los datos y coordina el registro a través del AuthViewModel.',
        'HomeFragment': '**Fragment principal (Home)**. Pantalla principal tras el login. Muestra las mascotas registradas del usuario, accesos rápidos y el estado general de la app.',
        'HomeViewModel': '**ViewModel del Home**. Gestiona el estado de la pantalla principal: lista de mascotas, estadísticas y coordina las peticiones al repositorio.',
        'MascotaCardAdapter': '**Adaptador de tarjetas de mascotas**. Muestra cada mascota como una tarjeta en la cuadrícula del Home, con foto, nombre y estado.',
        'CrearMascotaFragment': '**Fragment de creación de mascota**. Formulario para registrar una nueva mascota con nombre, especie, foto y otros detalles.',
        'MascotaDetailFragment': '**Fragment de detalle de mascota**. Muestra la información completa de una mascota: fotos, historial de avistamientos, estado y acciones disponibles.',
        'MascotaViewModel': '**ViewModel de mascota**. Gestiona el estado CRUD de mascotas: creación, edición, eliminación y carga de datos desde el repositorio.',
        'MascotaPerdidaAdapter': '**Adaptador del mural de mascotas perdidas**. Muestra cada reporte de mascota perdida como una tarjeta con foto, descripción y botón para reportar avistamiento.',
        'MuralFragment': '**Fragment del mural comunitario**. Muestra todos los reportes de mascotas perdidas de la comunidad, permite ver detalles y reportar avistamientos.',
        'RefugiosFragment': '**Fragment de refugios**. Muestra el mapa con los refugios de animales cercanos usando Google Maps, con marcadores y información de cada refugio.',
        'SimulatorFragment': '**Fragment del simulador BLE**. Herramienta de desarrollo que simula la distancia entre el teléfono y el collar BLE de la mascota, enviando los datos al smartwatch.',
        'Extensions': '**Funciones de extensión**. Extiende clases existentes de Android/Kotlin con utilidades adicionales: conversión de URLs, visibilidad de vistas, formato de fechas y distancias.',
        'SessionManager': '**Gestor de sesión**. Almacena y recupera los datos del usuario autenticado en SharedPreferences: ID, nombre, email, teléfono y avatar.',
        'DateUtils': '**Utilidades de fecha**. Funciones helper para formatear y convertir fechas en la aplicación.',
        'ImageUtils': '**Utilidades de imagen**. Funciones helper para comprimir, redimensionar y procesar imágenes antes de subirlas al servidor.',
        'ValidationUtils': '**Utilidades de validación**. Funciones para validar datos de entrada: emails, teléfonos, contraseñas y otros campos de formulario.',
        # TV module
        'LomitoTvApp': '**Aplicación TV**. Clase Application del módulo TV; inicializa componentes globales como Retrofit y configura el contexto de la app.',
        'LomitoTvApi': '**Interfaz API para TV**. Define los endpoints REST del backend accesibles desde el módulo TV con Retrofit.',
        'DashboardActivity': '**Actividad del Dashboard**. Pantalla principal del módulo TV que muestra las mascotas en una cuadrícula optimizada para pantalla grande con control remoto.',
        'DashboardViewModel': '**ViewModel del Dashboard TV**. Gestiona el estado del dashboard: carga de mascotas, manejo de errores y actualización de datos.',
        'MapaView': '**Vista del mapa**. Componente personalizado para mostrar el mapa con la ubicación del refugio en la pantalla del TV.',
        'MascotaDetalleActivity': '**Actividad de detalle de mascota (TV)**. Muestra la información completa de una mascota en la pantalla grande del TV.',
        'MascotaDetalleViewModel': '**ViewModel de detalle de mascota (TV)**. Gestiona los datos y estado de la pantalla de detalle de mascota para TV.',
        'MascotaPerfilActivity': '**Actividad de perfil de mascota (TV)**. Muestra el perfil completo de una mascota con foto grande y datos detallados.',
        'MascotaPerfilViewModel': '**ViewModel de perfil de mascota (TV)**. Gestiona los datos del perfil de mascota para TV.',
        'RefugioDifusionActivity': '**Actividad de difusión del refugio (TV)**. Muestra la transmisión en vivo del refugio y la lista de mascotas disponibles para adopción.',
        'RefugioDifusionViewModel': '**ViewModel de difusión (TV)**. Gestiona el estado del video en vivo y los datos del refugio para la pantalla de difusión.',
        'Theme': '**Tema visual (TV)**. Define los colores, tipografías y estilos de la aplicación TV con Material Design para pantallas grandes.',
        # Wear module
        'MascotaPerdida': '**Modelo de mascota perdida (Wear)**. Data class que representa una mascota perdida en el contexto del smartwatch.',
        'PollingService': '**Servicio de polling (Wear)**. Servicio en background que consulta periódicamente el backend para obtener nuevas mascotas perdidas.',
        'WatchPreferences': '**Preferencias del reloj**. Gestiona el almacenamiento de configuraciones del usuario en el smartwatch con SharedPreferences.',
        'WatchViewModel': '**ViewModel del reloj**. Gestiona el estado central del smartwatch: mascotas, alertas y configuración.',
        'WearMessageService': '**Servicio de mensajes Wear**. Recibe mensajes del teléfono emparejado a través de la API de Wearable y actualiza el estado del reloj.',
        'AlertActivity': '**Actividad de alerta (Wear)**. Muestra una alerta de proximidad en la pantalla del smartwatch cuando la mascota se aleja demasiado.',
        'WearMainActivity': '**Actividad principal Wear**. Pantalla principal del smartwatch que muestra el menú de opciones con acceso rápido a las funciones principales.',
        'AddMascotaActivity': '**Actividad de agregar mascota (Wear)**. Formulario simplificado para registrar una nueva mascota desde el smartwatch.',
        'MarcarPerdidaActivity': '**Actividad marcar perdida (Wear)**. Permite marcar una mascota como perdida directamente desde el smartwatch con un toque.',
        'MascotaDetailActivity': '**Actividad de detalle de mascota (Wear)**. Muestra la información básica de una mascota en la pequeña pantalla del reloj.',
        'MascotaListActivity': '**Actividad de lista de mascotas (Wear)**. Muestra la lista de mascotas del usuario en una lista circular optimizada para Wear OS.',
        'AgregarMascotaPerdidaActivity': '**Actividad de agregar mascota perdida (Wear)**. Permite reportar una nueva mascota perdida desde el smartwatch.',
        'ReportActivity': '**Actividad de reportes (Wear)**. Menú de reportes disponibles en el smartwatch: avistamiento, mascota perdida, etc.',
        'ReportarAvistamientoActivity': '**Actividad de reportar avistamiento (Wear)**. Permite reportar que se ha visto una mascota perdida, enviando la ubicación actual.',
        'SelectionActivity': '**Actividad de selección (Wear)**. Pantalla de selección genérica usada para elegir entre opciones en el smartwatch.',
        'SettingsActivity': '**Actividad de configuración (Wear)**. Permite configurar el umbral de distancia de alerta y otras preferencias del smartwatch.',
    }
    
    return descriptions.get(f, f'**Archivo `{filename}`** del paquete `{group_name}`.')


def process_and_comment_file(kt_file):
    """Lee, comenta y sobreescribe un archivo .kt con comentarios exhaustivos."""
    code = read_file_utf8(kt_file)
    if not code:
        print(f"  ⚠️ No se pudo leer: {kt_file}")
        return
    
    filename = os.path.basename(kt_file)
    commented = comment_kotlin_code(filename, code)
    
    # Solo escribir si se añadieron comentarios (el contenido cambió)
    if commented != code:
        write_file_utf8(kt_file, commented)
        print(f"  ✍️ Comentado: {filename}")
    else:
        print(f"  ✅ Ya documentado: {filename}")


def main():
    import sys
    readme_only = '--readme-only' in sys.argv
    
    print("Iniciando proceso de documentacion de Lomito Seguro...")
    print("=" * 70)
    
    modules = ['mobile', 'tv', 'wear']
    
    # === PASO 1: COMENTAR TODOS LOS ARCHIVOS (saltear si --readme-only) ===
    if not readme_only:
        print("\nPASO 1: Anadiendo comentarios a todos los archivos .kt...\n")
        for module in modules:
            print(f"\n  Modulo: {module}")
            files = get_module_files(module)
            for kt_file in files:
                process_and_comment_file(kt_file)
    else:
        print("\nModo README-ONLY: saltando comentado de archivos .kt\n")
    
    # === PASO 2: GENERAR LOS README ===
    print("\n📚 PASO 2: Generando los README de documentación...\n")
    
    doc_dir = os.path.join(BASE_DIR, "documentacion")
    os.makedirs(doc_dir, exist_ok=True)
    
    # README MOBILE
    generate_readme(
        module_name='mobile',
        module_title='Móvil (Android Smartphone)',
        module_description="""
## Objetivo de Esta Guía

Al estudiar y seguir esta guía, comprenderás:

1. Cómo estructurar un proyecto Android profesional con **Kotlin** y **Jetpack Compose** bajo los principios de **Clean Architecture** y **MVVM**.
2. Cómo implementar comunicación bidireccional con un smartwatch **Wear OS** usando la **Wearable Data Layer API**.
3. Cómo conectar la aplicación con un backend **Spring Boot** usando **Retrofit** y **OkHttp**.
4. Cómo implementar un sistema de notificaciones en tiempo real con **polling** y **NotificationCompat**.
5. Cómo gestionar sesiones de usuario con **SharedPreferences** y manejar el flujo de autenticación.
6. Cómo integrar **Google Maps** para mostrar refugios y reportar avistamientos geolocalizados.
7. Cómo implementar la funcionalidad de fotos con subida al servidor y visualización con **Glide/Coil**.

## Arquitectura del Módulo Móvil

El módulo móvil sigue la arquitectura **MVVM (Model-View-ViewModel)** con separación de responsabilidades en capas:

```
app/
├── config/          → Configuración global (URLs, constantes)
├── data/
│   ├── api/         → Interfaces Retrofit + Cliente HTTP
│   ├── model/       → Data classes (entidades de dominio)
│   └── repository/  → Repositorios (acceso a datos)
├── network/         → Interceptores OkHttp
├── repository/      → Repositorios de usuario
├── ui/              → Fragments + ViewModels + Adapters
│   ├── auth/        → Login y Registro
│   ├── home/        → Pantalla principal y lista de mascotas
│   ├── mascota/     → CRUD de mascotas
│   ├── alertas/     → Notificaciones de avistamientos
│   ├── mural/       → Mural comunitario de mascotas perdidas
│   ├── refugios/    → Mapa de refugios
│   └── simulator/   → Simulador BLE de distancia
└── util/            → Funciones de extensión y utilidades
```
""",
        output_path=os.path.join(doc_dir, "README_MOBILE.md")
    )
    
    # README TV
    generate_readme(
        module_name='tv',
        module_title='TV (Android TV)',
        module_description="""
## Objetivo de Esta Guía

Al estudiar y seguir esta guía, comprenderás:

1. Cómo construir una app para **Android TV** optimizada para control remoto y pantalla grande.
2. Cómo implementar **Leanback** con navegación por 5-way (D-pad) y enfoque visual.
3. Cómo integrar **ExoPlayer / Media3** para reproducción de video en tiempo real (streaming HLS/RTSP).
4. Cómo conectar la TV con el backend **Spring Boot** usando **Retrofit**.
5. Cómo mostrar mapas estáticos y la ubicación del refugio en pantalla grande.
6. Cómo implementar el patrón **MVVM** adaptado para Android TV.

## Arquitectura del Módulo TV

```
tv/
├── LomitoTvApp.kt       → Application class
├── data/
│   ├── api/             → Interfaz Retrofit + Cliente HTTP
│   ├── model/           → Data classes del dominio TV
│   └── repository/      → Repositorio de datos TV
├── ui/
│   ├── dashboard/       → Pantalla principal con lista de mascotas
│   ├── detalle/         → Vista de detalle de mascota
│   ├── perfil/          → Perfil de mascota en pantalla grande
│   ├── refugio/         → Difusión en vivo del refugio
│   └── theme/           → Tema visual Material Design para TV
└── util/                → Utilidades y extensiones
```
""",
        output_path=os.path.join(doc_dir, "README_TV.md")
    )
    
    # README WEAR
    generate_readme(
        module_name='wear',
        module_title='Wear OS (Smartwatch)',
        module_description="""
## Objetivo de Esta Guía

Al estudiar y seguir esta guía, comprenderás:

1. Cómo construir una app para **Wear OS** con **Wear Compose** optimizada para pantalla circular pequeña.
2. Cómo implementar comunicación bidireccional con el teléfono usando **Wearable Data Layer API** (MessageClient y DataClient).
3. Cómo implementar **polling** de datos del backend desde el smartwatch.
4. Cómo gestionar el **modo ambiente** de bajo consumo en el reloj.
5. Cómo implementar alertas hápticas (vibración) cuando la mascota supera el umbral de distancia.
6. Cómo persistir configuraciones en el reloj con **SharedPreferences**.

## Arquitectura del Módulo Wear

```
wear/
├── data/
│   ├── MascotaPerdida.kt        → Modelo de datos para mascotas perdidas
│   ├── PollingService.kt        → Servicio de polling al backend
│   ├── WatchPreferences.kt      → Persistencia local en el reloj
│   ├── WatchViewModel.kt        → ViewModel central del reloj
│   └── WearMessageService.kt   → Servicio de mensajes desde el teléfono
└── ui/
    ├── alert/       → Pantalla de alerta de proximidad
    ├── dashboard/   → Dashboard principal del reloj
    ├── home/        → Pantalla de inicio (WearMainActivity)
    ├── mascota/     → CRUD básico de mascotas desde el reloj
    ├── report/      → Reportes de avistamiento y mascotas perdidas
    ├── selection/   → Pantalla de selección genérica
    └── settings/    → Configuración del reloj (umbral, preferencias)
```
""",
        output_path=os.path.join(doc_dir, "README_WEAR.md")
    )
    
    print("\n" + "=" * 70)
    print("🎉 ¡Documentación completada exitosamente!")
    print(f"\n📁 Los archivos README se encuentran en:")
    print(f"   {doc_dir}")
    print("\n📄 Archivos generados:")
    print("   - README_MOBILE.md")
    print("   - README_TV.md")
    print("   - README_WEAR.md")


if __name__ == '__main__':
    main()
