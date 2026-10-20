package com.termux

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Менеджер bootstrap Termux.
 * Распаковывает встроенный rootfs при первом запуске.
 */
object BootstrapManager {

    private const val TAG = "BootstrapManager"
    private const val BOOTSTRAP_ASSET = "bootstrap-arm64.zip"
    private const val FILES_PATH = "/data/data/com.termux/files"
    private const val USR_PATH = "$FILES_PATH/usr"

    fun isInstalled(context: Context): Boolean {
        val bashBinary = File("$USR_PATH/bin/bash")
        return bashBinary.exists() && bashBinary.canExecute()
    }

    suspend fun install(
        context: Context,
        onProgress: (String) -> Unit,
        onComplete: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress("Создание директорий...")
            File(FILES_PATH).mkdirs()
            File(USR_PATH).mkdirs()
            File("$FILES_PATH/home").mkdirs()

            onProgress("Распаковка bootstrap...")
            extractBootstrap(context)

            onProgress("Установка прав...")
            setPermissions()

            onProgress("Настройка окружения...")
            setupEnvironment()

            onProgress("Bootstrap установлен!")
            onComplete(true)
        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation failed", e)
            onProgress("Ошибка: ${e.message}")
            onComplete(false)
        }
    }

    private fun extractBootstrap(context: Context) {
        try {
            context.assets.open(BOOTSTRAP_ASSET).use { inputStream ->
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
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to extract bootstrap: ${e.message}", e)
        }
    }

    private fun setPermissions() {
        // Сделать бинарники исполняемыми
        val binDir = File("$USR_PATH/bin")
        if (binDir.exists()) {
            binDir.listFiles()?.forEach { file ->
                file.setExecutable(true, false)
                file.setReadable(true, false)
            }
        }

        // Библиотеки
        val libDir = File("$USR_PATH/lib")
        if (libDir.exists()) {
            libDir.walkTopDown()
                .filter { it.name.endsWith(".so") || it.name.startsWith("ld-") }
                .forEach { 
                    it.setExecutable(true, false)
                    it.setReadable(true, false)
                }
        }
    }

    private fun setupEnvironment() {
        val homeDir = File("$FILES_PATH/home")
        
        // .bashrc
        val bashrc = File(homeDir, ".bashrc")
        if (!bashrc.exists()) {
            bashrc.writeText("""
                export PS1='\[\033[01;32m\]\u@opros\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
                export PATH="$USR_PATH/bin:${'$'}PATH"
                export HOME="$FILES_PATH/home"
                alias ls='ls --color=auto'
                alias ll='ls -la'
                
                echo ""
                echo "╔═══════════════════════════════════════════╗"
                echo "║  Opros - OpenCode + DeepSeek V4.1 Flash   ║"
                echo "╚═══════════════════════════════════════════╝"
                echo ""
                echo "Команды:"
                echo "  opencode  - запустить AI-агент"
                echo "  pkg install <package> - установить пакет"
                echo ""
            """.trimIndent())
        }

        // .profile
        val profile = File(homeDir, ".profile")
        if (!profile.exists()) {
            profile.writeText("""
                if [ -f "${'$'}HOME/.bashrc" ]; then
                    . "${'$'}HOME/.bashrc"
                fi
            """.trimIndent())
        }
    }
}
