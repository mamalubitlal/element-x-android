package io.github.romanvht.byedpi.library.jni

internal object ByeDpiJni {

    private var libraryLoaded = false

    init {
        try {
            System.loadLibrary("byedpi")
            libraryLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // Native library not available
        }
    }

    fun isAvailable(): Boolean = libraryLoaded

    external fun startNativeProxy(args: Array<String>): Int

    external fun stopNativeProxy(): Int
}
