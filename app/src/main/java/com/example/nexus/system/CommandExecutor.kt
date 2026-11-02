package com.example.nexus.system

import com.example.nexus.model.TerminalEntry
import com.example.nexus.model.TerminalKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Events emitted during live command streaming.
 */
sealed class ProcessStreamEvent {
    data class StdoutLine(val line: String) : ProcessStreamEvent()
    data class StderrLine(val line: String) : ProcessStreamEvent()
    data class Completed(val result: CommandResult) : ProcessStreamEvent()
}

/**
 * Encapsulates the execution result of a shell process.
 * Captures standard output, standard error, process exit code, and execution duration.
 */
data class CommandResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
    val isTimeout: Boolean = false,
    val workingDir: String = ""
) {
    val isSuccess: Boolean
        get() = exitCode == 0 && !isTimeout

    val hasError: Boolean
        get() = exitCode != 0 || stderr.isNotBlank() || isTimeout

    /**
     * Formats duration into a human-readable string (e.g. 14ms or 1.25s).
     */
    fun formattedDuration(): String {
        return if (durationMs < 1000) {
            "${durationMs}ms"
        } else {
            String.format("%.2fs", durationMs / 1000.0)
        }
    }

    /**
     * Concise telemetry badge for terminal status lines.
     */
    fun summaryBadge(): String {
        val status = when {
            isTimeout -> "TIMEOUT"
            exitCode == 0 -> "exit: 0"
            else -> "exit: $exitCode"
        }
        val outBytes = stdout.toByteArray().size
        val errBytes = stderr.toByteArray().size
        val dirName = if (workingDir.isNotBlank()) " | cwd: ${workingDir.substringAfterLast("/")}" else ""
        return "[$status | time: ${formattedDuration()} | out: ${outBytes}B | err: ${errBytes}B$dirName]"
    }

    /**
     * Converts stdout, stderr, and execution metadata into rich TerminalEntries
     * for seamless display in the NEXUS Terminal UI.
     */
    fun toTerminalEntries(): List<TerminalEntry> {
        val entries = mutableListOf<TerminalEntry>()

        if (stdout.isNotBlank()) {
            stdout.lines().forEach { line ->
                entries.add(TerminalEntry(UUID.randomUUID().toString(), TerminalKind.OUT, line))
            }
        }

        if (stderr.isNotBlank()) {
            stderr.lines().forEach { line ->
                entries.add(TerminalEntry(UUID.randomUUID().toString(), TerminalKind.ERR, line))
            }
        }

        if (entries.isEmpty() && isSuccess) {
            entries.add(TerminalEntry(UUID.randomUUID().toString(), TerminalKind.OUT, ""))
        }

        val badgeKind = if (isSuccess) TerminalKind.META else TerminalKind.WARN
        entries.add(TerminalEntry(UUID.randomUUID().toString(), badgeKind, summaryBadge()))

        return entries
    }

    /**
     * Single string representation formatted for plain-text terminal output.
     */
    fun formatTerminalDisplay(): String = buildString {
        if (stdout.isNotBlank()) {
            append(stdout)
        }
        if (stderr.isNotBlank()) {
            if (isNotEmpty()) append("\n")
            append("[STDERR]\n").append(stderr)
        }
        if (isNotEmpty()) append("\n")
        append(summaryBadge())
    }
}

/**
 * CommandExecutor executes live system shell processes using Java ProcessBuilder.
 *
 * Features:
 * - Full asynchronous execution on Dispatchers.IO.
 * - Concurrent stream consumption of stdout and stderr to prevent OS buffer deadlocks.
 * - Execution duration measurement in milliseconds.
 * - Dynamic shell resolution: detects Android `/system/bin/sh` or host `/bin/sh` / `sh`.
 * - Robust timeout enforcement and forced process cleanup.
 * - Environment variable injection and default working directory support.
 */
class CommandExecutor(
    val defaultWorkingDir: File? = null
) {

    /**
     * Asynchronously executes a shell command, capturing stdout, stderr, exit code, and duration.
     *
     * @param command The shell command string to execute.
     * @param workingDir Directory where the command will run. Defaults to defaultWorkingDir or current dir.
     * @param timeoutMs Maximum allowable execution time before the process is forcibly killed.
     * @param environment Optional map of environment variables to inject.
     */
    suspend fun execute(
        command: String,
        workingDir: File? = defaultWorkingDir,
        timeoutMs: Long = 15_000L,
        environment: Map<String, String> = emptyMap()
    ): CommandResult = withContext(Dispatchers.IO) {
        val targetDir = workingDir ?: File(".")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val startTime = System.currentTimeMillis()
        var process: Process? = null

        try {
            val shellPath = resolveShellPath()
            val pb = ProcessBuilder(shellPath, "-c", command)
            pb.directory(targetDir)

            val pbEnv = pb.environment()
            val systemPath = System.getenv("PATH") ?: "/system/bin:/system/xbin:/usr/bin:/bin"
            pbEnv["PATH"] = "/system/bin:/system/xbin:$systemPath"
            pbEnv["HOME"] = targetDir.absolutePath
            pbEnv["TMPDIR"] = targetDir.absolutePath
            pbEnv["TERM"] = "xterm-256color"
            pbEnv["USER"] = "nexus"
            pbEnv.putAll(environment)

            val proc = pb.start()
            process = proc

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            // Concurrently consume stdout to avoid buffer deadlocks on large process outputs
            val stdoutThread = Thread {
                try {
                    BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (stdoutBuilder.isNotEmpty()) stdoutBuilder.append("\n")
                            stdoutBuilder.append(line)
                        }
                    }
                } catch (_: Exception) {}
            }.apply { isDaemon = true; start() }

            // Concurrently consume stderr
            val stderrThread = Thread {
                try {
                    BufferedReader(InputStreamReader(proc.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (stderrBuilder.isNotEmpty()) stderrBuilder.append("\n")
                            stderrBuilder.append(line)
                        }
                    }
                } catch (_: Exception) {}
            }.apply { isDaemon = true; start() }

            val completed = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            val duration = System.currentTimeMillis() - startTime

            if (!completed) {
                proc.destroyForcibly()
                stdoutThread.interrupt()
                stderrThread.interrupt()
                return@withContext CommandResult(
                    command = command,
                    stdout = stdoutBuilder.toString(),
                    stderr = "Process timed out after ${timeoutMs}ms and was forcibly terminated.",
                    exitCode = 124, // Standard POSIX timeout exit code
                    durationMs = duration,
                    isTimeout = true,
                    workingDir = targetDir.name
                )
            }

            stdoutThread.join(1000)
            stderrThread.join(1000)

            val exitCode = proc.exitValue()
            CommandResult(
                command = command,
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                exitCode = exitCode,
                durationMs = duration,
                isTimeout = false,
                workingDir = targetDir.name
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            CommandResult(
                command = command,
                stdout = "",
                stderr = "Command execution failed: ${e.message ?: e.javaClass.simpleName}",
                exitCode = 127,
                durationMs = duration,
                isTimeout = false,
                workingDir = targetDir.name
            )
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }

    /**
     * Executes a command with live streaming of stdout and stderr lines as they are produced.
     * Cancelling the collector will cleanly destroy the underlying process.
     */
    fun executeStream(
        command: String,
        workingDir: File? = defaultWorkingDir,
        timeoutMs: Long = 60_000L,
        environment: Map<String, String> = emptyMap()
    ): Flow<ProcessStreamEvent> = channelFlow {
        val targetDir = workingDir ?: File(".")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val startTime = System.currentTimeMillis()
        val shellPath = resolveShellPath()
        val pb = ProcessBuilder(shellPath, "-c", command)
        pb.directory(targetDir)

        val pbEnv = pb.environment()
        val systemPath = System.getenv("PATH") ?: "/system/bin:/system/xbin:/usr/bin:/bin"
        pbEnv["PATH"] = "/system/bin:/system/xbin:$systemPath"
        pbEnv["HOME"] = targetDir.absolutePath
        pbEnv["TMPDIR"] = targetDir.absolutePath
        pbEnv["TERM"] = "xterm-256color"
        pbEnv["USER"] = "nexus"
        pbEnv.putAll(environment)

        val proc = pb.start()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        val stdoutJob = launch(Dispatchers.IO) {
            try {
                BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        if (stdoutBuilder.isNotEmpty()) stdoutBuilder.append("\n")
                        stdoutBuilder.append(l)
                        send(ProcessStreamEvent.StdoutLine(l))
                    }
                }
            } catch (_: Exception) {}
        }

        val stderrJob = launch(Dispatchers.IO) {
            try {
                BufferedReader(InputStreamReader(proc.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        if (stderrBuilder.isNotEmpty()) stderrBuilder.append("\n")
                        stderrBuilder.append(l)
                        send(ProcessStreamEvent.StderrLine(l))
                    }
                }
            } catch (_: Exception) {}
        }

        launch(Dispatchers.IO) {
            var isTimedOut = false
            val completed = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            val duration = System.currentTimeMillis() - startTime

            if (!completed) {
                isTimedOut = true
                proc.destroyForcibly()
                send(ProcessStreamEvent.StderrLine("Process timed out after ${timeoutMs}ms and was forcibly terminated."))
            }

            stdoutJob.join()
            stderrJob.join()

            val exitCode = if (isTimedOut) 124 else proc.exitValue()
            val result = CommandResult(
                command = command,
                stdout = stdoutBuilder.toString(),
                stderr = if (isTimedOut) "Process timed out after ${timeoutMs}ms." else stderrBuilder.toString(),
                exitCode = exitCode,
                durationMs = duration,
                isTimeout = isTimedOut,
                workingDir = targetDir.name
            )
            send(ProcessStreamEvent.Completed(result))
            close()
        }

        awaitClose {
            try {
                proc.destroyForcibly()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Resolves the best available shell on the current platform.
     * Prefers `/system/bin/sh` on Android devices, falling back to `/bin/sh` or `/usr/bin/sh` on Linux/host test runners.
     */
    fun resolveShellPath(): String {
        return when {
            File("/system/bin/sh").exists() -> "/system/bin/sh"
            File("/bin/sh").exists() -> "/bin/sh"
            File("/usr/bin/sh").exists() -> "/usr/bin/sh"
            else -> "sh"
        }
    }

    companion object {
        val default = CommandExecutor()

        suspend fun execute(
            command: String,
            workingDir: File? = null,
            timeoutMs: Long = 15_000L,
            environment: Map<String, String> = emptyMap()
        ): CommandResult {
            return default.execute(command, workingDir, timeoutMs, environment)
        }

        fun executeStream(
            command: String,
            workingDir: File? = null,
            timeoutMs: Long = 60_000L,
            environment: Map<String, String> = emptyMap()
        ): Flow<ProcessStreamEvent> {
            return default.executeStream(command, workingDir, timeoutMs, environment)
        }
    }
}
