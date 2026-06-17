package com.chtor.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.chtor.app.matrix.MatrixClient
import com.chtor.app.model.Screen
import com.chtor.app.screen.AccountProvider

// Top-level so `js()` is a single expression (Kotlin/Wasm restriction).
private val rawSearch: String = js("window.location.search || ''")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val initialScreen = parseScreen(rawSearch)
    val api = MatrixClient()
    println("ChatorApp starting, screen=$initialScreen")
    CanvasBasedWindow("Chator") {
        ChatorApp(api = api, initialScreen = initialScreen)
    }
    println("ChatorApp returned")
}

private fun parseScreen(search: String): Screen {
    if (!search.startsWith("?screen=")) return Screen.Onboarding
    return when (search.removePrefix("?screen=").lowercase()) {
        "choose"  -> Screen.ChooseServer
        "change"  -> Screen.ChangeServer("https://")
        "confirm" -> Screen.ConfirmServer(
            AccountProvider("Matrix.org", "https://matrix.org")
        )
        "login"   -> Screen.Login("https://matrix.org")
        "register" -> Screen.Register("https://matrix.org")
        "home"    -> Screen.Home
        else      -> Screen.Onboarding
    }
}
