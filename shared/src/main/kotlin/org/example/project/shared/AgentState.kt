package org.example.project.shared

import kotlinx.serialization.Serializable

@Serializable
enum class AgentState { Completed, None, Failed; }
