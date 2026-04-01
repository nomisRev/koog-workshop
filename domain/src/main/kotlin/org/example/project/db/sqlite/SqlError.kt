package org.example.project.db.sqlite

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException

sealed interface SqlError

data class SqlColumnRef(val table: String, val column: String)

data class Conflict(
    val columns: List<SqlColumnRef>,
    val resultCode: SQLiteErrorCode,
    val rawMessage: String
) : SqlError

data class NotNullViolation(
    val column: SqlColumnRef,
    val resultCode: SQLiteErrorCode,
    val rawMessage: String
) : SqlError

data class CheckViolation(
    val detail: String,
    val resultCode: SQLiteErrorCode,
    val rawMessage: String
) : SqlError

data class ForeignKeyViolation(
    val resultCode: SQLiteErrorCode,
    val rawMessage: String
) : SqlError

fun Throwable.toSqlErrorOrNull(): SqlError? {
    val sqliteException = toSqliteExceptionOrNull() ?: return null
    val rawMessage = sqliteException.message.orEmpty()
    val detail = rawMessage.sqliteDetail()

    return when (sqliteException.resultCode) {
        SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE,
        SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY,
        SQLiteErrorCode.SQLITE_CONSTRAINT_ROWID -> Conflict(
            columns = detail.toConflictColumns(),
            resultCode = sqliteException.resultCode,
            rawMessage = rawMessage
        )

        SQLiteErrorCode.SQLITE_CONSTRAINT_NOTNULL -> detail.toNotNullColumnOrNull()?.let {
            NotNullViolation(
                column = it,
                resultCode = sqliteException.resultCode,
                rawMessage = rawMessage
            )
        }

        SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK -> detail.toCheckDetailOrNull()?.let {
            CheckViolation(
                detail = it,
                resultCode = sqliteException.resultCode,
                rawMessage = rawMessage
            )
        }

        SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY -> ForeignKeyViolation(
            resultCode = sqliteException.resultCode,
            rawMessage = rawMessage
        )

        else -> null
    }
}

private fun Throwable.toSqliteExceptionOrNull(): SQLiteException? =
    when (this) {
        is SQLiteException -> this
        is ExposedSQLException -> cause as? SQLiteException
        else -> generateSequence(cause) { it.cause }
            .filterIsInstance<SQLiteException>()
            .firstOrNull()
    }

private fun String.sqliteDetail(): String {
    if (startsWith("[SQLITE_") && endsWith(")")) {
        val detailStart = indexOf(" (")
        if (detailStart >= 0) {
            return substring(detailStart + 2, length - 1)
        }
    }

    return this
}

private fun String.toConflictColumns(): List<SqlColumnRef> {
    val columnList = when {
        startsWith("UNIQUE constraint failed: ") ->
            removePrefix("UNIQUE constraint failed: ")

        startsWith("PRIMARY KEY constraint failed: ") ->
            removePrefix("PRIMARY KEY constraint failed: ")

        startsWith("rowid is not unique: ") ->
            removePrefix("rowid is not unique: ")

        else -> return emptyList()
    }

    return columnList.split(',')
        .mapNotNull { entry -> entry.trim().toQualifiedColumnOrNull() }
}

private fun String.toCheckDetailOrNull(): String? =
    when {
        startsWith("CHECK constraint failed: ") ->
            removePrefix("CHECK constraint failed: ").trim()

        else -> null
    }?.takeIf { it.isNotEmpty() }

private fun String.toNotNullColumnOrNull(): SqlColumnRef? =
    when {
        startsWith("NOT NULL constraint failed: ") ->
            removePrefix("NOT NULL constraint failed: ").trim()

        else -> null
    }?.toQualifiedColumnOrNull()

private fun String.toQualifiedColumnOrNull(): SqlColumnRef? {
    val separatorIndex = lastIndexOf('.')
    if (separatorIndex <= 0 || separatorIndex == lastIndex) return null

    return SqlColumnRef(
        table = substring(0, separatorIndex),
        column = substring(separatorIndex + 1)
    )
}
