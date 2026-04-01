package org.example.project.db.sqlite

import org.example.project.db.connectSqlite
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object UniqueRows : Table("unique_rows") {
    val name = varchar("name", 255).uniqueIndex()
}

private object CompositeUniqueRows : Table("composite_unique_rows") {
    val leftId = integer("left_id")
    val rightId = integer("right_id")

    init {
        uniqueIndex(leftId, rightId)
    }
}

private object CompositePrimaryKeyRows : Table("composite_primary_key_rows") {
    val merchantId = integer("merchant_id")
    val shippingMethodId = integer("shipping_method_id")

    override val primaryKey = PrimaryKey(merchantId, shippingMethodId)
}

private object NamedCheckRows : Table("named_check_rows") {
    val quantity = integer("quantity").check("quantity_positive") { it greaterEq 1 }
}

private object NotNullRows : Table("not_null_rows") {
    val requiredName = varchar("required_name", 255)
}

private object ParentRows : Table("parent_rows") {
    val id = integer("id")

    override val primaryKey = PrimaryKey(id)
}

private object ChildRows : Table("child_rows") {
    val id = integer("id")
    val parentId = reference("parent_id", ParentRows.id)

    override val primaryKey = PrimaryKey(id)
}

class SqlErrorIntegrationTest {

    @Test
    fun singleColumnUniqueConflict_extractsConflict() {
        val database = temporaryDatabase()
        transaction(database) {
            SchemaUtils.create(UniqueRows)
            UniqueRows.insert { it[name] = "Thorin" }
        }

        val exception = assertSqliteFailure(database) {
            UniqueRows.insert { it[name] = "Thorin" }
        }
        val sqliteException = assertIs<SQLiteException>(exception.cause)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE, sqliteException.resultCode)
        assertContains(sqliteException.message.orEmpty(), "UNIQUE constraint failed: unique_rows.name")

        val error = assertIs<Conflict>(exception.toSqlErrorOrNull())
        assertEquals(listOf(SqlColumnRef("unique_rows", "name")), error.columns)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE, error.resultCode)
        assertEquals(sqliteException.message, error.rawMessage)
    }

    @Test
    fun compositeUniqueConflict_extractsConflict() {
        val database = temporaryDatabase()
        transaction(database) {
            SchemaUtils.create(CompositeUniqueRows)
            CompositeUniqueRows.insert {
                it[leftId] = 1
                it[rightId] = 2
            }
        }

        val exception = assertSqliteFailure(database) {
            CompositeUniqueRows.insert {
                it[leftId] = 1
                it[rightId] = 2
            }
        }
        val sqliteException = assertIs<SQLiteException>(exception.cause)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE, sqliteException.resultCode)
        assertContains(
            sqliteException.message.orEmpty(),
            "UNIQUE constraint failed: composite_unique_rows.left_id, composite_unique_rows.right_id"
        )

        val error = assertIs<Conflict>(exception.toSqlErrorOrNull())
        assertEquals(
            listOf(
                SqlColumnRef("composite_unique_rows", "left_id"),
                SqlColumnRef("composite_unique_rows", "right_id")
            ),
            error.columns
        )
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE, error.resultCode)
        assertEquals(sqliteException.message, error.rawMessage)
    }

    @Test
    fun compositePrimaryKeyConflict_extractsConflict() {
        val database = temporaryDatabase()
        transaction(database) {
            SchemaUtils.create(CompositePrimaryKeyRows)
            CompositePrimaryKeyRows.insert {
                it[merchantId] = 1
                it[shippingMethodId] = 2
            }
        }

        val exception = assertSqliteFailure(database) {
            CompositePrimaryKeyRows.insert {
                it[merchantId] = 1
                it[shippingMethodId] = 2
            }
        }
        val sqliteException = assertIs<SQLiteException>(exception.cause)
        assertContains(
            sqliteException.message.orEmpty(),
            "composite_primary_key_rows.merchant_id, composite_primary_key_rows.shipping_method_id"
        )
        assertTrue(
            sqliteException.resultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY ||
                sqliteException.resultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
        )

        val error = assertIs<Conflict>(exception.toSqlErrorOrNull())
        assertEquals(
            listOf(
                SqlColumnRef("composite_primary_key_rows", "merchant_id"),
                SqlColumnRef("composite_primary_key_rows", "shipping_method_id")
            ),
            error.columns
        )
        assertEquals(sqliteException.resultCode, error.resultCode)
        assertEquals(sqliteException.message, error.rawMessage)
    }

    @Test
    fun namedCheckViolation_extractsCheckViolation() {
        val database = temporaryDatabase()
        transaction(database) {
            SchemaUtils.create(NamedCheckRows)
        }

        val exception = assertSqliteFailure(database) {
            NamedCheckRows.insert { it[quantity] = 0 }
        }
        val sqliteException = assertIs<SQLiteException>(exception.cause)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK, sqliteException.resultCode)
        assertContains(sqliteException.message.orEmpty(), "CHECK constraint failed: quantity_positive")

        val error = assertIs<CheckViolation>(exception.toSqlErrorOrNull())
        assertEquals("quantity_positive", error.detail)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK, error.resultCode)
        assertEquals(sqliteException.message, error.rawMessage)
    }

    @Test
    fun unnamedCheckViolation_extractsCheckViolation() {
        val database = temporaryDatabase()
        transaction(database) {
            exec(
                """
                CREATE TABLE unnamed_check_rows (
                    rating INTEGER CHECK (rating BETWEEN 1 AND 5)
                )
                """.trimIndent()
            )
        }

        val exception = assertSqliteFailure(database) {
            exec("INSERT INTO unnamed_check_rows(rating) VALUES (0)")
        }
        val sqliteException = assertIs<SQLiteException>(exception.cause)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK, sqliteException.resultCode)
        assertContains(sqliteException.message.orEmpty(), "CHECK constraint failed: rating BETWEEN 1 AND 5")

        val error = assertIs<CheckViolation>(exception.toSqlErrorOrNull())
        assertEquals("rating BETWEEN 1 AND 5", error.detail)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK, error.resultCode)
        assertEquals(sqliteException.message, error.rawMessage)
    }

    @Test
    fun notNullViolation_extractsNotNullViolation() {
        val database = temporaryDatabase()
        transaction(database) {
            SchemaUtils.create(NotNullRows)
        }

        val exception = assertSqliteFailure(database) {
            exec("INSERT INTO not_null_rows(required_name) VALUES (NULL)")
        }
        val sqliteException = assertIs<SQLiteException>(exception.cause)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_NOTNULL, sqliteException.resultCode)
        assertContains(sqliteException.message.orEmpty(), "NOT NULL constraint failed: not_null_rows.required_name")

        val error = assertIs<NotNullViolation>(exception.toSqlErrorOrNull())
        assertEquals(SqlColumnRef("not_null_rows", "required_name"), error.column)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_NOTNULL, error.resultCode)
        assertEquals(sqliteException.message, error.rawMessage)
    }

    @Test
    fun foreignKeyViolation_extractsForeignKeyViolation() {
        val database = temporaryDatabase()
        transaction(database) {
            SchemaUtils.create(ParentRows, ChildRows)
        }

        val exception = assertSqliteFailure(database) {
            ChildRows.insert {
                it[id] = 1
                it[parentId] = 99
            }
        }
        val sqliteException = assertIs<SQLiteException>(exception.cause)
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY, sqliteException.resultCode)
        assertContains(sqliteException.message.orEmpty(), "FOREIGN KEY constraint failed")

        val error = assertIs<ForeignKeyViolation>(exception.toSqlErrorOrNull())
        assertEquals(SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY, error.resultCode)
        assertEquals(sqliteException.message, error.rawMessage)
    }

    @Test
    fun unsupportedException_returnsNull() {
        assertNull(IllegalArgumentException("not sqlite").toSqlErrorOrNull())
    }

    private fun temporaryDatabase(): Database =
        connectSqlite(File.createTempFile("sql_error_", ".db").apply { deleteOnExit() })

    private fun assertSqliteFailure(
        database: Database,
        block: JdbcTransaction.() -> Unit
    ): ExposedSQLException =
        assertFailsWith<ExposedSQLException> {
            transaction(database) {
                block()
            }
        }
}
