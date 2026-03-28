package io.element.android.x.dpi

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.element.android.x.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MatrixTestActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var adapter: StrategyResultAdapter
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val strategyManager by lazy { DpiStrategyManager(this) }
    
    private var totalStrategies = 0
    private var testedStrategies = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matrix_test)
        
        recyclerView = findViewById(R.id.resultsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        
        adapter = StrategyResultAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        startTesting()
    }
    
    private fun startTesting() {
        val strategies = strategyManager.loadStrategies()
        val domains = strategyManager.loadTestDomains()
        
        totalStrategies = strategies.size
        testedStrategies = 0
        
        statusText.text = "Testing 0 of $totalStrategies strategies..."
        
        scope.launch {
            val results = mutableListOf<StrategyTestResult>()
            
            for (strategy in strategies) {
                testedStrategies++
                updateProgress()
                
                val result = strategyManager.testStrategy(strategy, domains)
                results.add(result)
                
                withContext(Dispatchers.Main) {
                    adapter.addResult(result)
                }
            }
            
            val sortedResults = results.sortedByDescending { it.successPercentage }
            adapter.updateResults(sortedResults)
            
            val bestResult = sortedResults.firstOrNull()
            if (bestResult != null) {
                val networkId = strategyManager.getNetworkId()
                strategyManager.saveStrategyForNetwork(networkId, bestResult.strategy, bestResult.command)
                strategyManager.saveTestResults(sortedResults, networkId)
            }
            
            statusText.text = "Testing complete! Best: ${bestResult?.strategy ?: "None"} (${bestResult?.successPercentage?.toInt()}%)"
            progressBar.progress = 100
        }
    }
    
    private fun updateProgress() {
        val progress = (testedStrategies * 100) / totalStrategies
        progressBar.progress = progress
        statusText.text = "Testing $testedStrategies of $totalStrategies strategies..."
    }
}
