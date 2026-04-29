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
    // FIXME Let's try non String inputs/outputs in some of these subtasks, to showcase domain modeling approach which is one of the Koog's strengths
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

    // FIXME If we introduce domain modeling approach, these conditions will get cleaner, with explicit boolean expressions instead of string matching
    //  Check e.g. how nodes are wired in this example using domain modeling approach
    //  https://github.com/JetBrains/koog/blob/06f44722b6c221d9e61de4aa9814f60b68e8b38a/examples/simple-examples/src/main/kotlin/ai/koog/agents/example/a2a/advancedjoke/JokeWriterAgentExecutor.kt#L302
    edge(confirmSlot forwardTo selectSlot onCondition { it.contains("change_requested") })
    edge(confirmSlot forwardTo finish onCondition { it.contains("cancelled") })
    edge(confirmSlot forwardTo book onCondition { !it.contains("change_requested") && !it.contains("cancelled") })

    book then finish then nodeFinish
}
