package com.jetbrains.example.koog.compose.agents.homeservices

import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import com.jetbrains.example.koog.compose.agents.common.AskUserTool
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@LLMDescription("Result of the emergency check phase")
@Serializable
enum class EmergencyCheckResult {
    @LLMDescription("User acknowledged the emergency and will call emergency services; do not proceed with scheduling")
    EMERGENCY_ACKNOWLEDGED,
    @LLMDescription("No emergency detected; proceed with regular appointment scheduling")
    PROCEED_WITH_SCHEDULING,
}

@LLMDescription("Collected details required to schedule a home service visit")
@Serializable
data class IntakeResult(
    @LLMDescription("Type of home service needed: PLUMBING, ELECTRICAL, HVAC, or HANDYMAN")
    val serviceType: ServiceType,
    @LLMDescription("One-sentence description of the issue to be resolved")
    val issueSummary: String,
    @LLMDescription("Full service address")
    val address: String,
    @LLMDescription("Customer's full name")
    val customerName: String,
    @LLMDescription("Optional access instructions such as gate code, pet, parking, or buzzer notes")
    val accessNotes: String? = null,
)
/*
@LLMDescription("Outcome of the intake assessment phase: either all details collected or user cancelled")
@Serializable
sealed interface AssessResult {
    @LLMDescription("User chose to cancel the scheduling process")
    @Serializable
    @SerialName("Cancelled")
    data object Cancelled : AssessResult

    @LLMDescription("All required intake details were successfully collected")
    @Serializable
    @SerialName("Collected")
    data class Collected(
        @LLMDescription("The collected intake details")
        val details: IntakeResult,
    ) : AssessResult
}*/

@LLMDescription("Outcome of the intake assessment phase: either all details collected or user cancelled")
@Serializable
data class AssessResult(
    @LLMDescription("Intake details collected by the agent, if any")
    val collected: IntakeResult?,
    @LLMDescription("Whether the user chose to cancel the scheduling process")
    val cancelled: Boolean,
)

@LLMDescription("An available appointment slot selected by the customer")
@Serializable
data class SelectedSlot(
    @LLMDescription("Unique slot identifier returned by getAvailableSlots")
    val slotId: String,
    @LLMDescription("Appointment date in yyyy-MM-dd format")
    val date: String,
    @LLMDescription("Time window: Morning (9:00-12:00), Early afternoon (12:00-15:00), or Late afternoon (15:00-18:00)")
    val timeWindow: String,
    @LLMDescription("Intake details associated with this slot")
    val intake: IntakeResult,
)

@LLMDescription("Customer's confirmation decision for the proposed appointment slot")
@Serializable
enum class ConfirmationStatus {
    @LLMDescription("Customer confirmed the slot and wants to proceed with booking")
    CONFIRMED,
    @LLMDescription("Customer wants to pick a different slot")
    CHANGE_REQUESTED,
    @LLMDescription("Customer cancelled the scheduling process")
    CANCELLED,
}

@LLMDescription("Result of the confirmation phase: the customer's decision and the slot under review")
@Serializable
data class ConfirmResult(
    @LLMDescription("Customer's confirmation decision")
    val status: ConfirmationStatus,
    @LLMDescription("The slot that was presented to the customer for confirmation")
    val slot: SelectedSlot,
)

fun homeServicesSchedulingStrategy(
    askUserTool: AskUserTool,
    findTools: HomeServicesFindTools,
    bookTools: HomeServicesBookTools,
) = strategy<String, String>("home-services-scheduling") {
    // FIXME Let's try non String inputs/outputs in some of these subtasks, to showcase domain modeling approach which is one of the Koog's strengths
    // Phase 0: check whether the request is an emergency before any scheduling
    val checkEmergency by subgraphWithTask<String, EmergencyCheckResult>(
        tools = askUserTool.asTools()
    ) { input ->
        """
        $homeServicesEmergencyCheckInstructions

        The user's initial message: $input
        """.trimIndent()
    }

    // Phase 1: gather service details from the user (no search or booking tools)
    val assess by subgraphWithTask<EmergencyCheckResult, AssessResult>(
        tools = askUserTool.asTools()
    ) { _ ->
        """
        $homeServicesIntakeInstructions

        The user's initial message: ${agentInput<String>()}
        """.trimIndent()
    }

    val compressHistory by nodeLLMCompressHistory<IntakeResult>()

    // Phase 2: find slots and let the user pick one (no booking tool available)
    val selectSlot by subgraphWithTask<IntakeResult, SelectedSlot>(
        tools = askUserTool.asTools() + findTools.asTools()
    ) { intake ->
        """
        $homeServicesSlotSelectionInstructions

        Intake results:
        - Customer: ${intake.customerName}
        - Service type: ${intake.serviceType}
        - Issue: ${intake.issueSummary}
        - Address: ${intake.address}
        ${intake.accessNotes?.let { "- Access notes: $it" } ?: ""}
        """.trimIndent()
    }

    // Phase 3: confirm the chosen date and time (only askUser — no tools to find or book)
    val confirmSlot by subgraphWithTask<SelectedSlot, ConfirmResult>(
        tools = askUserTool.asTools()
    ) { slot ->
        """
        $homeServicesConfirmationInstructions

        Selected slot:
        - Date: ${slot.date}
        - Time window: ${slot.timeWindow}
        - Service type: ${slot.intake.serviceType}
        - Customer: ${slot.intake.customerName}
        - Address: ${slot.intake.address}
        ${slot.intake.accessNotes?.let { "- Notes: $it" } ?: ""}

        Slot JSON (use this verbatim as the "slot" value when returning the result):
        ${Json.encodeToString(slot)}
        """.trimIndent()
    }

    // Phase 4: book the appointment (booking tool now available)
    val book by subgraphWithTask<SelectedSlot, String>(
        tools = askUserTool.asTools() + bookTools.asTools()
    ) { slot ->
        """
        $homeServicesBookingInstructions

        Confirmed booking details:
        - Customer: ${slot.intake.customerName}
        - Service type: ${slot.intake.serviceType}
        - Issue: ${slot.intake.issueSummary}
        - Slot ID: ${slot.slotId}
        - Date: ${slot.date}
        - Time window: ${slot.timeWindow}
        - Address: ${slot.intake.address}
        ${slot.intake.accessNotes?.let { "- Notes: $it" } ?: ""}
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

    nodeStart then checkEmergency
    edge(checkEmergency forwardTo nodeFinish onCondition { it == EmergencyCheckResult.EMERGENCY_ACKNOWLEDGED } transformed { "Handling emergency" })
    edge(checkEmergency forwardTo assess onCondition { it == EmergencyCheckResult.PROCEED_WITH_SCHEDULING })

    edge(assess forwardTo finish onCondition { it.cancelled || it.collected == null } transformed { "cancelled" })
    edge(assess forwardTo compressHistory onCondition { !it.cancelled && it.collected != null } transformed { it.collected!! })

    compressHistory then selectSlot then confirmSlot

    edge(confirmSlot forwardTo selectSlot onCondition { it.status == ConfirmationStatus.CHANGE_REQUESTED } transformed { it.slot.intake })
    edge(confirmSlot forwardTo finish onCondition { it.status == ConfirmationStatus.CANCELLED } transformed { "cancelled" })
    edge(confirmSlot forwardTo book onCondition { it.status == ConfirmationStatus.CONFIRMED } transformed { it.slot })

    book then finish then nodeFinish
}
