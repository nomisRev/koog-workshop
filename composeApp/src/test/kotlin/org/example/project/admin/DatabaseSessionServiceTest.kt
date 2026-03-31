package org.example.project.admin

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.db.tables.Currencies
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseSessionServiceTest {

    @Test
    fun `openDatabase succeeds for a valid schema`() = runBlocking {
        val databaseFile = java.io.File.createTempFile("admin_session_valid_", ".db").apply {
            deleteOnExit()
        }
        Database.connect("jdbc:sqlite:${databaseFile.absolutePath}").createTables()

        val service = DatabaseSessionService()
        val result = service.openDatabase(databaseFile.toPath())

        assertEquals(DatabaseSessionOpenResult.Success, result)
        assertNotNull(service.databaseOrNull())
        assertEquals(databaseFile.toPath().toAbsolutePath().normalize(), service.databasePathOrNull())

        service.closeSession()
        assertNull(service.databaseOrNull())
        assertNull(service.databasePathOrNull())
    }

    @Test
    fun `openDatabase reports invalid schema when tables are missing`() = runBlocking {
        val databaseFile = java.io.File.createTempFile("admin_session_invalid_schema_", ".db").apply {
            deleteOnExit()
        }
        val database = Database.connect("jdbc:sqlite:${databaseFile.absolutePath}")
        transaction(database) {
            SchemaUtils.create(Currencies)
        }

        val service = DatabaseSessionService()
        val result = service.openDatabase(databaseFile.toPath())

        assertIs<DatabaseSessionOpenResult.Failure.InvalidSchema>(result)
        assertTrue(result.missingTables.isNotEmpty())
        assertTrue(result.message.contains("Missing expected tables"))
        assertNull(service.databaseOrNull())
    }

    @Test
    fun `openDatabase reports unreadable path for a missing file`() = runBlocking {
        val missingFile = createTempFile(prefix = "admin_session_missing_", suffix = ".db")
        missingFile.deleteIfExists()

        val service = DatabaseSessionService()
        val result = service.openDatabase(missingFile)

        assertIs<DatabaseSessionOpenResult.Failure.UnreadablePath>(result)
        assertEquals(missingFile.toAbsolutePath().normalize(), result.path)
        assertNull(service.databaseOrNull())
    }
}
