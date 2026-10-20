package dev.opros.launcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.opros.launcher.databinding.ActivitySetupBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Setup wizard that installs OpenCode in Termux and configures DeepSeek.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var currentStep = 0

    data class SetupStep(
        val name: String,
        val description: String,
        val action: suspend () -> Boolean
    )

    private lateinit var steps: List<SetupStep>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSteps()
        checkPrerequisites()
    }

    private fun setupSteps() {
        steps = listOf(
            SetupStep(
                "Разрешения",
                "Включение allow-external-apps в Termux...",
                { enableExternalApps() }
            ),
            SetupStep(
                "OpenCode",
                "Установка OpenCode (native aarch64)...",
                { installOpenCode() }
            ),
            SetupStep(
                "DeepSeek",
                "Настройка DeepSeek V4.1 Flash Free...",
                { configureDeepSeek() }
            )
        )
    }

    private fun checkPrerequisites() {
        // Check if Termux is installed
        val termuxInstalled = try {
            packageManager.getPackageInfo(MainActivity.TERMUX_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }

        if (!termuxInstalled) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Требуется Termux")
                .setMessage("Установите Termux из F-Droid перед запуском Setup.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        // Start setup
        binding.btnStart.setOnClickListener { startSetup() }
    }

    private fun startSetup() {
        binding.btnStart.visibility = View.GONE
        binding.progressBar.max = steps.size

        lifecycleScope.launch {
            for ((index, step) in steps.withIndex()) {
                currentStep = index
                runOnUiThread {
                    binding.tvStepTitle.text = "Шаг ${index + 1}/${steps.size}: ${step.name}"
                    binding.tvStepDescription.text = step.description
                    binding.progressBar.progress = index
                }

                val success = step.action()
                if (!success) {
                    runOnUiThread {
                        binding.tvStepDescription.text = "ОШИБКА: ${step.name}"
                        binding.btnStart.visibility = View.VISIBLE
                        binding.btnStart.text = "Повторить"
                    }
                    return@launch
                }

                delay(500)
            }

            // All done
            runOnUiThread {
                binding.progressBar.progress = steps.size
                binding.tvStepTitle.text = "Setup завершён!"
                binding.tvStepDescription.text = "OpenCode + DeepSeek готовы к работе"
                binding.btnStart.visibility = View.VISIBLE
                binding.btnStart.text = "Запустить OpenCode"
                binding.btnStart.setOnClickListener {
                    launchOpenCode()
                }
            }
        }
    }

    private suspend fun enableExternalApps(): Boolean {
        appendLog("Проверка allow-external-apps...")
        
        val alreadyEnabled = TermuxCommandExecutor.isExternalAppsAllowed(this)
        if (alreadyEnabled) {
            appendLog("✓ Уже включено")
            return true
        }

        appendLog("Отправка команды в Termux...")
        appendLog("")
        appendLog("ВАЖНО: Откройте Termux и подтвердите выполнение команды!")
        appendLog("")
        
        val command = TermuxCommandExecutor.getEnableExternalAppsCommand()
        val success = TermuxCommandExecutor.runInTermux(
            this,
            command,
            runInBackground = true
        )

        if (success) {
            appendLog("✓ Команда отправлена")
            appendLog("")
            appendLog("Подождите 3 секунды...")
            delay(3000)
            
            // Check if it worked
            val enabled = TermuxCommandExecutor.isExternalAppsAllowed(this)
            if (enabled) {
                appendLog("✓ allow-external-apps включен")
                return true
            } else {
                appendLog("✗ Не удалось включить")
                appendLog("")
                appendLog("Вручную выполните в Termux:")
                appendLog(TermuxCommandExecutor.getEnableExternalAppsCommand())
                return false
            }
        } else {
            appendLog("✗ Ошибка отправки команды")
            return false
        }
    }

    private suspend fun installOpenCode(): Boolean {
        appendLog("Установка OpenCode (guysoft/opencode-termux)...")
        appendLog("Это нативная сборка для Android aarch64")
        appendLog("")

        val command = TermuxCommandExecutor.getInstallOpenCodeCommand()
        val success = TermuxCommandExecutor.runInTermux(
            this,
            command,
            runInBackground = true
        )

        if (success) {
            appendLog("✓ Команда установки отправлена в Termux")
            appendLog("")
            appendLog("Откройте Termux для просмотра процесса...")
            appendLog("Установка займёт 1-3 минуты")
            appendLog("")
            
            // Wait and check
            appendLog("Ожидание завершения (60 сек)...")
            delay(60000)
            
            val installed = TermuxCommandExecutor.checkOpenCodeInstalled(this)
            if (installed) {
                val version = TermuxCommandExecutor.getOpenCodeVersion(this)
                appendLog("✓ OpenCode установлен: $version")
                return true
            } else {
                appendLog("✗ OpenCode не обнаружен")
                appendLog("Проверьте Termux на наличие ошибок")
                return false
            }
        } else {
            appendLog("✗ Ошибка отправки команды")
            return false
        }
    }

    private suspend fun configureDeepSeek(): Boolean {
        appendLog("Настройка DeepSeek V4.1 Flash Free...")
        appendLog("")

        val command = TermuxCommandExecutor.getConfigureDeepSeekCommand()
        val success = TermuxCommandExecutor.runInTermux(
            this,
            command,
            runInBackground = true
        )

        if (success) {
            appendLog("✓ Конфигурация отправлена")
            delay(2000)
            
            val configured = TermuxCommandExecutor.isDeepSeekConfigured(this)
            if (configured) {
                appendLog("✓ DeepSeek V4.1 Flash Free настроен")
                appendLog("")
                appendLog("Модель: opencode/deepseek-v4.1-flash-free")
                appendLog("Стоимость: \$0 (бесплатно)")
                appendLog("Контекст: 200K токенов")
                return true
            } else {
                appendLog("✗ Ошибка конфигурации")
                return false
            }
        } else {
            appendLog("✗ Ошибка отправки команды")
            return false
        }
    }

    private fun launchOpenCode() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            binding.tvLog.append("$message\n")
            val scrollAmount = binding.tvLog.layout?.getLineTop(binding.tvLog.lineCount) ?: 0
            binding.scrollView.scrollTo(0, scrollAmount - binding.scrollView.height)
        }
    }
}
