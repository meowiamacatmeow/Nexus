# Implementation Plan - Real Process Execution & Live Online AI

Upgrade PROJECT NEXUS from simulated/mocked command feedback to genuine on-device execution using Android's `/system/bin/sh` and `ProcessBuilder`, real network socket port scanning and pinging, verifiable process telemetry (real PID, execution time, exit codes, and output byte counters), and zero-configuration online Gemini AI connectivity using the server-side API configuration without asking the user for API keys.

## User Review Required

> [!IMPORTANT]
> - **Process Execution Model**: All terminal and task commands will execute through Android's real shell (`/system/bin/sh -c ...`) with working directory rooted in the app's verified internal workspaces storage.
> - **Online AI Connectivity**: Online AI will connect to Google Gemini using the project's injected `BuildConfig.GEMINI_API_KEY` (configured via AI Studio's server-side environment secrets in `.env.example`). No API key prompt or input will be presented to the user.
> - **Real Network Telemetry**: Network scans and checks (`nmap`, `ping`, host analysis) will perform actual DNS lookups (`InetAddress`), live TCP socket connections on target ports, and real latency measurements rather than static mock templates.

---

## Proposed Changes

### 1. Online AI Architecture (`com.example.nexus.ai`)

#### `.env.example` & Secrets Plugin
- Uncomment `GEMINI_API_KEY=MY_GEMINI_API_KEY` in `.env.example` so AI Studio injects the server-side Gemini API key into `BuildConfig.GEMINI_API_KEY` at compile and run time.

#### `GeminiDirectRestProvider.kt`
- Create a direct, robust Gemini REST client using existing `OkHttp` and `Retrofit` dependencies.
- Calls `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}`.
- Includes conversation history context, active agent role/boundaries, and fallback to local deterministic mode if offline or if network is unavailable.
- Provides real connection status, round-trip latency, and token response metrics.

#### `NexusAIProvider.kt` & `SettingsScreen.kt`
- Wire `GeminiDirectRestProvider` as the primary online AI engine.
- Update AI status indicator across Dashboard, Chat, and Settings to display live connectivity state ("Online - Gemini 2.5 Flash Connected" / "Offline Fallback").
- Add a "Test Online AI Connection" button in Settings that performs a live handshake test and displays actual server latency without asking for keys.

---

### 2. Real OS Process Execution Engine (`com.example.nexus.system`)

#### `RealProcessExecutor.kt`
- Implement a dedicated OS process execution engine:
  - Invokes `java.lang.ProcessBuilder("/system/bin/sh", "-c", command)`.
  - Sets the working directory to the current active workspace directory (`context.filesDir/workspaces/...`).
  - Sets environment variables (`HOME`, `PATH=/system/bin:/system/xbin:$PATH`, `TMPDIR`, `NEXUS_WORKSPACE`).
  - Implements execution timeouts (configurable, default 15s) with thread-safe process destruction on cancel.
  - Captures genuine standard output (`stdout`) and standard error (`stderr`) streams.
  - Returns `ProcessTelemetry`: real exit code (e.g. `0`, `1`, `127`), execution duration in milliseconds, stdout bytes, stderr bytes, and timestamp.

#### `RealNetworkProbe.kt`
- Implement real network testing for commands like `nmap` and `ping`:
  - Performs live DNS resolution with `InetAddress.getAllByName(host)`.
  - Executes live TCP socket probes on requested ports (e.g., 22, 80, 443, 8080) with connection timeouts and microsecond latency measurement.
  - Executes `/system/bin/ping -c 3` via `ProcessBuilder` if ICMP is available, capturing real packet transmission and round-trip times.

---

### 3. Terminal & Task Engine Real Execution Integration

#### `NexusTerminalEngine.kt`
- Route shell commands directly to `RealProcessExecutor`:
  - Commands like `ls`, `cat`, `touch`, `mkdir`, `echo`, `date`, `uname`, `id`, `ps`, `df`, `find`, `grep`, and arbitrary shell pipelines execute in the real workspace directory.
  - Changes to files via commands (`echo "data" > file.txt`, `mkdir test`) immediately reflect on real storage and in the File Manager.
  - Display verifiable execution telemetry metadata for every command:
    `[exit: 0 | time: 14ms | out: 242 B | cwd: /workspaces]`
- Route `nmap` and network commands to `RealNetworkProbe` to produce actual live scanning output with real target IPs and live port states.
- For C++ and Python: run real `/system/bin/sh` compiler/runtime checks. If on-device compiler is not bundled by Android OS, report real exit code (`which: not found`) and provide real bytecode/interpreted execution with verified disk artifacts.

#### `NexusTaskEngine.kt`
- Execute approved task steps with real operations:
  - File/folder creation and writes occur on real disk with hash and size verification.
  - Network tasks perform live DNS and socket requests.
  - Real execution logs, durations, and exit statuses recorded in Room database.

---

### 4. UI Verifiable Telemetry Display (`com.example.nexus.ui`)

#### `TerminalScreen.kt`
- Add a visual "Telemetry Drawer / Badge" under terminal entries displaying real process exit codes, execution duration, and process output stats.
- Provide a toggle to view raw process stderr/stdout.

#### `DashboardScreen.kt` & `ChatScreen.kt`
- Show real online AI connectivity badge (live ping time to Gemini API).
- Show real storage and real process count.

---

## Verification Plan

### Automated Tests
- Run `gradle :app:testDebugUnitTest`:
  - Verify Room database operations and DAOs remain 100% passing.
  - Add test cases in `ExampleRobolectricTest.kt` verifying:
    - `RealProcessExecutor` executes standard commands (e.g. `echo "NEXUS"`, `pwd`, file creation) and produces valid exit codes, stdout, and telemetry.
    - `RealNetworkProbe` resolves known hostnames and handles timeout/connection errors gracefully.
    - AI provider fallback logic works when offline and switches to online when configured.

### Manual Verification
- In Terminal:
  - Run `echo "hello world" > greeting.txt` -> verify `greeting.txt` is created on disk and inspectable in File Manager.
  - Run `cat greeting.txt` -> verify content matches.
  - Run `date`, `uname -a`, `id`, `ls -la` -> verify real Linux process output and exit code `0`.
  - Run `nmap scanme.nmap.org` -> verify real DNS resolution to `45.33.32.156` and live port socket results with latency.
- In Chat & Settings:
  - Verify AI Status displays "Online (Gemini 2.5 Flash)" with live status.
  - Send message in Chat -> verify real online AI response without any API key prompts.
