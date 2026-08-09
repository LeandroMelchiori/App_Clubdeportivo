package com.example.clubdeportivo

object ListadoExportFilter {
    fun noSocios(items: List<DBHelper.NoSocioCard>, query: String): List<DBHelper.NoSocioCard> {
        val q = query.normalizedQuery()
        if (q.isEmpty()) return items
        return items.filter { it.nombre.matchesQuery(q) || it.apellido.matchesQuery(q) }
    }

    fun socios(items: List<DBHelper.SocioCard>, query: String): List<DBHelper.SocioCard> {
        val q = query.normalizedQuery()
        if (q.isEmpty()) return items
        return items.filter { it.nombre.matchesQuery(q) || it.apellido.matchesQuery(q) }
    }

    fun vencimientos(
        items: List<DBHelper.VencimientoCard>,
        query: String,
        tipo: VencimientoFilters.Tipo
    ): List<DBHelper.VencimientoCard> {
        val byStatus = VencimientoFilters.filtrar(items, tipo)
        val q = query.normalizedQuery()
        if (q.isEmpty()) return byStatus
        return byStatus.filter { it.nombre.matchesQuery(q) || it.apellido.matchesQuery(q) }
    }

    private fun String.normalizedQuery(): String = trim().lowercase()
    private fun String.matchesQuery(query: String): Boolean = lowercase().contains(query)
}
