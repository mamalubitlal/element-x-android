# ByeDPI Library for Android

A Kotlin library that provides an easy-to-use API for testing and selecting DPI bypass strategies using ByeDPI.

## Features

- **Server Management**: Start and stop the ByeDPI proxy server
- **72+ Built-in Strategies**: Pre-configured bypass strategies for various scenarios
- **6 Site Lists**: Default site lists for YouTube, Google Video, Discord, Cloudflare, Social Media, and General Sites
- **Strategy Testing**: Test multiple strategies against multiple sites with configurable parameters
- **Strategy Picker**: Browse, filter, search, and select strategies with ease
- **Site List Picker**: Manage and activate/deactivate site lists
- **Custom Strategies**: Add your own custom bypass strategies
- **Custom Site Lists**: Create and manage your own site lists
- **Import/Export**: Import and export strategies and site lists in JSON or text format
- **Progress Tracking**: Real-time progress updates during testing
- **Result Analysis**: Get best strategies, filter by success rate, etc.
- **Auto-Discovery**: Automatically find the best strategy for your network

## Installation

Add the library to your project's `settings.gradle.kts`:

```kotlin
include(":library")
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":library"))
}
```

## Quick Start

### Starting the Proxy Server

```kotlin
import io.github.romanvht.byedpi.library.ByeDpiLibrary
import io.github.romanvht.byedpi.library.server.ProxyConfig

// Create library instance
val library = ByeDpiLibrary()

// Start with default configuration
library.startServer(ProxyConfig.DEFAULT)

// Or with custom configuration
val config = ProxyConfig(
    ip = "127.0.0.1",
    port = 1080,
    httpConnect = true
)
library.startServer(config)

// Check if server is running
if (library.isServerRunning) {
    println("Server running on ${library.getServer().currentConfig?.address}")
}

// Stop server when done
library.stopServer()
```

### Auto-Start Best Strategy

```kotlin
// Automatically find and start the best strategy
val bestStrategy = library.autoStartBestStrategy(
    siteListIds = listOf("youtube", "discord"),
    minSuccessPercentage = 50,
    onProgress = { progress, message ->
        println("${(progress * 100).toInt()}% - $message")
    }
)

if (bestStrategy != null) {
    println("Started with strategy: ${bestStrategy.name}")
    println("Success rate: ${bestStrategy.successPercentage}%")
} else {
    println("No working strategy found")
}
```

### Basic Usage

```kotlin
import io.github.romanvht.byedpi.library.ByeDpiLibrary

// Create library instance
val library = ByeDpiLibrary(proxyHost = "127.0.0.1", proxyPort = 1080)

// Get default strategies
val strategies = library.getDefaultStrategies()

// Get default sites
val sites = library.getActiveDomains()

// Test strategies
val results = library.testStrategies(
    strategies = strategies,
    sites = sites,
    config = TestConfig.DEFAULT
)

// Get best strategy (with at least 50% success rate)
val best = library.getBestStrategy(results, minSuccessPercentage = 50)

// Start server with the best strategy
if (best != null) {
    library.startServerWithStrategy(best)
}
```

### Strategy Picker

```kotlin
import io.github.romanvht.byedpi.library.picker.StrategyPicker

val picker = StrategyPicker()

// Get all available strategies
val strategies = picker.getAvailableStrategies()

// Filter by category
val sniStrategies = picker.filterByCategory(StrategyCategory.SNI_BASED)

// Search strategies
val results = picker.search("fragment")

// Add custom strategy
picker.addCustomStrategy("-d1 -s1 -a1", "My Strategy", "Custom strategy")

// Select a strategy
picker.selectStrategy(strategy)

// Get selected strategies
val selected = picker.getSelectedStrategies()
```

### Site List Picker

```kotlin
import io.github.romanvht.byedpi.library.picker.SiteListPicker

val picker = SiteListPicker()

// Get available lists
val lists = picker.getAvailableLists()

// Activate specific lists
picker.activateList("youtube")
picker.activateList("discord")

// Get all domains from active lists
val domains = picker.getActiveDomains()

// Add custom list
picker.addCustomList(
    name = "My Sites",
    domains = listOf("example.com", "test.com"),
    isActive = true
)
```

### Testing with Defaults

```kotlin
// Test with all default strategies and active site lists
val results = library.testWithDefaults()

// Test with specific site lists only
val results = library.testWithDefaults(
    siteListIds = listOf("youtube", "discord")
)

// Test only SNI-based strategies
val results = library.testWithDefaults(
    strategyCategory = StrategyCategory.SNI_BASED
)

// With progress callback
val results = library.testWithDefaults(
    onProgress = { progress ->
        println("Progress: ${(progress * 100).toInt()}%")
    }
)
```

### Custom Test Configuration

```kotlin
val config = TestConfig(
    delaySeconds = 2,
    requestsPerSite = 3,
    requestTimeoutSeconds = 10,
    maxConcurrentRequests = 10,
    sniValue = "google.com",
    fullLog = true
)

val results = library.testStrategies(
    strategies = strategies,
    sites = sites,
    config = config,
    onStrategyStart = { index, strategy ->
        println("Testing strategy ${index + 1}: ${strategy.name}")
    },
    onSiteChecked = { strategyIndex, siteResult ->
        println("${siteResult.site}: ${siteResult.successPercentage}%")
    },
    onStrategyComplete = { index, strategy ->
        println("Strategy ${index + 1} complete: ${strategy.successPercentage}%")
    }
)
```

## API Reference

### ByeDpiLibrary

Main entry point for the library.

```kotlin
class ByeDpiLibrary(proxyHost: String = "127.0.0.1", proxyPort: Int = 1080)
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isTesting` | `StateFlow<Boolean>` | Whether a test is running |
| `currentStrategyIndex` | `StateFlow<Int>` | Current strategy being tested |
| `testingProgress` | `StateFlow<Float>` | Overall progress (0.0 to 1.0) |
| `isServerRunning` | `Boolean` | Whether the server is running |
| `serverStatus` | `ServerStatus` | Current server status |

#### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `startServer(config)` | `ServerResult` | Start the ByeDPI proxy server |
| `startServerWithStrategy(strategy, sniValue)` | `ServerResult` | Start server with a strategy |
| `startServerExternal(binaryPath, config)` | `ServerResult` | Start with external binary |
| `stopServer()` | `ServerResult` | Stop the proxy server |
| `restartServer(config?)` | `ServerResult` | Restart the server |
| `pingServer()` | `Boolean` | Check if server is responsive |
| `autoStartBestStrategy(...)` | `Strategy?` | Auto-find and start best strategy |
| `isNativeLibraryAvailable()` | `Boolean` | Check if native library is loaded |
| `getDefaultStrategies()` | `List<Strategy>` | Get all default strategies |
| `getStrategiesByCategory(category)` | `List<Strategy>` | Get strategies by category |
| `parseStrategies(content)` | `List<Strategy>` | Parse custom strategies from string |
| `createStrategy(command, name, description)` | `Strategy` | Create a single strategy |
| `getDefaultSiteLists()` | `List<SiteList>` | Get all default site lists |
| `getActiveSiteLists()` | `List<SiteList>` | Get active site lists |
| `getSiteList(id)` | `SiteList?` | Get site list by ID |
| `getActiveDomains()` | `List<String>` | Get all domains from active lists |
| `createSiteList(name, domains, isActive)` | `SiteList` | Create a custom site list |
| `testStrategies(...)` | `List<Strategy>` | Test strategies against sites |
| `testWithDefaults(...)` | `List<Strategy>` | Test with default settings |
| `testSingleStrategy(...)` | `Strategy` | Test a single strategy |
| `getBestStrategy(results, minSuccessPercentage)` | `Strategy?` | Get best strategy from results |
| `getWorkingStrategies(results, threshold)` | `List<Strategy>` | Get strategies above threshold |
| `testProxyConnection(timeoutSeconds)` | `Boolean` | Test proxy connectivity |
| `stopTesting()` | `Unit` | Stop current test |
| `replaceSni(command, sniValue)` | `String` | Replace {sni} placeholder |

### ProxyConfig

```kotlin
data class ProxyConfig(
    val ip: String = "127.0.0.1",
    val port: Int = 1080,
    val httpConnect: Boolean = false,
    val maxConnections: Int = 0,
    val bufferSize: Int = 0,
    val desyncUdp: Boolean = false,
    val udpFakeCount: Int = 0,
    val hostsMode: HostsMode = HostsMode.NONE,
    val hosts: String? = null,
    val customArgs: String? = null,
    val useCustomCommand: Boolean = false,
    val resolveDns: Boolean = true,
    val ipv6: Boolean = true,
    val debug: Boolean = false
)
```

### ServerStatus

```kotlin
enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}
```

### Strategy

```kotlin
data class Strategy(
    val command: String,
    val name: String = "",
    val description: String = "",
    var successCount: Int = 0,
    var totalRequests: Int = 0,
    var currentProgress: Int = 0,
    var isCompleted: Boolean = false,
    val siteResults: MutableList<SiteCheckResult> = mutableListOf()
)
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `successPercentage` | `Int` | Success percentage (0-100) |

### SiteList

```kotlin
data class SiteList(
    val id: String,
    val name: String,
    val domains: List<String>,
    val isActive: Boolean = true,
    val isBuiltIn: Boolean = false
)
```

### TestConfig

```kotlin
data class TestConfig(
    val delaySeconds: Int = 1,
    val requestsPerSite: Int = 1,
    val requestTimeoutSeconds: Long = 5,
    val maxConcurrentRequests: Int = 20,
    val sniValue: String = "google.com",
    val fullLog: Boolean = false
)
```

#### Presets

| Preset | Description |
|--------|-------------|
| `TestConfig.DEFAULT` | Default configuration |
| `TestConfig.QUICK` | Quick test (fewer requests, shorter timeout) |
| `TestConfig.THOROUGH` | Thorough test (more requests, longer timeout) |

### StrategyCategory

```kotlin
enum class StrategyCategory {
    SIMPLE,
    COMPLEX,
    SNI_BASED,
    FRAGMENTATION,
    DELAY,
    ALL_CATEGORIES
}
```

## Import/Export

```kotlin
import io.github.romanvht.byedpi.library.io.StrategyExporter

// Export strategies to JSON
val json = StrategyExporter.exportStrategiesToJson(strategies)

// Export strategies to text
val text = StrategyExporter.exportStrategiesToText(strategies)

// Import strategies from JSON
val imported = StrategyExporter.importStrategiesFromJson(json)

// Export to file
StrategyExporter.exportStrategiesToFile(strategies, File("strategies.json"))

// Import from file
val strategies = StrategyExporter.importStrategiesFromFile(File("strategies.json"))

// Export results to CSV
val csv = StrategyExporter.exportResultsToCsv(results)
```

## Default Site Lists

| ID | Name | Domains |
|----|------|---------|
| `youtube` | YouTube | youtu.be, youtube.com, i.ytimg.com, ... |
| `googlevideo` | Google Video | googlevideo.com, *.googlevideo.com, ... |
| `discord` | Discord | discord.com, discordapp.com, ... |
| `cloudflare` | Cloudflare | cloudflare.com, cdnjs.cloudflare.com, ... |
| `social` | Social Media | twitter.com, facebook.com, ... |
| `general` | General Sites | google.com, wikipedia.org, ... |

## Requirements

- Android SDK 21+ (Android 5.0+)
- Kotlin 1.8+
- Gson library (for JSON serialization)

## License

This library is part of the ByeByeDPI Android project and is licensed under the same terms.
