package com.termux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class TermuxService : Service() {

    private val binder = LocalBinder()
    private var terminalSession: TerminalEmulator? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "termux_service_channel"
        const val NOTIFICATION_ID = 1
        const val FILES_PATH = "/data/data/com.termux/files"
        const val USR_PATH = "$FILES_PATH/usr"
        const val HOME_PATH = "$FILES_PATH/home"
    }

    inner class LocalBinder : Binder() {
        fun getService(): TermuxService = this@TermuxService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initializeTerminal()
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalSession?.close()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Opros Development Environment",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the development environment running"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Opros Dev Environment")
            .setContentText("Termux + Debian + OpenCode active")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initializeTerminal() {
        if (TermuxInstaller.isBootstrapInstalled(this)) {
            terminalSession = TerminalEmulator(
                shellPath = "$USR_PATH/bin/bash",
                homePath = HOME_PATH,
                environment = createTermuxEnvironment()
            )
            terminalSession?.start()
        }
    }

    private fun createTermuxEnvironment(): Map<String, String> {
        return mapOf(
            "HOME" to HOME_PATH,
            "PREFIX" to USR_PATH,
            "PATH" to "$USR_PATH/bin:$USR_PATH/bin/applets",
            "LD_LIBRARY_PATH" to "$USR_PATH/lib",
            "LANG" to "en_US.UTF-8",
            "TERM" to "xterm-256color",
            "TMPDIR" to "$FILES_PATH/usr/tmp",
            "ANDROID_DATA" to "/data",
            "ANDROID_ROOT" to "/system"
        )
    }

    fun getCurrentSession(): TerminalEmulator? {
        return terminalSession
    }

    fun executeCommand(command: String) {
        serviceScope.launch {
            terminalSession?.writeToTerminal(command)
        }
    }

    fun executeInDebian(command: String) {
        serviceScope.launch {
            terminalSession?.writeToTerminal("proot-distro login debian -- $command\n")
        }
    }

    fun isDebianInstalled(): Boolean {
        val debianPath = File("$FILES_PATH/usr/var/lib/proot-distro/installed-rootfs/debian")
        return debianPath.exists()
    }

    fun isOpenCodeInstalled(): Boolean {
        val opencodePath = File("$USR_PATH/bin/opencode")
        return opencodePath.exists()
    }
}
