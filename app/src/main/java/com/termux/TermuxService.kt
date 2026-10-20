package com.termux

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Сервис для управления Termux-сессиями.
 */
class TermuxService : Service() {

    private val binder = LocalBinder()
    private var session: TerminalSession? = null

    companion object {
        const val FILES_PATH = "/data/data/com.termux/files"
        const val USR_PATH = "$FILES_PATH/usr"
        const val HOME_PATH = "$FILES_PATH/home"
    }

    inner class LocalBinder : Binder() {
        fun getService(): TermuxService = this@TermuxService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
    }

    fun isBootstrapReady(): Boolean {
        return BootstrapManager.isInstalled(this)
    }

    fun getOrCreateSession(): TerminalSession {
        if (session == null || !session!!.isAlive()) {
            session = TerminalSession(
                shellPath = "$USR_PATH/bin/bash",
                homePath = HOME_PATH,
                env = createEnvironment()
            )
            session?.start()
        }
        return session!!
    }

    suspend fun executeCommand(command: String) {
        withContext(Dispatchers.IO) {
            session?.write(command)
        }
    }

    suspend fun isOpenCodeInstalled(): Boolean = withContext(Dispatchers.IO) {
        val opencodePath = File("$USR_PATH/bin/opencode")
        opencodePath.exists() && opencodePath.canExecute()
    }

    suspend fun isDeepSeekConfigured(): Boolean = withContext(Dispatchers.IO) {
        val configFile = File("$HOME_PATH/.config/opencode/opencode.json")
        if (!configFile.exists()) return@withContext false
        configFile.readText().contains("deepseek")
    }

    private fun createEnvironment(): Map<String, String> {
        return mapOf(
            "HOME" to HOME_PATH,
            "PREFIX" to USR_PATH,
            "PATH" to "$USR_PATH/bin:$USR_PATH/bin/applets",
            "LD_LIBRARY_PATH" to "$USR_PATH/lib",
            "LANG" to "en_US.UTF-8",
            "TERM" to "xterm-256color",
            "TMPDIR" to "$USR_PATH/tmp"
        )
    }
}
