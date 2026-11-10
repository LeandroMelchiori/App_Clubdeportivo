package com.example.clubdeportivo.backend.db

import java.util.UUID
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Abre las transacciones de la API en uno de dos contextos.
 *
 * - [inClub]: datos de un club. La transacción pasa al rol `club_app`, que cumple las reglas por fila,
 *   y fija el club activo. Los repositorios de datos de club toman el club de [currentClubId], así que
 *   fuera de este contexto fallan en lugar de consultar sin filtro.
 * - [identity]: operaciones que no pertenecen a un solo club (alta de club, login, renovación de
 *   sesión). Corren con el usuario dueño de las tablas.
 *
 * Ver `V3__aislar_clubes_con_rls.sql`.
 */
@Component
class DatabaseContext(transactionManager: PlatformTransactionManager, private val jdbc: JdbcClient) {

    private val transactions = TransactionTemplate(transactionManager)
    private val activeClub = ThreadLocal<UUID>()

    fun <T> inClub(clubId: UUID, block: () -> T): T = inNewTransaction {
        // SET LOCAL y set_config(..., true) duran hasta el final de esta transacción: la conexión vuelve
        // al pool sin rol ni club.
        jdbc.sql("SET LOCAL ROLE club_app").update()
        jdbc.sql("SELECT set_config('app.club_id', :club, true)")
            .param("club", clubId.toString())
            .query(String::class.java)
            .single()
        activeClub.set(clubId)
        try {
            block()
        } finally {
            activeClub.remove()
        }
    }

    fun <T> identity(block: () -> T): T = inNewTransaction(block)

    /** El club de la transacción en curso. Falla si no se está dentro de [inClub]. */
    fun currentClubId(): UUID =
        checkNotNull(activeClub.get()) { "Consulta de datos de un club fuera de DatabaseContext.inClub" }

    private fun <T> inNewTransaction(block: () -> T): T {
        // Unirse a una transacción abierta mezclaría contextos: el rol y el club quedarían fijados
        // también para lo que ya se hizo en ella.
        check(!TransactionSynchronizationManager.isActualTransactionActive()) {
            "Los contextos de DatabaseContext no se anidan"
        }
        @Suppress("UNCHECKED_CAST")
        return transactions.execute { block() } as T
    }
}
