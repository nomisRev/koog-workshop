package org.example.project.domain.model

import org.example.project.domain.id.CharacterId
import kotlin.time.Instant

data class Character(
    val id: CharacterId,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
