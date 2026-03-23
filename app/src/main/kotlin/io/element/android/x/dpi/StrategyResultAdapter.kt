package io.element.android.x.dpi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.element.android.x.R

class StrategyResultAdapter : RecyclerView.Adapter<StrategyResultAdapter.ViewHolder>() {
    
    private val results = mutableListOf<StrategyTestResult>()
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val strategyName: TextView = view.findViewById(R.id.strategyName)
        val successPercentage: TextView = view.findViewById(R.id.successPercentage)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val domainsText: TextView = view.findViewById(R.id.domainsText)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_strategy_result, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        
        holder.strategyName.text = result.strategy
        holder.successPercentage.text = "${result.successPercentage.toInt()}%"
        holder.progressBar.progress = result.successPercentage.toInt()
        holder.domainsText.text = result.domains.entries
            .sortedByDescending { it.value.successPercentage }
            .take(5)
            .joinToString("\n") { (domain, domainResult) ->
                "$domain: ${domainResult.successPercentage.toInt()}%"
            }
    }
    
    override fun getItemCount() = results.size
    
    fun addResult(result: StrategyTestResult) {
        results.add(result)
        notifyItemInserted(results.size - 1)
    }
    
    fun updateResults(newResults: List<StrategyTestResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
}
