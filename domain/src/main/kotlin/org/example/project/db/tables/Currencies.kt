package org.example.project.db.tables

object Currencies : StoreTable("currencies") {
    val code = varchar("code", 50).uniqueIndex()
    val name = varchar("name", 255)
    val symbol = varchar("symbol", 10)
    val iconPath = varchar("icon_path", 500).nullable()
}
