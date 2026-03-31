package org.example.project.admin

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.db.tables.Currencies
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminAppViewModelTest {

    @Test
    fun `openDatabase connects and switches to the dashboard`() = runBlocking {
        val databaseFile = createValidDatabaseFile("admin_app_valid_")
        val service = DatabaseSessionService()
        val viewModel = AdminAppViewModel(service)

        viewModel.openDatabase(databaseFile.toPath())

        val state = viewModel.uiState.value
        val expectedPath = databaseFile.toPath().toAbsolutePath().normalize().toString()

        assertEquals(AdminDestination.Dashboard, state.currentDestination)
        assertIs<DatabaseConnectionState.Connected>(state.databaseConnectionState)
        assertEquals(expectedPath, state.selectedDatabasePath)
        assertIs<SchemaValidationState.Valid>(state.schemaValidationState)
        assertNotNull(service.databaseOrNull())
        assertEquals(databaseFile.toPath().toAbsolutePath().normalize(), service.databasePathOrNull())
    }

    @Test
    fun `openDatabase keeps the active session when validation fails and closeDatabase resets`() = runBlocking {
        val validDatabaseFile = createValidDatabaseFile("admin_app_connected_")
        val invalidSchemaFile = createPartialDatabaseFile("admin_app_invalid_schema_")
        val service = DatabaseSessionService()
        val viewModel = AdminAppViewModel(service)

        viewModel.openDatabase(validDatabaseFile.toPath())
        viewModel.openDatabase(invalidSchemaFile.toPath())

        val invalidState = viewModel.uiState.value
        val expectedPath = validDatabaseFile.toPath().toAbsolutePath().normalize().toString()

        assertEquals(AdminDestination.Dashboard, invalidState.currentDestination)
        assertIs<DatabaseConnectionState.Connected>(invalidState.databaseConnectionState)
        assertEquals(expectedPath, invalidState.selectedDatabasePath)
        assertIs<SchemaValidationState.Invalid>(invalidState.schemaValidationState)
        assertTrue(invalidState.schemaValidationState.message.contains("Missing expected tables"))
        assertEquals(validDatabaseFile.toPath().toAbsolutePath().normalize(), service.databasePathOrNull())

        viewModel.closeDatabase()

        val closedState = viewModel.uiState.value
        assertEquals(AdminDestination.Startup, closedState.currentDestination)
        assertIs<DatabaseConnectionState.Disconnected>(closedState.databaseConnectionState)
        assertNull(closedState.selectedDatabasePath)
        assertIs<SchemaValidationState.Idle>(closedState.schemaValidationState)
        assertNull(service.databaseOrNull())
        assertNull(service.databasePathOrNull())
    }

    private fun createValidDatabaseFile(prefix: String): java.io.File {
        val databaseFile = java.io.File.createTempFile(prefix, ".db").apply {
            deleteOnExit()
        }
        Database.connect("jdbc:sqlite:${databaseFile.absolutePath}").createTables()
        return databaseFile
    }

    private fun createPartialDatabaseFile(prefix: String): java.io.File {
        val databaseFile = java.io.File.createTempFile(prefix, ".db").apply {
            deleteOnExit()
        }
        val database = Database.connect("jdbc:sqlite:${databaseFile.absolutePath}")
        transaction(database) {
            SchemaUtils.create(Currencies)
        }
        return databaseFile
    }
}
