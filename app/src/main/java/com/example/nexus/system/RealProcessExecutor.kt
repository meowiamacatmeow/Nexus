package com.example.nexus.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ProcessTelemetry(
    val command: String,
    val exitCode: Int,
    val executionTimeMs: Long,
    val stdout: String,
    val stderr: String,
    val workingDir: String,
    val isTimeout: Boolean = false
) {
    val isSuccess: Boolean get() = exitCode == 0 && !isTimeout

    fun summaryBadge(): String {
        val statusText = if (isTimeout) "TIMEOUT" else "exit: $exitCode"
        val outLen = stdout.toByteArray().size
        return "[$statusText | time: ${executionTimeMs}ms | out: ${outLen}B | cwd: ${workingDir.substringAfterLast("/")}]"
    }
}

object RealProcessExecutor {

    suspend fun execute(
        command: String,
        workingDir: File,
        timeoutMs: Long = 15_000L,
        extraEnv: Map<String, String> = emptyMap()
    ): ProcessTelemetry {
        val result = CommandExecutor.execute(
            command = command,
            workingDir = workingDir,
            timeoutMs = timeoutMs,
            environment = extraEnv
        )
        return ProcessTelemetry(
            command = result.command,
            exitCode = result.exitCode,
            executionTimeMs = result.durationMs,
            stdout = result.stdout,
            stderr = result.stderr,
            workingDir = result.workingDir,
            isTimeout = result.isTimeout
        )
    }
}
