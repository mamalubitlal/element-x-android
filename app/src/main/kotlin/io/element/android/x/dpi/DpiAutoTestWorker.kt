package io.element.android.x.dpi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.element.android.x.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DpiAutoTestWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "dpi_auto_test"
        const val CHANNEL_ID = "dpi_test_channel"
        const val NOTIFICATION_ID = 1001
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val strategyManager = DpiStrategyManager(context)
            val networkId = strategyManager.getNetworkId()
            
            if (!strategyManager.isStrategyExpired(networkId)) {
                return@withContext Result.success()
            }
            
            val strategies = strategyManager.loadStrategies()
            val domains = strategyManager.loadTestDomains()
            
            if (strategies.isEmpty()) {
                return@withContext Result.failure()
            }
            
            val results = mutableListOf<StrategyTestResult>()
            
            for (strategy in strategies) {
                val result = strategyManager.testStrategy(strategy, domains)
                results.add(result)
                
                if (result.successPercentage >= 90f) {
                    strategyManager.saveStrategyForNetwork(networkId, result.strategy, result.command)
                    strategyManager.saveTestResults(results, networkId)
                    showSuccessNotification(result.strategy)
                    return@withContext Result.success()
                }
            }
            
            val bestResult = results.maxByOrNull { it.successPercentage }
            if (bestResult != null && bestResult.successPercentage > 0) {
                strategyManager.saveStrategyForNetwork(networkId, bestResult.strategy, bestResult.command)
                strategyManager.saveTestResults(results, networkId)
                showPartialSuccessNotification(bestResult.successPercentage)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private fun showSuccessNotification(strategyName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DPI Bypass Configured")
            .setContentText("Best strategy: $strategyName")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showPartialSuccessNotification(percentage: Float) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DPI Test Complete")
            .setContentText("Best success rate: ${percentage.toInt()}%")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DPI Testing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "DPI bypass strategy testing notifications"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
