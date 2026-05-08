package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.admin.app.AdminRoute
import org.example.project.chat.ChatScreen
import org.example.project.chat.ChatViewModel
import org.example.project.domain.character.Character
import org.example.project.login.LoginScreen
import kotlin.uuid.Uuid

private sealed interface Screen {
    data object Login : Screen
    data class Chat(val character: Character) : Screen
}

fun main() {
    val session = Uuid.random()
    println("Session: $session")

    val dependencies = dependencies()

    application {
        var adminWindowOpen by remember { mutableStateOf(false) }
        var screen by remember { mutableStateOf<Screen>(Screen.Login) }
        var characters by remember { mutableStateOf<List<Character>>(emptyList()) }

        LaunchedEffect(Unit) {
            characters = dependencies.characterServices.characterService.listCharacters()
        }

        MaterialTheme(AppColorScheme) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Fantasy Store Chat",
                state = rememberWindowState(
                    size = DpSize(1200.dp, 800.dp)
                )
            ) {
                when (val current = screen) {
                    is Screen.Login -> {
                        LoginScreen(
                            characters = characters,
                            onAdminClick = { adminWindowOpen = true },
                            onCharacterSelected = { screen = Screen.Chat(it) }
                        )
                    }

                    is Screen.Chat -> {
                        val chatViewModel: ChatViewModel = viewModel(
                            key = current.character.id.value.toString(),
                            factory = ChatViewModel.factory(
                                character = current.character,
                                session = session,
                                chatAgentProvider = dependencies.chatAgentProvider,
                                chatService = dependencies.characterServices.chatService,
                                historyProvider = dependencies.chatHistoryProvider,
                                onNavigateBack = { screen = Screen.Login },
                            )
                        )
                        ChatScreen(viewModel = chatViewModel)
                    }
                }

                if (adminWindowOpen) {
                    Window(
                        onCloseRequest = { adminWindowOpen = false },
                        title = "Fantasy Store Admin",
                        state = rememberWindowState(
                            size = DpSize(1500.dp, 800.dp),
                            position = WindowPosition(100.dp, 100.dp)
                        )
                    ) {
                        AdminRoute(dependencies.storeServices)
                    }
                }
            }
        }
    }
}
