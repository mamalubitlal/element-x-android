package com.chtor.app

import com.chtor.app.matrix.ChatorJs

// expect/actual — keeps time-zone-aware formatters in commonMain, JS impl in wasmJsMain.
actual fun currentTimeMillis(): Long = ChatorJs.now().toLong()

actual fun timezoneOffsetMillis(): Long = -ChatorJs.tzOffsetMin() * 60_000L
