package org.example.project.db.tables

object Orders : StoreTable("orders") {
    val character = reference("character_id", Characters)
    val status = varchar("status", 50)              // OrderStatus.name
    val totalPrice = long("total_price")
    val totalCurrency = reference("total_currency_id", Currencies)
}

object SubOrders : StoreTable("sub_orders") {
    val order = reference("order_id", Orders)
    val merchant = reference("merchant_id", Merchants)
    val status = varchar("status", 50)              // OrderStatus.name
    val shippingMethod = reference("shipping_method_id", ShippingMethods)
    val shippingCost = long("shipping_cost")
    val merchantTotalPrice = long("merchant_total_price")
}

object OrderItems : StoreTable("order_items") {
    val subOrder = reference("sub_order_id", SubOrders)
    val product = reference("product_id", Products)
    val quantity = integer("quantity")
    val snapshottedPrice = long("snapshotted_price")
    val snapshottedCurrency = reference("snapshotted_currency_id", Currencies)
}
