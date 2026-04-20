package com.jetbrains.example.koog.compose.agents.homeservices

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

enum class ServiceType {
    PLUMBING, ELECTRICAL, HVAC, CLEANING, HANDYMAN
}

enum class Urgency {
    EMERGENCY, SOON, FLEXIBLE
}

enum class TimeWindow(val label: String, val hours: String, val startHour: Int) {
    MORNING("Morning", "9:00–12:00", 9),
    EARLY_AFTERNOON("Early afternoon", "12:00–15:00", 12),
    LATE_AFTERNOON("Late afternoon", "15:00–18:00", 15),
}

data class Slot(
    val id: String,
    val serviceType: ServiceType,
    val date: LocalDate,
    val timeWindow: TimeWindow,
    var booked: Boolean = false,
    var customerName: String? = null,
    var address: String? = null,
    var notes: String? = null,
)

/**
 * Shared schedule data used by both [HomeServicesFindTools] and [HomeServicesBookTools].
 */
class HomeServicesSchedule {
    val today: LocalDate = LocalDate.now()
    val slots: MutableList<Slot> = generateSlots()

    private fun tradeWindows(type: ServiceType): List<TimeWindow> = when (type) {
        ServiceType.PLUMBING -> listOf(TimeWindow.MORNING, TimeWindow.EARLY_AFTERNOON)
        ServiceType.ELECTRICAL -> listOf(TimeWindow.MORNING, TimeWindow.LATE_AFTERNOON)
        ServiceType.HVAC -> listOf(TimeWindow.MORNING, TimeWindow.EARLY_AFTERNOON, TimeWindow.LATE_AFTERNOON)
        ServiceType.CLEANING -> listOf(TimeWindow.MORNING, TimeWindow.EARLY_AFTERNOON)
        ServiceType.HANDYMAN -> listOf(TimeWindow.EARLY_AFTERNOON, TimeWindow.LATE_AFTERNOON)
    }

    private fun availableOnSaturday(type: ServiceType): Boolean = when (type) {
        ServiceType.PLUMBING, ServiceType.HVAC, ServiceType.CLEANING -> true
        ServiceType.ELECTRICAL, ServiceType.HANDYMAN -> false
    }

    private fun generateSlots(): MutableList<Slot> {
        val result = mutableListOf<Slot>()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        for (dayOffset in 0..13) {
            val date = today.plusDays(dayOffset.toLong())
            if (date.dayOfWeek == DayOfWeek.SUNDAY) continue

            for (type in ServiceType.entries) {
                if (date.dayOfWeek == DayOfWeek.SATURDAY && !availableOnSaturday(type)) continue

                for (window in tradeWindows(type)) {
                    val id = "svc_${type.name.lowercase()}_${date.format(dateFormatter)}_${window.name.lowercase()}_1"
                    result.add(Slot(id = id, serviceType = type, date = date, timeWindow = window))
                }
            }
        }

        // Pre-book more slots in the nearest days, fewer further out
        val sampleNames = listOf("J. Smith", "M. Garcia", "A. Johnson", "R. Patel", "K. Williams")
        for (slot in result) {
            val daysAway = (slot.date.toEpochDay() - today.toEpochDay()).toInt()
            val hash = slot.id.hashCode().absoluteValue
            // Days 0-2: ~60% booked, days 3-5: ~40%, days 6-9: ~20%, days 10+: ~10%
            val threshold = when {
                daysAway <= 2 -> 5
                daysAway <= 5 -> 5
                daysAway <= 9 -> 5
                else -> 10
            }
            val cutoff = when {
                daysAway <= 2 -> 3
                daysAway <= 5 -> 2
                daysAway <= 9 -> 1
                else -> 1
            }
            if (hash % threshold < cutoff) {
                slot.booked = true
                slot.customerName = sampleNames[hash % sampleNames.size]
            }
        }

        return result
    }
}

/**
 * Tool set for searching available slots. Does NOT include booking.
 */
class HomeServicesFindTools(private val schedule: HomeServicesSchedule) : ToolSet {

    @Tool
    @LLMDescription(
        "Find available appointment slots for a home service. Returns only unbooked slots matching the criteria. " +
                "The schedule covers the next 14 days from today (Sundays excluded)."
    )
    fun findAvailableSlots(
        @LLMDescription("Service type: PLUMBING, ELECTRICAL, HVAC, CLEANING, or HANDYMAN") serviceType: String,
        @LLMDescription("Urgency level: EMERGENCY (same-day / next-day preferred), SOON (within 3 days), or FLEXIBLE (any open slot)") urgency: String,
        @LLMDescription("Preferred day in yyyy-MM-dd format, or 'any' to see all available days") preferredDay: String,
        @LLMDescription("Preferred time window: MORNING (9-12), EARLY_AFTERNOON (12-15), LATE_AFTERNOON (15-18), or 'any'") timeWindow: String,
    ): String {
        val type = ServiceType.entries.find { it.name.equals(serviceType, ignoreCase = true) }
            ?: return "Error: Invalid service type '$serviceType'. Valid: ${ServiceType.entries.joinToString()}."

        val urg = Urgency.entries.find { it.name.equals(urgency, ignoreCase = true) }
            ?: return "Error: Invalid urgency '$urgency'. Valid: ${Urgency.entries.joinToString()}."

        val prefDay = if (preferredDay.equals("any", ignoreCase = true)) null
        else runCatching { LocalDate.parse(preferredDay) }.getOrNull()
            ?: return "Error: Invalid date '$preferredDay'. Use yyyy-MM-dd or 'any'."

        val prefWindow = if (timeWindow.equals("any", ignoreCase = true)) null
        else TimeWindow.entries.find { it.name.equals(timeWindow, ignoreCase = true) }
            ?: return "Error: Invalid time window '$timeWindow'. Valid: ${TimeWindow.entries.joinToString()} or 'any'."

        val now = LocalTime.now()
        var matches = schedule.slots.filter { slot ->
            !slot.booked
                    && slot.serviceType == type
                    && (prefDay == null || slot.date == prefDay)
                    && (prefWindow == null || slot.timeWindow == prefWindow)
                    // Skip today's windows that have already started
                    && !(slot.date == schedule.today && now.hour >= slot.timeWindow.startHour)
        }

        // Narrow results by urgency preference
        if (urg == Urgency.EMERGENCY) {
            val urgent = matches.filter { it.date <= schedule.today.plusDays(1) }
            if (urgent.isNotEmpty()) matches = urgent
        } else if (urg == Urgency.SOON) {
            val soon = matches.filter { it.date <= schedule.today.plusDays(3) }
            if (soon.isNotEmpty()) matches = soon
        }

        if (matches.isEmpty()) {
            val nextAvailable = schedule.slots.firstOrNull { slot ->
                !slot.booked
                        && slot.serviceType == type
                        && !(slot.date == schedule.today && now.hour >= slot.timeWindow.startHour)
            }
            return if (nextAvailable != null) {
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val day = nextAvailable.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
                "No available slots found for $type with the given criteria. " +
                        "The earliest available slot is on $day ${nextAvailable.date.format(fmt)}, " +
                        "${nextAvailable.timeWindow.label} (${nextAvailable.timeWindow.hours})."
            } else {
                "No available slots found for $type in the next 14 days."
            }
        }

        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return buildString {
            appendLine("Available slots (${matches.size} found):")
            for (slot in matches) {
                val day = slot.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
                appendLine("  • ${slot.id} — $day ${slot.date.format(fmt)}, ${slot.timeWindow.label} (${slot.timeWindow.hours})")
            }
        }
    }
}

/**
 * Tool set for booking an appointment. Separated from [HomeServicesFindTools]
 * so the strategy can gate access: the LLM can only book after confirmation.
 */
class HomeServicesBookTools(private val schedule: HomeServicesSchedule) : ToolSet {

    @Tool
    @LLMDescription("Book a home service appointment into a specific slot. Fails if the slot is already booked or invalid.")
    fun scheduleAppointment(
        @LLMDescription("Customer's full name") customerName: String,
        @LLMDescription("Service type: PLUMBING, ELECTRICAL, HVAC, CLEANING, or HANDYMAN") serviceType: String,
        @LLMDescription("Slot ID from findAvailableSlots, e.g. svc_plumbing_20260422_morning_1") slotId: String,
        @LLMDescription("Service address") address: String,
        @LLMDescription("Optional access notes (gate code, pet, parking, etc.) or empty string") notes: String,
    ): String {
        val slot = schedule.slots.find { it.id == slotId }
            ?: return "Error: Unknown slot ID '$slotId'."

        if (slot.booked) return "Error: Slot $slotId is already booked."

        val type = ServiceType.entries.find { it.name.equals(serviceType, ignoreCase = true) }
            ?: return "Error: Invalid service type '$serviceType'."

        if (slot.serviceType != type) {
            return "Error: Slot $slotId is for ${slot.serviceType}, not $type."
        }

        slot.booked = true
        slot.customerName = customerName
        slot.address = address
        slot.notes = notes.ifBlank { null }

        val dayName = slot.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return buildString {
            appendLine("Appointment confirmed!")
            appendLine("  Service: ${slot.serviceType}")
            appendLine("  Customer: $customerName")
            appendLine("  Date: $dayName, ${slot.date.format(fmt)}")
            appendLine("  Window: ${slot.timeWindow.label} (${slot.timeWindow.hours})")
            appendLine("  Address: $address")
            if (!notes.isNullOrBlank()) appendLine("  Notes: $notes")
            appendLine("  Booking ID: $slotId")
        }
    }
}
