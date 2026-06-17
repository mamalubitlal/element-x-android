package com.chtor.app.matrix

// Typed declarations for the JS bridge exposed in index.html as `window.chator`.
// Kotlin/Wasm K2 interop: external fun can use String directly, compiler handles JS conversion.

internal external object ChatorJs {
    fun xhr(method: String, url: String, body: String, auth: String): String
    fun now(): Double
    fun tzOffsetMin(): Int
    fun encodeURI(s: String): String
    fun clipboardCopy(text: String)
    fun registerPush(): String?
    fun pollPush(): String?
    fun openUrl(url: String)
    val storage: JsStorage
}

actual fun copyToClipboard(text: String) { ChatorJs.clipboardCopy(text) }
actual fun platformUserAgent(): String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"



actual fun platformOpenUrl(url: String) { ChatorJs.openUrl(url) }

internal external object JsStorage {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}

internal fun ChatorJs.xhr(method: String, url: String, body: String, auth: String?): String =
    xhr(method, url, body, auth ?: "")

internal fun JsStorage.getString(key: String): String? = get(key)
internal fun JsStorage.setString(key: String, value: String) { set(key, value) }
internal fun JsStorage.removeString(key: String) { remove(key) }
