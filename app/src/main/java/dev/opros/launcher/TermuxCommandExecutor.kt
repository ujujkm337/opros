package dev.opros.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Executes commands in Termux via the RUN_COMMAND intent API.
 * 
 * Termux exposes a service (com.termux.app.RunCommandService) that allows
 * external apps to execute commands inside Termux.
 * 
 * Requirements:
 * - Termux must be installed from F-Droid or GitHub
 * - User must enable "allow-external-apps" in Termux properties:
 *   echo 'allow-external-apps = true' >> ~/.termux/termux.properties
 */
object TermuxCommandExecutor {

    private const val TAG = "TermuxExecutor"
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    
    private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    private const val TERMUX_HOME = "/data/data/com.termux/files/home"

    /**
     * Check if external apps are allowed in Termux
     */
    suspend fun isExternalAppsAllowed(context: Context): Boolean = withContext(Dispatchers.IO) {
        val propsFile = File("$TERMUX_HOME/.termux/termux.properties")
        if (!propsFile.exists()) return@withContext false
        
        val content = propsFile.readText()
        content.contains("allow-external-apps = true") ||
        content.contains("allow-external-apps=true")
    }

    /**
     * Enable external apps access (user must run this in Termux)
     */
    fun getEnableExternalAppsCommand(): String {
        return "mkdir -p ~/.termux && echo 'allow-external-apps = true' >> ~/.termux/termux.properties && termux-reload-settings"
    }

    /**
     * Run a command in Termux via RUN_COMMAND intent
     */
    suspend fun runInTermux(
        context: Context,
        command: String,
        workingDir: String = TERMUX_HOME,
        runInBackground: Boolean = false,
        stdin: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
                putExtra("com.termux.RUN_COMMAND_PATH", "$TERMUX_PREFIX/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workingDir)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", runInBackground)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", if (runInBackground) "0" else "1")
                if (stdin != null) {
                    putExtra("com.termux.RUN_COMMAND_STDIN", stdin)
                }
            }

            context.startForegroundService(intent)
            Log.i(TAG, "Command sent to Termux: $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command to Termux", e)
            false
        }
    }

    /**
     * Check if OpenCode is installed in Termux
     */
    suspend fun checkOpenCodeInstalled(context: Context): Boolean = withContext(Dispatchers.IO) {
        val opencodePath = File("$TERMUX_PREFIX/bin/opencode")
        opencodePath.exists() && opencodePath.canExecute()
    }

    /**
     * Get OpenCode version
     */
    suspend fun getOpenCodeVersion(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(
                "$TERMUX_PREFIX/bin/bash", "-c",
                "opencode --version 2>/dev/null || echo ''"
            )
            .redirectErrorStream(true)
            .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val version = reader.readLine()?.trim()
            process.waitFor()

            if (version?.isNotEmpty() == true) version else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get OpenCode version", e)
            null
        }
    }

    /**
     * Check if DeepSeek is configured in OpenCode
     */
    suspend fun isDeepSeekConfigured(context: Context): Boolean = withContext(Dispatchers.IO) {
        val configFile = File("$TERMUX_HOME/.config/opencode/opencode.json")
        if (!configFile.exists()) return@withContext false
        
        val content = configFile.readText()
        content.contains("deepseek") || content.contains("opencode/")
    }

    /**
     * Install OpenCode via guysoft/opencode-termux (native aarch64)
     */
    fun getInstallOpenCodeCommand(): String {
        return """
            set -e
            echo "=== Установка OpenCode для Termux (native aarch64) ==="
            
            # Update packages
            echo "[1/4] Обновление пакетов..."
            pkg update -y
            
            # Install dependencies
            echo "[2/4] Установка зависимостей..."
            pkg install -y curl ripgrep
            
            # Download and install opencode-termux (guysoft native build)
            echo "[3/4] Загрузка OpenCode (native aarch64)..."
            cd /tmp
            curl -LO https://github.com/guysoft/opencode-termux/releases/latest/download/opencode-aarch64.deb
            dpkg -i opencode-aarch64.deb || pkg install -f -y
            
            # Verify installation
            echo "[4/4] Проверка установки..."
            if command -v opencode &> /dev/null; then
                echo "✓ OpenCode установлен успешно!"
                opencode --version
            else
                echo "✗ Ошибка установки OpenCode"
                exit 1
            fi
            
            # Cleanup
            rm -f /tmp/opencode-aarch64.deb
            
            echo ""
            echo "=== Установка завершена ==="
        """.trimIndent()
    }

    /**
     * Configure DeepSeek V4.1 Flash Free in OpenCode
     */
    fun getConfigureDeepSeekCommand(): String {
        return """
            set -e
            echo "=== Настройка DeepSeek V4.1 Flash Free ==="
            
            # Create config directory
            mkdir -p ~/.config/opencode
            
            # Write OpenCode config with DeepSeek V4.1 Flash Free as default
            cat > ~/.config/opencode/opencode.json << 'EOF'
            {
              "${'$'}schema": "https://opencode.ai/config.json",
              "model": "opencode/deepseek-v4.1-flash-free"
            }
            EOF
            
            echo "✓ Конфигурация записана"
            echo ""
            echo "Модель: opencode/deepseek-v4.1-flash-free"
            echo "Стоимость: \$0 (бесплатно, ограниченное время)"
            echo "Контекст: 200K токенов"
            echo ""
            echo "Для использования запустите: opencode"
            echo "При первом запуске выполните: /connect"
            echo "и выберите провайдера OpenCode (Zen)"
            
            echo ""
            echo "=== Настройка завершена ==="
        """.trimIndent()
    }

    /**
     * Full setup command (install OpenCode + configure DeepSeek)
     */
    fun getFullSetupCommand(): String {
        return """
            ${getInstallOpenCodeCommand()}
            
            echo ""
            echo "====================================="
            echo ""
            
            ${getConfigureDeepSeekCommand()}
        """.trimIndent()
    }
}
