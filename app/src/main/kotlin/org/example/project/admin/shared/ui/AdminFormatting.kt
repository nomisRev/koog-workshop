package org.example.project.admin.shared.ui

import org.example.project.admin.products.ProductActiveFilter
import org.example.project.admin.products.ProductReviewSummary
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

private val adminDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

internal fun Long.formatAmount(currencyCode: String): String = "$this $currencyCode"

internal fun Enum<*>.labelize(): String = name.labelize()

internal fun ProductActiveFilter.labelize(): String =
    when (this) {
        ProductActiveFilter.ALL -> "All"
        ProductActiveFilter.ACTIVE -> "Active"
        ProductActiveFilter.INACTIVE -> "Inactive"
    }

internal fun String.labelize(): String =
    lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { segment ->
            segment.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase()
                } else {
                    char.toString()
                }
            }
        }

internal fun ProductReviewSummary.toDisplayText(): String =
    if (reviewCount == 0 || averageRating == null) {
        "No reviews yet"
    } else {
        "${String.format(Locale.US, "%.1f", averageRating)} / 5 from $reviewCount reviews"
    }

internal fun Instant.formatAdminInstant(): String =
    adminDateFormatter.format(JavaInstant.ofEpochMilli(toEpochMilliseconds()))
