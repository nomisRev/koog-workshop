package org.example.project.admin

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.db.adminSchemaTables
import org.example.project.db.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path

private data class DatabaseSession(
    val database: Database,
    val path: Path
)

sealed interface DatabaseSessionOpenResult {
    data object Success : DatabaseSessionOpenResult

    sealed interface Failure : DatabaseSessionOpenResult {
        val message: String

        data class UnreadablePath(
            val path: Path,
            override val message: String
        ) : Failure

        data class InvalidSchema(
            val missingTables: List<String>,
            override val message: String
        ) : Failure

        data class InvalidDatabase(
            val path: Path,
            override val message: String,
            val cause: Throwable? = null
        ) : Failure
    }
}

class DatabaseSessionService {

    @Volatile
    private var currentSession: DatabaseSession? = null

    fun databaseOrNull(): Database? = currentSession?.database

    fun databasePathOrNull(): Path? = currentSession?.path

    fun closeSession() {
        currentSession = null
    }

    suspend fun openDatabase(path: Path): DatabaseSessionOpenResult = withContext(Dispatchers.IO) {
        val normalizedPath = path.toAbsolutePath().normalize()

        if (!Files.isRegularFile(normalizedPath) || !Files.isReadable(normalizedPath)) {
            return@withContext DatabaseSessionOpenResult.Failure.UnreadablePath(
                path = normalizedPath,
                message = "The selected file is not readable: $normalizedPath"
            )
        }

        val dataSource = SQLiteDataSource(SQLiteConfig().apply {
            setReadOnly(true)
        }).apply {
            url = "jdbc:sqlite:${normalizedPath.toUri().toASCIIString()}"
        }

        val candidateDatabase = Database.connect(dataSource)

        try {
            val missingTables = candidateDatabase.suspendTransaction(readOnly = true) {
                val actualTables = SchemaUtils.listTables().map { it.lowercase() }.toSet()
                adminSchemaTables()
                    .map { it.tableName }
                    .filterNot { tableName -> tableName.lowercase() in actualTables }
            }

            if (missingTables.isNotEmpty()) {
                return@withContext DatabaseSessionOpenResult.Failure.InvalidSchema(
                    missingTables = missingTables,
                    message = "Missing expected tables: ${missingTables.joinToString(", ")}"
                )
            }

            currentSession = DatabaseSession(candidateDatabase, normalizedPath)
            DatabaseSessionOpenResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            DatabaseSessionOpenResult.Failure.InvalidDatabase(
                path = normalizedPath,
                message = throwable.message ?: "Unable to open SQLite database.",
                cause = throwable
            )
        }
    }
}
