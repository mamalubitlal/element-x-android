package com.chtor.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chtor.app.matrix.MatrixApi
import com.chtor.app.model.Screen
import com.chtor.app.screen.AccountProvider
import com.chtor.app.screen.ChangeServerScreen
import com.chtor.app.screen.ChatScreen
import com.chtor.app.screen.ChooseAccountProviderScreen
import com.chtor.app.screen.ConfirmAccountProviderScreen
import com.chtor.app.screen.HomeScreen
import com.chtor.app.screen.LoginPasswordScreen
import com.chtor.app.screen.OnboardingScreen
import com.chtor.app.screen.RegisterScreen
import com.chtor.app.screen.SearchScreen
import com.chtor.app.screen.SettingsScreen
import com.chtor.app.screen.SpaceScreen
import com.chtor.app.screen.ThreadScreen
import com.chtor.app.screen.RoomThreadsScreen
import com.chtor.app.screen.BugReportScreen
import kotlinx.coroutines.launch

@Composable
fun ChatorApp(
    api: MatrixApi,
    initialScreen: Screen = Screen.Onboarding,
) {
    MaterialTheme(colorScheme = ChatorDarkScheme) {
        Surface(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            var screen by remember { mutableStateOf(initialScreen) }
            var registerMode by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            // Dev: ?screen=choose|change|confirm|login|home — skip click testing.
            // initialScreen is set from URL params in Main.kt (wasmJsMain).
            LaunchedEffect(Unit) {
                if (initialScreen == Screen.Onboarding && api.isLoggedIn()) {
                    screen = Screen.Home
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val s = screen) {
                    Screen.Onboarding -> OnboardingScreen(
                        onContinue = { registerMode = false; screen = Screen.ChooseServer },
                        onCreateAccount = { registerMode = true; screen = Screen.ChooseServer }
                    )
                    Screen.ChooseServer -> ChooseAccountProviderScreen(
                        onBack = { screen = Screen.Onboarding },
                        onProviderSelected = { provider ->
                            screen = Screen.ConfirmServer(provider)
                        },
                        onOther = { screen = Screen.ChangeServer("https://") }
                    )
                    is Screen.ChangeServer -> ChangeServerScreen(
                        initialUrl = s.initialUrl,
                        onBack = { screen = Screen.ChooseServer },
                        onSubmit = { url ->
                            screen = Screen.ConfirmServer(
                                AccountProvider(
                                    title = url.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                                    url = url
                                )
                            )
                        }
                    )
                    is Screen.ConfirmServer -> ConfirmAccountProviderScreen(
                        provider = s.provider,
                        isAccountCreation = registerMode,
                        onContinue = {
                            if (registerMode) { registerMode = false; screen = Screen.Register(s.provider.url) }
                            else screen = Screen.Login(s.provider.url)
                        },
                        onChange = { screen = Screen.ChooseServer }
                    )
                    is Screen.Register -> RegisterScreen(
                        api = api,
                        homeserver = s.homeserver,
                        onBack = { screen = Screen.ConfirmServer(
                            AccountProvider(title = s.homeserver, url = s.homeserver)
                        ) },
                        onChangeServer = { screen = Screen.ChooseServer },
                        onRegistered = {
                            scope.launch { runCatching { api.registerPush() } }
                            screen = Screen.Home
                        }
                    )
                    is Screen.Login -> LoginPasswordScreen(
                        api = api,
                        homeserver = s.homeserver,
                        onBack = { screen = Screen.ConfirmServer(
                            AccountProvider(title = s.homeserver, url = s.homeserver)
                        ) },
                        onChangeServer = { screen = Screen.ChooseServer },
                        onLoggedIn = {
                            // Best-effort: register web-push so background notifications work.
                            scope.launch { runCatching { api.registerPush() } }
                            screen = Screen.Home
                        }
                    )
                    Screen.Home -> HomeScreen(
                        api = api,
                        onOpenChat = { room -> screen = Screen.Chat(room.id, room.name) },
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenSearch = { screen = Screen.Search },
                        onOpenSpace = { id, name -> screen = Screen.Space(id, name) }
                    )
                    Screen.Search -> SearchScreen(
                        api = api,
                        onBack = { screen = Screen.Home },
                        onOpenHit = { roomId, roomName, eventId ->
                            screen = Screen.Chat(roomId, roomName, eventId)
                        }
                    )
                    Screen.Settings -> SettingsScreen(
                        api = api,
                        onBack = { screen = Screen.Home },
                        onOpenBugReport = { screen = Screen.BugReport },
                        onLogout = {
                            scope.launch {
                                api.logout()
                                screen = Screen.Onboarding
                            }
                        }
                    )
                    Screen.BugReport -> BugReportScreen(
                        api = api,
                        onBack = { screen = Screen.Settings }
                    )
                    is Screen.Chat -> ChatScreen(
                        api = api,
                        roomId = s.roomId,
                        roomName = s.roomName,
                        eventId = s.eventId,
                        onBack = { screen = Screen.Home },
                        onOpenThread = { rootId, rootBody ->
                            screen = Screen.Thread(s.roomId, s.roomName, rootId, rootBody)
                        },
                        onOpenRoomThreads = { screen = Screen.RoomThreads(s.roomId, s.roomName) }
                    )
                    is Screen.Space -> SpaceScreen(
                        api = api,
                        spaceId = s.spaceId,
                        spaceName = s.spaceName,
                        onBack = { screen = Screen.Home },
                        onOpenRoom = { roomId, roomName -> screen = Screen.Chat(roomId, roomName) },
                        onOpenSpace = { id, name -> screen = Screen.Space(id, name) }
                    )
                    is Screen.Thread -> ThreadScreen(
                        api = api,
                        roomId = s.roomId,
                        roomName = s.roomName,
                        rootEventId = s.rootEventId,
                        rootBody = s.rootBody,
                        onBack = { screen = Screen.Home }
                    )
                    is Screen.RoomThreads -> RoomThreadsScreen(
                        api = api,
                        roomId = s.roomId,
                        roomName = s.roomName,
                        onBack = { screen = Screen.Home },
                        onOpenThread = { rootId, rootBody ->
                            screen = Screen.Thread(s.roomId, s.roomName, rootId, rootBody)
                        }
                    )
                }
            }
        }
    }
}


