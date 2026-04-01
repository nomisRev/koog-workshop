package org.example.project.db

import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File
import java.nio.file.Path

fun connectSqlite(url: String): Database {
    val dataSource = SQLiteDataSource(SQLiteConfig().apply {
        enforceForeignKeys(true)
    }).apply {
        this.url = url
    }

    return Database.connect(dataSource)
}

fun connectSqlite(path: Path): Database =
    connectSqlite("jdbc:sqlite:${path.toAbsolutePath().normalize().toUri().toASCIIString()}")

fun connectSqlite(file: File): Database =
    connectSqlite(file.toPath())
