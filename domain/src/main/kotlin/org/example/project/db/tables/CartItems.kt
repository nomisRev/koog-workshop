package org.example.project.db.tables

object CartItems : StoreTable("cart_items") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)
    val quantity = integer("quantity").default(1)

    init {
        uniqueIndex(character, product)
    }
}
