package org.example.project.domain.model

data class Page<T>(
    val items: List<T>,
    val total: Long,
    val offset: Long,
    val limit: Long
)
