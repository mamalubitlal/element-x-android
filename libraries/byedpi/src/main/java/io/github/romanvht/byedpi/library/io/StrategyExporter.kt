package io.github.romanvht.byedpi.library.io

import io.github.romanvht.byedpi.library.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Utility class for exporting and importing strategies and site lists
 */
object StrategyExporter {
    
    private val gson = Gson()
    
    // ==================== Strategy Export/Import ====================
    
    /**
     * Export strategies to a JSON string
     */
    fun exportStrategiesToJson(strategies: List<Strategy>): String {
        val exportData = strategies.map { strategy ->
            mapOf(
                "command" to strategy.command,
                "name" to strategy.name,
                "description" to strategy.description
            )
        }
        return gson.toJson(exportData)
    }
    
    /**
     * Export strategies to a simple text format (one per line)
     */
    fun exportStrategiesToText(strategies: List<Strategy>): String {
        return strategies.joinToString("\n") { strategy ->
            if (strategy.name.isNotEmpty()) {
                "# ${strategy.name}\n${strategy.command}"
            } else {
                strategy.command
            }
        }
    }
    
    /**
     * Import strategies from a JSON string
     */
    fun importStrategiesFromJson(json: String): List<Strategy> {
        return try {
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val data: List<Map<String, String>> = gson.fromJson(json, type)
            data.map { map ->
                Strategy(
                    command = map["command"] ?: "",
                    name = map["name"] ?: "",
                    description = map["description"] ?: ""
                )
            }.filter { it.command.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Import strategies from a text file (one per line)
     * Lines starting with # are treated as comments or strategy names
     */
    fun importStrategiesFromText(text: String): List<Strategy> {
        val strategies = mutableListOf<Strategy>()
        var currentName = ""
        
        for (line in text.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> continue
                trimmed.startsWith("#") -> {
                    currentName = trimmed.removePrefix("#").trim()
                }
                else -> {
                    strategies.add(
                        Strategy(
                            command = trimmed,
                            name = currentName,
                            description = ""
                        )
                    )
                    currentName = ""
                }
            }
        }
        
        return strategies
    }
    
    /**
     * Export strategies to a file
     */
    fun exportStrategiesToFile(
        strategies: List<Strategy>,
        file: File,
        format: ExportFormat = ExportFormat.JSON
    ) {
        val content = when (format) {
            ExportFormat.JSON -> exportStrategiesToJson(strategies)
            ExportFormat.TEXT -> exportStrategiesToText(strategies)
        }
        file.writeText(content)
    }
    
    /**
     * Import strategies from a file
     */
    fun importStrategiesFromFile(file: File): List<Strategy> {
        if (!file.exists()) return emptyList()
        val content = file.readText()
        
        return when {
            file.extension.equals("json", ignoreCase = true) -> importStrategiesFromJson(content)
            else -> importStrategiesFromText(content)
        }
    }
    
    // ==================== Site List Export/Import ====================
    
    /**
     * Export site lists to a JSON string
     */
    fun exportSiteListsToJson(lists: List<SiteList>): String {
        val exportData = lists.map { list ->
            mapOf(
                "id" to list.id,
                "name" to list.name,
                "domains" to list.domains,
                "isActive" to list.isActive
            )
        }
        return gson.toJson(exportData)
    }
    
    /**
     * Export site lists to a simple text format
     * Each list is separated by "---" header
     */
    fun exportSiteListsToText(lists: List<SiteList>): String {
        return lists.joinToString("\n\n") { list ->
            buildString {
                appendLine("# ${list.name}")
                appendLine("# ID: ${list.id}")
                appendLine("# Active: ${list.isActive}")
                list.domains.forEach { domain ->
                    appendLine(domain)
                }
            }
        }
    }
    
    /**
     * Import site lists from a JSON string
     */
    fun importSiteListsFromJson(json: String): List<SiteList> {
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val data: List<Map<String, Any>> = gson.fromJson(json, type)
            data.map { map ->
                val domainsType = object : TypeToken<List<String>>() {}.type
                val domains: List<String> = gson.fromJson(
                    gson.toJson(map["domains"]), 
                    domainsType
                )
                SiteList(
                    id = map["id"] as? String ?: "",
                    name = map["name"] as? String ?: "",
                    domains = domains,
                    isActive = (map["isActive"] as? Boolean) ?: true,
                    isBuiltIn = false
                )
            }.filter { it.id.isNotEmpty() && it.domains.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Import site lists from a text file
     */
    fun importSiteListsFromText(text: String): List<SiteList> {
        val lists = mutableListOf<SiteList>()
        var currentName = ""
        var currentId = ""
        var isActive = true
        val currentDomains = mutableListOf<String>()
        
        fun saveCurrent() {
            if (currentName.isNotEmpty() && currentDomains.isNotEmpty()) {
                lists.add(
                    SiteList(
                        id = currentId.ifEmpty { currentName.lowercase().replace(" ", "_") },
                        name = currentName,
                        domains = currentDomains.toList(),
                        isActive = isActive,
                        isBuiltIn = false
                    )
                )
            }
            currentName = ""
            currentId = ""
            isActive = true
            currentDomains.clear()
        }
        
        for (line in text.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> continue
                trimmed.startsWith("---") -> saveCurrent()
                trimmed.startsWith("#") -> {
                    val content = trimmed.removePrefix("#").trim()
                    when {
                        content.startsWith("ID:", ignoreCase = true) -> {
                            currentId = content.removePrefix("ID:").trim().lowercase()
                        }
                        content.startsWith("Active:", ignoreCase = true) -> {
                            isActive = content.removePrefix("Active:").trim()
                                .equals("true", ignoreCase = true)
                        }
                        else -> {
                            currentName = content
                        }
                    }
                }
                else -> currentDomains.add(trimmed)
            }
        }
        
        saveCurrent()
        return lists
    }
    
    /**
     * Export site lists to a file
     */
    fun exportSiteListsToFile(
        lists: List<SiteList>,
        file: File,
        format: ExportFormat = ExportFormat.JSON
    ) {
        val content = when (format) {
            ExportFormat.JSON -> exportSiteListsToJson(lists)
            ExportFormat.TEXT -> exportSiteListsToText(lists)
        }
        file.writeText(content)
    }
    
    /**
     * Import site lists from a file
     */
    fun importSiteListsFromFile(file: File): List<SiteList> {
        if (!file.exists()) return emptyList()
        val content = file.readText()
        
        return when {
            file.extension.equals("json", ignoreCase = true) -> importSiteListsFromJson(content)
            else -> importSiteListsFromText(content)
        }
    }
    
    // ==================== Results Export ====================
    
    /**
     * Export test results to a JSON string
     */
    fun exportResultsToJson(results: List<Strategy>): String {
        val exportData = results.map { strategy ->
            mapOf(
                "command" to strategy.command,
                "name" to strategy.name,
                "successCount" to strategy.successCount,
                "totalRequests" to strategy.totalRequests,
                "successPercentage" to strategy.successPercentage,
                "siteResults" to strategy.siteResults.map { siteResult ->
                    mapOf(
                        "site" to siteResult.site,
                        "successCount" to siteResult.successCount,
                        "totalCount" to siteResult.totalCount,
                        "successPercentage" to siteResult.successPercentage
                    )
                }
            )
        }
        return gson.toJson(exportData)
    }
    
    /**
     * Export results to CSV format
     */
    fun exportResultsToCsv(results: List<Strategy>): String {
        val header = "Strategy,Success %,Success Count,Total Requests,Command"
        val rows = results.map { strategy ->
            "\"${strategy.name}\",${strategy.successPercentage},${strategy.successCount},${strategy.totalRequests},\"${strategy.command}\""
        }
        return (listOf(header) + rows).joinToString("\n")
    }
}

/**
 * Export format options
 */
enum class ExportFormat {
    JSON,
    TEXT
}
