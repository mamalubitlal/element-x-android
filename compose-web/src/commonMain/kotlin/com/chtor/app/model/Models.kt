package com.chtor.app.model

import com.chtor.app.screen.AccountProvider

// Centralized navigation targets. Keep simple — full Android app uses Appyx nodes,
// the PWA collapses to a stack of screens.
sealed interface Screen {
    data object Onboarding : Screen
    data object ChooseServer : Screen
    data class  ChangeServer(val initialUrl: String) : Screen
    data class  ConfirmServer(val provider: AccountProvider) : Screen
    data class  Login(val homeserver: String) : Screen
    data class  Register(val homeserver: String) : Screen
    data object Home : Screen
    data object Settings : Screen
    data object Search : Screen
    data class  Space(val spaceId: String, val spaceName: String) : Screen
    data class  Thread(val roomId: String, val roomName: String, val rootEventId: String, val rootBody: String) : Screen
    data class  RoomThreads(val roomId: String, val roomName: String) : Screen
    data object BugReport : Screen
    data class  Chat(val roomId: String, val roomName: String, val eventId: String? = null) : Screen
}
