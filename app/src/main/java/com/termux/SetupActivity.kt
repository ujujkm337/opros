package com.termux

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.termux.databinding.ActivitySetupBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Мастер первого запуска.
 * 1. Распаковка bootstrap
 * 2. Установка OpenCode (guysoft native aarch64)
 * 3. Настройка DeepSeek V4.1 Flash Free
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    private val USR = TermuxService.USR_PATH
    private val HOME = TermuxService.HOME_PATH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener { startSetup() }
    }

    private fun startSetup() {
        binding.btnStart.visibility = View.GONE
        binding.progressBar.max = 3

        lifecycleScope.launch {
            // Шаг 1: Bootstrap
            updateStep(1, "Bootstrap", "Распаковка Termux rootfs...")
            val bootstrapOk = installBootstrap()
            if (!bootstrapOk) {
                showError("Не удалось установить bootstrap")
                return@launch
            }

            // Шаг 2: OpenCode
            updateStep(2, "OpenCode", "Установка OpenCode (native aarch64)...")
            val opencodeOk = installOpenCode()
            if (!opencodeOk) {
                appendLog("⚠ OpenCode не установлен — можно установить позже вручную")
            }

            // Шаг 3: DeepSeek
            updateStep(3, "DeepSeek", "Настройка DeepSeek V4.1 Flash Free...")
            configureDeepSeek()

            // Готово!
            runOnUiThread {
                binding.progressBar.progress = 3
                binding.tvStepTitle.text = "✓ Установка завершена!"
                binding.tvStepDescription.text = "OpenCode + DeepSeek готовы к работе"
                binding.btnStart.visibility = View.VISIBLE
                binding.btnStart.text = "🚀 Запустить"
                binding.btnStart.setOnClickListener { finish() }
            }

            // Сохранить
            val prefs = getSharedPreferences("opros", MODE_PRIVATE)
            prefs.edit().putBoolean("first_run", false).apply()
        }
    }

    private suspend fun installBootstrap(): Boolean {
        if (BootstrapManager.isInstalled(this)) {
            appendLog("✓ Bootstrap уже установлен")
            return true
        }

        var success = false
        BootstrapManager.install(
            this,
            onProgress = { msg -> appendLog(msg) },
            onComplete = { result -> success = result }
        )
        return success
    }

    private suspend fun installOpenCode(): Boolean {
        appendLog("Установка OpenCode (guysoft/opencode-termux)...")
        
        // Обновить пакеты
        val updateResult = runShellCommand("$USR/bin/pkg update -y")
        appendLog("pkg update: exit ${updateResult.second}")
        
        // Установить зависимости
        val depsResult = runShellCommand("$USR/bin/pkg install -y curl ripgrep")
        appendLog("pkg install deps: exit ${depsResult.second}")

        // Скачать и установить OpenCode
        val installCmd = """
            cd /tmp
            $USR/bin/curl -LO https://github.com/guysoft/opencode-termux/releases/latest/download/opencode-aarch64.deb
            $USR/bin/dpkg -i opencode-aarch64.deb || $USR/bin/pkg install -f -y
            rm -f /tmp/opencode-aarch64.deb
        """.trimIndent()
        
        val installResult = runShellCommand(installCmd)
        appendLog("OpenCode install: exit ${installResult.second}")
        
        if (installResult.first.isNotEmpty()) {
            appendLog(installResult.first.takeLast(500))
        }

        // Проверить
        val checkResult = runShellCommand("$USR/bin/opencode --version 2>&1 || echo NOT_INSTALLED")
        val version = checkResult.first.trim()
        
        if (!version.contains("NOT_INSTALLED") && version.isNotEmpty()) {
            appendLog("✓ OpenCode установлен: $version")
            return true
        } else {
            appendLog("✗ OpenCode не установлен")
            return false
        }
    }

    private suspend fun configureDeepSeek() {
        val configDir = File("$HOME/.config/opencode")
        configDir.mkdirs()
        
        val configFile = File(configDir, "opencode.json")
        configFile.writeText("""
{
  "${'$'}schema": "https://opencode.ai/config.json",
  "model": "opencode/deepseek-v4.1-flash-free"
}
        """.trimIndent())
        
        appendLog("✓ DeepSeek V4.1 Flash Free настроен")
        appendLog("  Модель: opencode/deepseek-v4.1-flash-free")
        appendLog("  Цена: \$0 (бесплатно)")
        appendLog("  Контекст: 200K токенов")
    }

    private suspend fun runShellCommand(command: String): Pair<String, Int> {
        return try {
            val env = arrayOf(
                "HOME=$HOME",
                "PREFIX=$USR",
                "PATH=$USR/bin",
                "LD_LIBRARY_PATH=$USR/lib",
                "LANG=en_US.UTF-8",
                "TERM=xterm-256color",
                "TMPDIR=$USR/tmp"
            )
            
            val process = Runtime.getRuntime().exec(
                arrayOf("$USR/bin/sh", "-c", command),
                env,
                File(HOME)
            )
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            
            val exitCode = process.waitFor()
            Pair(output.toString(), exitCode)
        } catch (e: Exception) {
            Pair("Error: ${e.message}", -1)
        }
    }

    private fun updateStep(num: Int, name: String, description: String) {
        runOnUiThread {
            binding.tvStepTitle.text = "Шаг $num/3: $name"
            binding.tvStepDescription.text = description
            binding.progressBar.progress = num - 1
        }
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            binding.tvLog.append("$message\n")
            val scrollAmount = binding.tvLog.layout?.getLineTop(binding.tvLog.lineCount) ?: 0
            binding.scrollView.scrollTo(0, scrollAmount - binding.scrollView.height)
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            binding.tvStepDescription.text = "ОШИБКА: $message"
            binding.btnStart.visibility = View.VISIBLE
            binding.btnStart.text = "Повторить"
            binding.btnStart.setOnClickListener { startSetup() }
        }
    }
}
