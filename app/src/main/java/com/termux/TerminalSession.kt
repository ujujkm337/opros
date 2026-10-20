package com.termux

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Терминальная сессия — обёртка над shell-процессом Termux.
 */
class TerminalSession(
    private val shellPath: String,
    private val homePath: String,
    private val env: Map<String, String>
) {
    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var outputCallback: ((String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isCtrlMode = false

    fun start() {
        try {
            val pb = ProcessBuilder(shellPath, "--login")
                .directory(File(homePath))
                .redirectErrorStream(true)

            pb.environment().apply {
                clear()
                putAll(env)
            }

            process = pb.start()
            outputStream = process?.outputStream
            inputStream = process?.inputStream

            startOutputReader()
            Log.i("TerminalSession", "Session started: $shellPath")
        } catch (e: Exception) {
            Log.e("TerminalSession", "Failed to start", e)
        }
    }

    fun write(text: String) {
        try {
            outputStream?.let {
                it.write(text.toByteArray())
                it.flush()
            }
        } catch (e: Exception) {
            Log.e("TerminalSession", "Write error", e)
        }
    }

    fun toggleCtrl() {
        isCtrlMode = !isCtrlMode
    }

    fun setOutputCallback(callback: (String) -> Unit) {
        outputCallback = callback
    }

    fun isAlive(): Boolean = process?.isAlive == true

    fun destroy() {
        scope.cancel()
        try {
            outputStream?.close()
            inputStream?.close()
            process?.destroy()
        } catch (_: Exception) {}
    }

    private fun startOutputReader() {
        scope.launch {
            try {
                val buffer = ByteArray(4096)
                while (true) {
                    val read = inputStream?.read(buffer) ?: -1
                    if (read == -1) break
                    val output = String(buffer, 0, read, Charsets.UTF_8)
                    withContext(Dispatchers.Main) {
                        outputCallback?.invoke(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("TerminalSession", "Reader error", e)
            }
        }
    }
}
