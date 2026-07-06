package dev.tohure.tanayenai.domain.repository

/**
 * Abstracción del nombre visible del usuario (persistido en preferencias).
 *
 * Permite que la capa de dominio lea el nombre elegido por el usuario sin depender
 * de la implementación concreta de preferencias (Dependency Inversion).
 */
fun interface DisplayNameProvider {
    /** Nombre elegido por el usuario, o null si aún no lo ha establecido. */
    fun getDisplayName(): String?
}
