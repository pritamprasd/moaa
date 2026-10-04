package dev.pritam.host.optimizer
 
import android.content.Context
import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel
import dev.pritam.host.tool.sensors.manager.SensorsManager
import java.util.Locale

/**
 * Global System Performance & Memory Optimizer.
 *
 * When triggered:
 * 1. Suspends any active/idle hardware sensor streaming listeners and their coroutine timers.
 * 2. Prunes old in-memory telemetry and trace logs in [AppLogHub] down to a lean buffer (last 50 items).
 * 3. Triggers garbage collection to compact heap and free reclaimed memory.
 * 4. Logs a diagnostic report to [AppLogHub].
 */
object AppPerformanceOptimizer {

    data class OptimizationSummary(
        val sensorsPausedCount: Int,
        val logsPrunedCount: Int,
        val memoryReclaimedMb: Double,
        val cacheClearedBytes: Long
    )

    fun boostPerformance(context: Context): OptimizationSummary {
        val runtime = Runtime.getRuntime()
        val memBefore = runtime.totalMemory() - runtime.freeMemory()

        // 1. Suspend background hardware sensor polling
        val sensorsPaused = SensorsManager.pauseActiveStreaming()

        // 2. Prune log viewer buffer down to 50 entries
        val logsPruned = AppLogHub.pruneDownTo(50)

        // 3. Purge transient files in application cache directory
        var cacheCleared = 0L
        try {
            val cacheFiles = context.cacheDir.listFiles() ?: emptyArray()
            for (f in cacheFiles) {
                if (f.isFile) {
                    val len = f.length()
                    if (f.delete()) cacheCleared += len
                }
            }
        } catch (_: Throwable) {}

        // 4. Compact and reclaim JVM heap memory
        System.gc()

        val memAfter = runtime.totalMemory() - runtime.freeMemory()
        val reclaimedMb = ((memBefore - memAfter).toDouble() / (1024.0 * 1024.0)).coerceAtLeast(0.0)

        AppLogHub.log(
            toolId = "system-optimizer",
            toolName = "System Optimizer",
            level = LogLevel.INFO,
            tag = "Boost",
            message = "SYSTEM OPTIMIZER [BOOST] Reclaimed ~${String.format(Locale.US, "%.1f", reclaimedMb)} MB heap RAM, suspended $sensorsPaused sensors, pruned $logsPruned diagnostic logs, cleared ${cacheCleared / 1024} KB cache."
        )

        return OptimizationSummary(
            sensorsPausedCount = sensorsPaused,
            logsPrunedCount = logsPruned,
            memoryReclaimedMb = reclaimedMb,
            cacheClearedBytes = cacheCleared
        )
    }
}
