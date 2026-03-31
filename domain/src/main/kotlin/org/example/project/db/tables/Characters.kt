package org.example.project.db.tables

object Characters : StoreTable("characters") {
    val name = varchar("name", 255).uniqueIndex()
}
