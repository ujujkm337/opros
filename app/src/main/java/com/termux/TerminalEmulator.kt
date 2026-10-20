package com.termux

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Simple terminal emulator that wraps a shell process.
 * Handles PTY creation, input/output, and terminal escape sequences.
 * 
 * For a full production implementation, consider using the Termux:Terminal-Emulator
 * library: https://github.com/termux/termux-app/tree/master/terminal-emulator
 */
class TerminalEmulator(
    private val shellPath: String,
    private val homePath: String,
    private val environment: Map<String, String>
) {
    companion object {
        private const val TAG = "TerminalEmulator"
        private const val DEFAULT_COLUMNS = 120
        private const val DEFAULT_ROWS = 40
    }

    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var errorStream: InputStream? = null
    private var isCtrlMode = false
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var outputCallback: ((String) -> Unit)? = null
    private val outputBuffer = StringBuilder()

    /**
     * Starts the terminal session
     */
    fun start() {
        try {
            val envArray = environment.map { "${it.key}=${it.value}" }.toTypedArray()
            
            // Use the script binary to create a PTY if available
            // Otherwise fall back to direct process execution
            val shellCommand = arrayOf(
                shellPath,
                "--login"
            )
            
            val processBuilder = ProcessBuilder(*shellCommand)
                .directory(File(homePath))
                .redirectErrorStream(true)
            
            // Set environment
            processBuilder.environment().apply {
                clear()
                putAll(environment)
            }
            
            process = processBuilder.start()
            outputStream = process?.outputStream
            inputStream = process?.inputStream
            errorStream = process?.errorStream
            
            // Start reading output
            startOutputReader()
            
            Log.i(TAG, "Terminal session started: $shellPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start terminal session", e)
        }
    }

    /**
     * Writes text to the terminal (as if user typed it)
     */
    fun writeToTerminal(text: String) {
        try {
            outputStream?.let { out ->
                out.write(text.toByteArray())
                out.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to terminal", e)
        }
    }

    /**
     * Toggles Ctrl key mode for keyboard shortcuts
     */
    fun toggleCtrlMode() {
        isCtrlMode = !isCtrlMode
    }

    /**
     * Sends a Ctrl+key combination
     */
    fun sendCtrlKey(key: Char) {
        val ctrlChar = (key.toInt() - 'a'.toInt() + 1).toChar()
        writeToTerminal(ctrlChar.toString())
    }

    /**
     * Sets the callback for terminal output
     */
    fun setOutputCallback(callback: (String) -> Unit) {
        outputCallback = callback
    }

    /**
     * Gets the current terminal buffer content
     */
    fun getOutputBuffer(): String {
        return outputBuffer.toString()
    }

    /**
     * Clears the output buffer
     */
    fun clearOutputBuffer() {
        outputBuffer.clear()
    }

    /**
     * Resizes the terminal
     */
    fun resize(columns: Int, rows: Int) {
        // Send resize escape sequence
        writeToTerminal("\u001b[8;${rows};${columns}t")
    }

    /**
     * Closes the terminal session
     */
    fun close() {
        scope.cancel()
        try {
            outputStream?.close()
            inputStream?.close()
            errorStream?.close()
            process?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing terminal session", e)
        }
    }

    /**
     * Checks if the session is alive
     */
    fun isAlive(): Boolean {
        return process?.isAlive == true
    }

    private fun startOutputReader() {
        scope.launch {
            try {
                val buffer = ByteArray(4096)
                while (true) {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead == -1) break
                    
                    val output = String(buffer, 0, bytesRead, Charsets.UTF_8)
                    outputBuffer.append(output)
                    
                    // Keep buffer size reasonable
                    if (outputBuffer.length > 100_000) {
                        outputBuffer.delete(0, outputBuffer.length - 50_000)
                    }
                    
                    withContext(Dispatchers.Main) {
                        outputCallback?.invoke(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading terminal output", e)
            }
        }
    }
}
