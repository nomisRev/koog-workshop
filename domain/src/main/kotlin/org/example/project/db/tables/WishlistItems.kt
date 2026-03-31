package org.example.project.db.tables

object WishlistItems : StoreTable("wishlist_items") {
    val character = reference("character_id", Characters)
    val product = reference("product_id", Products)

    init {
        uniqueIndex(character, product)
    }
}
