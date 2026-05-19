package com.jetbrains.koog.workshop.agents.homeservices.graph

import ai.koog.agents.core.tools.annotations.LLMDescription
import com.jetbrains.koog.workshop.agents.homeservices.ServiceType
import com.jetbrains.koog.workshop.agents.homeservices.UrgencyLevel
import kotlinx.serialization.Serializable

@LLMDescription("Result of the emergency triage phase")
@Serializable
data class TriageResult(
    @LLMDescription("The outcome of the emergency triage")
    val status: TriageOutcome,
    @LLMDescription("Brief justification of why this is an emergency. Required when status is EMERGENCY_DETECTED, null otherwise.")
    val justification: String? = null,
)

@Serializable
enum class TriageOutcome {
    @LLMDescription("Emergency detected; justification must be provided")
    emergency_detected,
    @LLMDescription("No emergency detected; proceed with regular appointment scheduling")
    proceed_with_scheduling,
    @LLMDescription("User cancelled the scheduling process")
    cancelled,
}

@LLMDescription("Collected details required to schedule a home service visit")
@Serializable
data class IssueDetails(
    @LLMDescription("Type of home service needed: PLUMBING, ELECTRICAL, HVAC, or HANDYMAN")
    val serviceType: ServiceType,
    @LLMDescription("One-sentence description of the issue to be resolved")
    val issueSummary: String,
    @LLMDescription("Full service address")
    val address: String,
    @LLMDescription("Customer's full name")
    val customerName: String,
    @LLMDescription("Urgency level: URGENT (significant disruption, within 2 days) or STANDARD (non-critical, scheduled)")
    val urgencyLevel: UrgencyLevel,
    @LLMDescription("Optional access instructions such as gate code, pet, parking, or buzzer notes")
    val accessNotes: String? = null,
    @LLMDescription("Optional time preference volunteered by the user (e.g. 'morning', 'after 3pm', 'Wednesday'). Never ask for this — only record if the user mentions it unprompted.")
    val timePreferencesNote: String? = null,
)

@LLMDescription("Outcome of the issue details collection phase: either all details collected or user cancelled")
@Serializable
data class IssueDetailsOutcome(
    @LLMDescription("Issue details collected by the agent, if any")
    val collected: IssueDetails?,
    @LLMDescription("Whether the user chose to cancel the scheduling process")
    val cancelled: Boolean,
) {
    fun success() = collected != null && !cancelled
}

@LLMDescription("Outcome of the slot selection phase: either a slot was chosen or user cancelled")
@Serializable
data class SlotSelectionOutcome(
    @LLMDescription("The slot selected by the customer, if any")
    val selected: SelectedSlot?,
    @LLMDescription("Whether the user chose to cancel the scheduling process")
    val cancelled: Boolean,
) {
    fun success() = selected != null && !cancelled
}

@LLMDescription("An available appointment slot selected by the customer")
@Serializable
data class SelectedSlot(
    @LLMDescription("Unique slot identifier returned by getAvailableSlots")
    val slotId: String,
    @LLMDescription("Appointment date in yyyy-MM-dd format")
    val date: String,
    @LLMDescription("Time window: Morning (9:00-12:00), Early afternoon (12:00-15:00), or Late afternoon (15:00-18:00)")
    val timeWindow: String
)

@LLMDescription("Customer's confirmation decision for the proposed appointment")
@Serializable
enum class ConfirmationOutcome {
    @LLMDescription("Customer confirmed the slot and wants to proceed with booking")
    confirmed,
    @LLMDescription("Customer wants to pick a different slot")
    change_requested,
    @LLMDescription("Customer cancelled the scheduling process")
    cancelled,
}
