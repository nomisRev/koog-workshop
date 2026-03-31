package org.example.project.admin

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

enum class AdminDestination {
    Startup,
    Dashboard,
}

sealed interface DatabaseConnectionState {
    data object Disconnected : DatabaseConnectionState
    data object Connecting : DatabaseConnectionState
    data object Connected : DatabaseConnectionState
}

sealed interface SchemaValidationState {
    data object Idle : SchemaValidationState
    data object Validating : SchemaValidationState
    data object Valid : SchemaValidationState
    data class Invalid(val message: String) : SchemaValidationState
}

data class AdminAppUiState(
    val currentDestination: AdminDestination = AdminDestination.Startup,
    val databaseConnectionState: DatabaseConnectionState = DatabaseConnectionState.Disconnected,
    val selectedDatabasePath: String? = null,
    val schemaValidationState: SchemaValidationState = SchemaValidationState.Idle,
    val availableDestinations: PersistentList<AdminDestination> = persistentListOf(
        AdminDestination.Dashboard
    ),
)
