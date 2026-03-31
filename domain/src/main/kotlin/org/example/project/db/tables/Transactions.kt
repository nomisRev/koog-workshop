package org.example.project.db.tables

object Transactions : StoreTable("transactions") {
    val character = reference("character_id", Characters)
    val currency = reference("currency_id", Currencies)
    val amount = long("amount")
    val type = varchar("type", 50)
    val referenceId = uuid("reference_id").nullable()
    val referenceType = varchar("reference_type", 50).nullable()
    val description = text("description").nullable()

    init {
        index(false, character, currency)
    }
}
