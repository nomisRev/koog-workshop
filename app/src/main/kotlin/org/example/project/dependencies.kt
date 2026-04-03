package org.example.project

import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import org.example.project.chat.ChatAgent
import org.example.project.db.createDataSource
import org.example.project.db.createDatabase
import org.example.project.admin.merchants.AdminMerchantService
import org.example.project.admin.orders.operations.AdminOrderService
import org.example.project.admin.products.AdminProductService
import org.example.project.koog.JdbcChatHistoryProvider
import java.lang.System.getenv

fun dependencies(): Dependencies {
    val dataSource = createDataSource()
    val chatProvider = JdbcChatHistoryProvider(dataSource).also { it.createTable() }
    val database = createDatabase(dataSource)
    val productService = AdminProductService(database)
    val merchantService = AdminMerchantService(database)
    val orderService = AdminOrderService(database)
    val executor = simpleOpenAIExecutor(requireNotNull(getenv("OPENAI_API_KEY")) { "OPENAI_API_KEY not set" })
    val chat = ChatAgent(executor = executor, history = chatProvider)
    return Dependencies(Dependencies.Services(productService, merchantService, orderService), chat)
}

class Dependencies(
    val services: Services,
    val chat: ChatAgent
) {
    class Services(
        val productService: AdminProductService,
        val merchantService: AdminMerchantService,
        val orderService: AdminOrderService
    )
}
