package org.example.project.admin.data

import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.db.seedAdminDemoDataIfEmpty
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.time.Clock

private const val ADMIN_DATABASE_DIRECTORY = ".agent-fantasy-store"
private const val ADMIN_DATABASE_FILE = "agent-fantasy-store.db"

fun adminDatabasePath(): Path =
    Paths.get(System.getProperty("user.home"), ADMIN_DATABASE_DIRECTORY, ADMIN_DATABASE_FILE)

fun createAdminDatabase(
    path: Path = adminDatabasePath(),
    clock: Clock = Clock.System
): Database {
    val normalizedPath = path.toAbsolutePath().normalize()
    Files.createDirectories(normalizedPath.parent)

    return connectSqlite(normalizedPath)
        .createTables()
        .seedAdminDemoDataIfEmpty(clock)
}
