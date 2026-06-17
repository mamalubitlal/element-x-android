package com.chtor.app.matrix

// Public cross-platform helpers. `commonMain` callers can use these without
// touching the `internal` JS bridge in `wasmJsMain`.
expect fun copyToClipboard(text: String)
expect fun platformUserAgent(): String
expect fun platformOpenUrl(url: String)

