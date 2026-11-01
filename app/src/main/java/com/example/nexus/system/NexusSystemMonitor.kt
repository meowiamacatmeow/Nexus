package com.example.nexus.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.example.nexus.model.SystemInfo

class NexusSystemMonitor(private val context: Context) {

    fun getSystemInfo(): SystemInfo {
        // Memory
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val ramTotalMb = memInfo.totalMem / (1024 * 1024)
        val ramAvailMb = memInfo.availMem / (1024 * 1024)
        val ramUsedMb = ramTotalMb - ramAvailMb
        val ramPercent = if (ramTotalMb > 0) ((ramUsedMb.toDouble() / ramTotalMb) * 100).toInt() else 0

        // Storage
        val filesDir = context.filesDir
        val freeBytes = filesDir.freeSpace
        val totalBytes = filesDir.totalSpace
        val storageFreeGb = freeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val storageTotalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

        // Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val cpuCores = Runtime.getRuntime().availableProcessors()
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        return SystemInfo(
            ramFreeMb = ramAvailMb,
            ramTotalMb = ramTotalMb,
            ramPercent = ramPercent,
            storageFreeGb = storageFreeGb,
            storageTotalGb = storageTotalGb,
            batteryLevel = batteryPct,
            isCharging = isCharging,
            cpuCores = cpuCores,
            abi = abi
        )
    }
}
