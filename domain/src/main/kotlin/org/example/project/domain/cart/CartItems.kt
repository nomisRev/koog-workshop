package org.example.project.domain.cart

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters
import org.example.project.domain.catalog.Products

object CartItems : StoreTable("cart_items") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)
    val quantity = integer("quantity").default(1)

    init {
        uniqueIndex(character, product)
    }
}
