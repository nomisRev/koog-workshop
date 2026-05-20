package org.example.project.db

import ai.koog.agents.features.chathistory.jdbc.JdbcChatHistoryProvider
import ai.koog.agents.features.chatmemory.sql.SQLChatHistorySchemaMigrator
import kotlinx.serialization.json.Json
import javax.sql.DataSource

class SqliteJdbcChatHistoryProvider @JvmOverloads constructor(
    dataSource: DataSource,
    tableName: String = "chat_history",
    ttlSeconds: Long? = null,
    migrator: SQLChatHistorySchemaMigrator = SqliteJdbcChatHistorySchemaMigrator(dataSource, tableName),
    json: Json = Json { ignoreUnknownKeys = true }
) : JdbcChatHistoryProvider(dataSource, migrator, ttlSeconds, tableName) {
    override val upsertSql: String = """
        INSERT INTO $tableName (conversation_id, messages_json, updated_at, ttl_timestamp)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(conversation_id) DO UPDATE SET
            messages_json = excluded.messages_json,
            updated_at = excluded.updated_at,
            ttl_timestamp = excluded.ttl_timestamp
    """.trimIndent()
}

public class SqliteJdbcChatHistorySchemaMigrator @JvmOverloads constructor(
    private val dataSource: DataSource,
    private val tableName: String = "chat_history"
) : SQLChatHistorySchemaMigrator {
    override suspend fun migrate() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS $tableName (
                        conversation_id VARCHAR(255) NOT NULL,
                        messages_json TEXT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        ttl_timestamp BIGINT NULL,
                        PRIMARY KEY (conversation_id)
                    )
                    """.trimIndent()
                )
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_${tableName}_updated_at ON $tableName (updated_at)")
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_${tableName}_ttl_timestamp ON $tableName (ttl_timestamp)")
            }
        }
    }
}
