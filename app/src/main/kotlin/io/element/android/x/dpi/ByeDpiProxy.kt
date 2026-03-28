package io.element.android.x.dpi

import android.util.Log

class ByeDpiProxy {
    companion object {
        private const val TAG = "ByeDpiProxy"
        
        init {
            try {
                System.loadLibrary("byedpi")
                Log.i(TAG, "Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }
    
    external fun nativeStart(args: Array<String>): Int
    external fun nativeStop(): Int
    external fun nativeIsRunning(): Int
    
    fun start(command: String): Boolean {
        val args = command.split(" ").toTypedArray()
        val result = nativeStart(args)
        return result == 0
    }
    
    fun stop(): Boolean {
        val result = nativeStop()
        return result == 0
    }
    
    fun isRunning(): Boolean {
        return nativeIsRunning() == 1
    }
}