package com.jetbrains.example.koog.compose.agents.homeservices

import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.ext.agent.subgraphWithTask
import com.jetbrains.example.koog.compose.agents.common.AskUserTool

fun homeServicesSchedulingStrategy(
    askUserTool: AskUserTool,
    findTools: HomeServicesFindTools,
    bookTools: HomeServicesBookTools,
) = strategy<String, String>("home-services-scheduling") {
    // Phase 1: gather service details from the user (no search or booking tools)
    val assess by subgraphWithTask<String, String>(
        tools = askUserTool.asTools()
    ) { input ->
        """
        $homeServicesIntakeInstructions

        The user's initial message: $input
        """.trimIndent()
    }

    val compressHistory by nodeLLMCompressHistory<String>()

    // Phase 2: find slots and let the user pick one (no booking tool available)
    val selectSlot by subgraphWithTask<String, String>(
        tools = askUserTool.asTools() + findTools.asTools()
    ) { intakeResult ->
        """
        $homeServicesSlotSelectionInstructions

        Intake results:
        $intakeResult
        """.trimIndent()
    }

    // Phase 3: confirm the chosen date and time (only askUser — no tools to find or book)
    val confirmSlot by subgraphWithTask<String, String>(
        tools = askUserTool.asTools()
    ) { selectedSlot ->
        """
        $homeServicesConfirmationInstructions

        Selected slot details:
        $selectedSlot
        """.trimIndent()
    }

    // Phase 4: book the appointment (booking tool now available)
    val book by subgraphWithTask<String, String>(
        tools = askUserTool.asTools() + bookTools.asTools()
    ) { confirmedDetails ->
        """
        $homeServicesBookingInstructions

        Confirmed booking details:
        $confirmedDetails
        """.trimIndent()
    }

    // Phase 5: thank the user and ask for a satisfaction rating
    val finish by subgraphWithTask<String, String>(
        tools = askUserTool.asTools()
    ) { previousResult ->
        """
        $homeServicesFinishInstructions

        Conversation outcome:
        $previousResult
        """.trimIndent()
    }

    nodeStart then assess then compressHistory then selectSlot then confirmSlot

    edge(confirmSlot forwardTo selectSlot onCondition { it.contains("change_requested") })
    edge(confirmSlot forwardTo finish onCondition { it.contains("cancelled") })
    edge(confirmSlot forwardTo book onCondition { !it.contains("change_requested") && !it.contains("cancelled") })

    book then finish then nodeFinish
}
