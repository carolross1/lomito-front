// Paquete: com.lomito.seguro.ui.auth
package com.lomito.seguro.ui.auth

// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Usuario
import com.lomito.seguro.data.model.Usuario
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Declaración de la clase AuthState
sealed class AuthState {
    // Singleton Loading: instancia única compartida en toda la aplicación
    object Loading : AuthState()
    // Clase de datos Success: modelo inmutable con propiedades de dominio
    data class Success(val usuario: Usuario) : AuthState()
    // Clase de datos Error: modelo inmutable con propiedades de dominio
    data class Error(val message: String) : AuthState()
}

// ViewModel AuthViewModel: gestiona el estado y la lógica de negocio de la pantalla
class AuthViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()
    // Constante _authState: valor inmutable que no cambia tras su asignación
    private val _authState = MutableLiveData<AuthState>()
    // Constante authState: valor inmutable que no cambia tras su asignación
    val authState: LiveData<AuthState> = _authState

    // Función login: define la lógica de esta operación
    fun login(correo: String, contrasena: String) {
        _authState.value = AuthState.Loading
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = repo.login(correo, contrasena)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (response.isSuccessful && response.body() != null) {
                    _authState.value = AuthState.Success(response.body()!!)
                } else {
                    _authState.value = AuthState.Error("Credenciales inválidas")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    // Función register: define la lógica de esta operación
    fun register(nombre: String, correo: String, telefono: String, contrasena: String) {
        _authState.value = AuthState.Loading
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = repo.register(nombre, correo, telefono, contrasena)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (response.isSuccessful && response.body() != null) {
                    _authState.value = AuthState.Success(response.body()!!)
                } else {
                    _authState.value = AuthState.Error("Error al registrarse")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error de conexión: ${e.message}")
            }
        }
    }
}
