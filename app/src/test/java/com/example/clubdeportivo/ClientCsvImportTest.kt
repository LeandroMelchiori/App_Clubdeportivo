package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientCsvImportTest {
    private val today = LocalDate.parse("2026-11-04")

    @Test
    fun preview_normalizesValidRowsWithLocalFormats() {
        val preview = preview(
            "DNI;Nombre;Apellido;Fecha de nacimiento;Teléfono;Domicilio;Correo electrónico\n" +
                "30.111.222;Ana;Pérez;5/4/1990;(341) 555-1111;Calle 1;Ana@Club.com\n"
        )

        assertNull(preview.fileError)
        assertEquals(
            ClientCsvImport.Client(
                dni = "30111222",
                name = "Ana",
                lastName = "Pérez",
                birthDate = "1990-04-05",
                phone = "3415551111",
                address = "Calle 1",
                email = "ana@club.com"
            ),
            preview.validClients.single()
        )
        assertEquals("Pérez, Ana", preview.rows.single().displayName)
    }

    @Test
    fun preview_acceptsIsoDatesAndReportsIgnoredColumns() {
        val preview = preview(
            "$HEADER,observaciones\n30111222,Ana,Perez,1990-04-05,3415551111,Calle 1,ana@club.com,VIP\n"
        )

        assertEquals("1990-04-05", preview.validClients.single().birthDate)
        assertEquals(listOf("observaciones"), preview.ignoredColumns)
    }

    @Test
    fun preview_collectsEveryErrorOfARow() {
        val preview = preview("$HEADER\n12,,Perez,31/02/1990,12,,mail-invalido\n")
        val row = preview.rows.single()

        assertFalse(row.isValid)
        assertEquals(
            listOf(
                "DNI inválido: debe tener 8 o 9 dígitos",
                "Falta el nombre",
                "Fecha de nacimiento inválida: usa DD/MM/AAAA",
                "Teléfono inválido: debe tener entre 9 y 12 dígitos",
                "Falta el domicilio",
                "Email inválido"
            ),
            row.errors
        )
    }

    @Test
    fun preview_rejectsFutureBirthDatesAndShiftedColumns() {
        val preview = preview(
            "$HEADER\n" +
                "30111222,Ana,Perez,05/11/2026,3415551111,Calle 1,ana@club.com\n" +
                "30111223,Juan,Gomez,01/01/1990,3415551112,Calle 2, Piso 3,juan@club.com\n"
        )

        assertEquals(listOf("La fecha de nacimiento no puede ser futura"), preview.rows[0].errors)
        assertTrue(preview.rows[1].errors.first().startsWith("La fila tiene más columnas"))
        assertEquals(0, preview.validClients.size)
    }

    @Test
    fun preview_rejectsExistingAndRepeatedIdentities() {
        val preview = preview(
            "$HEADER\n" +
                "30111222,Ana,Perez,01/01/1990,3415551111,Calle 1,ana@club.com\n" +
                "40111111,Juan,Gomez,01/01/1990,3415551112,Calle 2,juan@club.com\n" +
                "30111222,Eva,Diaz,01/01/1990,3415551113,Calle 3,ANA@club.com\n" +
                "30111224,Leo,Ruiz,01/01/1990,3415551114,Calle 4,existente@club.com\n",
            existingDnis = setOf("40111111"),
            existingEmails = setOf("Existente@Club.com")
        )

        assertTrue(preview.rows[0].isValid)
        assertEquals(listOf("El DNI ya está registrado"), preview.rows[1].errors)
        assertEquals(
            listOf("DNI repetido en la fila 2", "Email repetido en la fila 2"),
            preview.rows[2].errors
        )
        assertEquals(listOf("El email ya está registrado"), preview.rows[3].errors)
        assertEquals(3, preview.invalidCount)
    }

    @Test
    fun preview_reportsMissingColumnsAndEmptyFiles() {
        assertEquals(
            "Faltan columnas obligatorias: Fecha de nacimiento, Email",
            preview("dni,nombre,apellido,telefono,direccion\n30111222,Ana,Perez,3415551111,Calle 1\n").fileError
        )
        assertEquals("El archivo no tiene filas de clientes", preview("$HEADER\n").fileError)
        assertEquals("El archivo no tiene filas de clientes", preview("").fileError)
    }

    @Test
    fun preview_limitsRowsPerFile() {
        val rows = (1..ClientCsvImport.MAX_ROWS + 1).joinToString("\n") { "x" }

        assertEquals("El archivo supera el máximo de 2000 filas", preview("$HEADER\n$rows").fileError)
    }

    @Test
    fun template_containsOnlyTheExpectedHeader() {
        assertEquals("$HEADER\n", ClientCsvImport.template())
        assertTrue(preview(ClientCsvImport.template()).fileError != null)
    }

    @Test
    fun text_describesSummaryActionsAndConfirmation() {
        assertEquals(
            "1 fila lista para importar · 2 filas con errores",
            ClientCsvImportText.summary(1, 2)
        )
        assertEquals("3 filas listas para importar · sin errores", ClientCsvImportText.summary(3, 0))
        assertEquals("Importar 1 cliente", ClientCsvImportText.importButton(1))
        assertEquals("No hay clientes para importar", ClientCsvImportText.importButton(0))
        assertTrue(ClientCsvImportText.confirmMessage(2, 1).contains("2 clientes nuevos como no socios"))
        assertTrue(ClientCsvImportText.confirmMessage(2, 1).contains("La fila con errores no se importa."))
        assertEquals("Se importó 1 cliente", ClientCsvImportText.imported(1))
        assertEquals("DNI 30111222 · ana@club.com", ClientCsvImportText.rowDetail("30111222", "ana@club.com"))
        assertEquals("Sin DNI ni email", ClientCsvImportText.rowDetail("", ""))
        assertEquals("• Falta el DNI\n• Email inválido", ClientCsvImportText.rowErrors(listOf("Falta el DNI", "Email inválido")))
    }

    private fun preview(
        text: String,
        existingDnis: Set<String> = emptySet(),
        existingEmails: Set<String> = emptySet()
    ) = ClientCsvImport.preview(text, existingDnis, existingEmails, today)

    private companion object {
        const val HEADER = "dni,nombre,apellido,fecha_nacimiento,telefono,direccion,email"
    }
}
