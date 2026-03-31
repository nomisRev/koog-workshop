package org.example.project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.update
import java.nio.file.Path

class AdminAppViewModel(
    private val databaseSessionService: DatabaseSessionService
) : ViewModel() {

    private val sessionMutex = Mutex()
    private val _uiState = MutableStateFlow(AdminAppUiState())

    val uiState: StateFlow<AdminAppUiState> = _uiState.asStateFlow()

    suspend fun openDatabase(path: Path) {
        sessionMutex.withLock {
            val normalizedPath = path.toAbsolutePath().normalize()
            val hadActiveSession = databaseSessionService.databaseOrNull() != null

            _uiState.update {
                it.copy(
                    databaseConnectionState = DatabaseConnectionState.Connecting,
                    schemaValidationState = SchemaValidationState.Validating
                )
            }

            when (val result = databaseSessionService.openDatabase(normalizedPath)) {
                DatabaseSessionOpenResult.Success -> _uiState.update {
                    it.copy(
                        currentDestination = AdminDestination.Dashboard,
                        databaseConnectionState = DatabaseConnectionState.Connected,
                        selectedDatabasePath = normalizedPath.toString(),
                        schemaValidationState = SchemaValidationState.Valid
                    )
                }

                is DatabaseSessionOpenResult.Failure.InvalidSchema -> _uiState.update {
                    it.copy(
                        currentDestination = if (hadActiveSession) {
                            AdminDestination.Dashboard
                        } else {
                            AdminDestination.Startup
                        },
                        databaseConnectionState = if (hadActiveSession) {
                            DatabaseConnectionState.Connected
                        } else {
                            DatabaseConnectionState.Disconnected
                        },
                        selectedDatabasePath = if (hadActiveSession) {
                            it.selectedDatabasePath
                        } else {
                            null
                        },
                        schemaValidationState = SchemaValidationState.Invalid(result.message)
                    )
                }

                is DatabaseSessionOpenResult.Failure.InvalidDatabase -> _uiState.update {
                    it.copy(
                        currentDestination = if (hadActiveSession) {
                            AdminDestination.Dashboard
                        } else {
                            AdminDestination.Startup
                        },
                        databaseConnectionState = if (hadActiveSession) {
                            DatabaseConnectionState.Connected
                        } else {
                            DatabaseConnectionState.Disconnected
                        },
                        selectedDatabasePath = if (hadActiveSession) {
                            it.selectedDatabasePath
                        } else {
                            null
                        },
                        schemaValidationState = SchemaValidationState.Invalid(result.message)
                    )
                }

                is DatabaseSessionOpenResult.Failure.UnreadablePath -> _uiState.update {
                    it.copy(
                        currentDestination = if (hadActiveSession) {
                            AdminDestination.Dashboard
                        } else {
                            AdminDestination.Startup
                        },
                        databaseConnectionState = if (hadActiveSession) {
                            DatabaseConnectionState.Connected
                        } else {
                            DatabaseConnectionState.Disconnected
                        },
                        selectedDatabasePath = if (hadActiveSession) {
                            it.selectedDatabasePath
                        } else {
                            null
                        },
                        schemaValidationState = SchemaValidationState.Invalid(result.message)
                    )
                }
            }
        }
    }

    suspend fun closeDatabase() {
        sessionMutex.withLock {
            databaseSessionService.closeSession()
            _uiState.update {
                it.copy(
                    currentDestination = AdminDestination.Startup,
                    databaseConnectionState = DatabaseConnectionState.Disconnected,
                    selectedDatabasePath = null,
                    schemaValidationState = SchemaValidationState.Idle
                )
            }
        }
    }

    companion object {
        fun factory(databaseSessionService: DatabaseSessionService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == AdminAppViewModel::class) {
                        return AdminAppViewModel(databaseSessionService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }
}
