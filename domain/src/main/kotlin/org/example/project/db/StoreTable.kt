package org.example.project.db

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.update as exposedUpdate
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
abstract class StoreTable(name: String) : UuidTable(name) {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

context(_: Transaction)
fun <T : StoreTable> T.update(
    where: (() -> Op<Boolean>)? = null,
    limit: Int? = null,
    body: T.(UpdateStatement) -> Unit
): Int {
    val whereClause = where ?: { Op.TRUE }
    return exposedUpdate(whereClause, limit) { statement ->
        body(statement)
        statement[updatedAt] = CurrentTimestamp
    }
}
