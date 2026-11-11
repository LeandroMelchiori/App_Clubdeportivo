package com.example.clubdeportivo.backend.club

/** Rol de una persona dentro de un club. Una misma cuenta puede tener un rol distinto en cada club. */
enum class Role {
    /** Administración: gestiona el club y su personal. */
    ADMIN,

    /** Recepción. */
    RECEPTION,

    /** Caja. */
    CASHIER,

    /** Solo lectura. */
    READ_ONLY,
}
