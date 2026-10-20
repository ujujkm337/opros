package com.termux

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Handles installation of the Termux bootstrap environment.
 * 
 * The bootstrap is a pre-built rootfs that contains:
 * - BusyBox utilities
 * - Package manager (pkg/apt)
 * - Basic Linux tools
 * - Shell (bash)
 * 
 * The bootstrap zip should be placed in app/src/main/assets/bootstrap-arm64.zip
 */
object TermuxInstaller {

    private const val TAG = "TermuxInstaller"
    private const val BOOTSTRAP_ASSET = "bootstrap-arm64.zip"
    private const val FILES_PATH = "/data/data/com.termux/files"
    private const val USR_PATH = "$FILES_PATH/usr"

    /**
     * Checks if the Termux bootstrap is already installed
     */
    fun isBootstrapInstalled(context: Context): Boolean {
        val usrDir = File(USR_PATH)
        val bashBinary = File("$USR_PATH/bin/bash")
        return usrDir.exists() && bashBinary.exists()
    }

    /**
     * Installs the Termux bootstrap from assets
     */
    suspend fun installBootstrap(
        context: Context,
        onProgress: (String) -> Unit,
        onComplete: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress("Creating directories...")
            createDirectories()

            onProgress("Extracting bootstrap...")
            extractBootstrap(context)

            onProgress("Setting permissions...")
            setPermissions()

            onProgress("Running post-install setup...")
            runPostInstallSetup()

            onProgress("Bootstrap installed successfully!")
            onComplete(true)
        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation failed", e)
            onProgress("Installation failed: ${e.message}")
            onComplete(false)
        }
    }

    private fun createDirectories() {
        File(FILES_PATH).mkdirs()
        File(USR_PATH).mkdirs()
        File("$FILES_PATH/home").mkdirs()
    }

    private fun extractBootstrap(context: Context) {
        try {
            val inputStream = context.assets.open(BOOTSTRAP_ASSET)
            ZipInputStream(inputStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val outputFile = File(FILES_PATH, entry.name)
                    
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            zipStream.copyTo(fos)
                        }
                    }
                    
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to extract bootstrap: ${e.message}", e)
        }
    }

    private fun setPermissions() {
        // Make binaries executable
        val binDir = File("$USR_PATH/bin")
        if (binDir.exists()) {
            binDir.listFiles()?.forEach { file ->
                file.setExecutable(true, false)
            }
        }

        // Make lib executables
        val libDir = File("$USR_PATH/lib")
        if (libDir.exists()) {
            libDir.walkTopDown()
                .filter { it.name.endsWith(".so") || it.name.startsWith("ld-") }
                .forEach { it.setExecutable(true, false) }
        }
    }

    private fun runPostInstallSetup() {
        // Create essential symlinks and configurations
        val homeDir = File("$FILES_PATH/home")
        
        // Create .bashrc
        val bashrc = File(homeDir, ".bashrc")
        if (!bashrc.exists()) {
            bashrc.writeText("""
                # ~/.bashrc
                export PS1='\[\033[01;32m\]\u@opros\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
                export PATH="$USR_PATH/bin:${'$'}PATH"
                export HOME="$FILES_PATH/home"
                alias ls='ls --color=auto'
                alias ll='ls -la'
                
                # Welcome message
                echo "Welcome to Opros Development Environment"
                echo "Type 'help' for available commands"
            """.trimIndent())
        }

        // Create .profile
        val profile = File(homeDir, ".profile")
        if (!profile.exists()) {
            profile.writeText("""
                # ~/.profile
                if [ -f "${'$'}HOME/.bashrc" ]; then
                    . "${'$'}HOME/.bashrc"
                fi
            """.trimIndent())
        }
    }

    /**
     * Downloads and installs a Termux package
     */
    suspend fun installPackage(
        packageName: String,
        onProgress: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Installing $packageName...")
            val process = Runtime.getRuntime().exec(
                arrayOf("$USR_PATH/bin/pkg", "install", "-y", packageName),
                createEnvironmentArray(),
                File(FILES_PATH)
            )
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install package: $packageName", e)
            false
        }
    }

    private fun createEnvironmentArray(): Array<String> {
        return arrayOf(
            "HOME=$FILES_PATH/home",
            "PREFIX=$USR_PATH",
            "PATH=$USR_PATH/bin",
            "LD_LIBRARY_PATH=$USR_PATH/lib",
            "LANG=en_US.UTF-8",
            "TERM=xterm-256color"
        )
    }
}
