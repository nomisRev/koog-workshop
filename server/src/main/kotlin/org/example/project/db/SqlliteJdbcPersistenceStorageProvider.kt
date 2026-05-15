package org.example.project.db

import ai.koog.agents.features.persistence.jdbc.JdbcPersistenceStorageProvider
import ai.koog.agents.features.persistence.jdbc.PostgresJdbcPersistenceSchemaMigrator
import ai.koog.agents.features.sql.providers.SQLPersistenceSchemaMigrator
import org.jetbrains.exposed.v1.core.Table
import javax.sql.DataSource

class SqlliteJdbcPersistenceStorageProvider @JvmOverloads constructor(
    dataSource: DataSource,
    tableName: String = "agent_checkpoints",
    ttlSeconds: Long? = null,
    migrator: SQLPersistenceSchemaMigrator = PostgresJdbcPersistenceSchemaMigrator(dataSource, tableName),
) : JdbcPersistenceStorageProvider(dataSource, migrator, ttlSeconds, tableName) {
    override val upsertSql: String = """
        INSERT INTO $tableName (
            persistence_id,
            checkpoint_id,
            created_at,
            checkpoint_json,
            ttl_timestamp,
            version
        )
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(persistence_id, checkpoint_id) DO UPDATE SET
            created_at = excluded.created_at,
            checkpoint_json = excluded.checkpoint_json,
            ttl_timestamp = excluded.ttl_timestamp,
            version = excluded.version
    """.trimIndent()
}

object AgentCheckpoints : Table("agent_checkpoints") {
    val persistenceId = text("persistence_id")
    val checkpointId = text("checkpoint_id")
    val createdAt = long("created_at")
    val checkpointJson = text("checkpoint_json")
    val ttlTimestamp = long("ttl_timestamp").nullable()
    val version = long("version")

    override val primaryKey = PrimaryKey(persistenceId, checkpointId)
}
