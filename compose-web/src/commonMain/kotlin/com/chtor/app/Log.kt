package com.chtor.app

/**
 * Minimal ring-buffered in-memory log. Used by the bug report screen.
 * Survives only for the session; not persisted to localStorage.
 *
 * The intent is to capture the last [CAPACITY] lines of activity so a user can
 * attach them to a bug report without us shipping a heavy logging framework.
 *
 * Not thread-safe by design: the app logs from coroutine contexts and the
 * Compose main thread; concurrent access from multiple real OS threads would
 * require a lock — YAGNI.
 */
object Log {
    private const val CAPACITY = 200

    data class Entry(
        val ts: Long,
        val level: Level,
        val tag: String,
        val message: String
    )

    enum class Level { INFO, WARN, ERROR }

    private val ring = ArrayDeque<Entry>()

    fun i(tag: String, message: String) = add(Level.INFO, tag, message)
    fun w(tag: String, message: String) = add(Level.WARN, tag, message)
    fun e(tag: String, message: String) = add(Level.ERROR, tag, message)
    fun e(tag: String, t: Throwable)    = add(Level.ERROR, tag, t.toString())

    fun tail(n: Int = CAPACITY): List<Entry> {
        val size = ring.size
        if (size == 0) return emptyList()
        val take = n.coerceAtMost(size)
        return ring.toList().subList(size - take, size)
    }

    fun clear() { ring.clear() }

    private fun add(level: Level, tag: String, message: String) {
        ring.addLast(Entry(currentTimeMillis(), level, tag, message))
        while (ring.size > CAPACITY) ring.removeFirst()
    }
}

/** Format the last N log entries as a plain-text block for copy-paste. */
fun formatLogReport(entries: List<Log.Entry>): String = buildString {
    for (e in entries) {
        val level = when (e.level) {
            Log.Level.INFO  -> "I"
            Log.Level.WARN  -> "W"
            Log.Level.ERROR -> "E"
        }
        append(formatHmsMs(e.ts)).append(' ')
            .append(level).append('/').append(e.tag).append(": ")
            .append(e.message).append('\n')
    }
}

private fun formatHmsMs(ts: Long): String {
    val totalSec = ts / 1000
    val ms = (ts % 1000).toInt()
    val sec = (totalSec % 60).toInt()
    val min = ((totalSec / 60) % 60).toInt()
    val hr  = ((totalSec / 3600) % 24).toInt()
    return "${hr.pad2()}:${min.pad2()}:${sec.pad2()}.${ms.pad3()}"
}

private fun Int.pad2(): String = if (this < 10) "0$this" else this.toString()
private fun Int.pad3(): String = when {
    this < 10   -> "00$this"
    this < 100  -> "0$this"
    else        -> this.toString()
}
