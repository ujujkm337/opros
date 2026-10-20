package dev.opros.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.opros.launcher.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Main launcher activity for Opros.
 * Communicates with Termux via RUN_COMMAND intent to launch OpenCode.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkTermuxInstalled()
    }

    private fun setupUI() {
        // Launch OpenCode button
        binding.btnLaunchOpenCode.setOnClickListener {
            launchOpenCode()
        }

        // Open Termux button
        binding.btnOpenTermux.setOnClickListener {
            openTermux()
        }

        // Setup wizard button
        binding.btnSetup.setOnClickListener {
            startSetup()
        }

        // Status button
        binding.btnCheckStatus.setOnClickListener {
            checkOpenCodeStatus()
        }

        // Info button
        binding.btnInfo.setOnClickListener {
            showInfo()
        }
    }

    private fun checkTermuxInstalled() {
        val isInstalled = isTermuxInstalled()
        
        if (!isInstalled) {
            binding.tvStatus.text = "⚠ Termux не установлен"
            binding.tvStatus.setTextColor(getColor(R.color.error))
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Требуется Termux")
                .setMessage(
                    "Для работы Opros необходим Termux из F-Droid.\n\n" +
                    "ВАЖНО: Не устанавливайте Termux из Google Play — эта версия устарела!\n\n" +
                    "Скачайте Termux с:\n" +
                    "• F-Droid: https://f-droid.org/packages/com.termux/\n" +
                    "• GitHub: https://github.com/termux/termux-app/releases"
                )
                .setPositiveButton("Скачать Termux") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
                    startActivity(intent)
                }
                .setNegativeButton("Позже", null)
                .show()
        } else {
            binding.tvStatus.text = "✓ Termux установлен"
            binding.tvStatus.setTextColor(getColor(R.color.success))
            checkOpenCodeInstalled()
        }
    }

    private fun isTermuxInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun checkOpenCodeInstalled() {
        lifecycleScope.launch {
            val installed = TermuxCommandExecutor.checkOpenCodeInstalled(this@MainActivity)
            runOnUiThread {
                if (installed) {
                    binding.tvStatus.text = "✓ OpenCode готов к работе"
                    binding.tvStatus.setTextColor(getColor(R.color.success))
                } else {
                    binding.tvStatus.text = "⚠ OpenCode не установлен — запустите Setup"
                    binding.tvStatus.setTextColor(getColor(R.color.warning))
                }
            }
        }
    }

    private fun launchOpenCode() {
        if (!isTermuxInstalled()) {
            Toast.makeText(this, "Сначала установите Termux", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            binding.tvStatus.text = "Запуск OpenCode..."
            
            // Launch OpenCode in Termux
            val success = TermuxCommandExecutor.runInTermux(
                this@MainActivity,
                "opencode",
                runInBackground = false
            )
            
            runOnUiThread {
                if (success) {
                    binding.tvStatus.text = "✓ OpenCode запущен в Termux"
                    binding.tvStatus.setTextColor(getColor(R.color.success))
                    
                    // Open Termux to show the session
                    openTermux()
                } else {
                    binding.tvStatus.text = "✗ Ошибка запуска — проверьте Setup"
                    binding.tvStatus.setTextColor(getColor(R.color.error))
                    Toast.makeText(
                        this@MainActivity,
                        "Не удалось запустить OpenCode. Запустите Setup.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun openTermux() {
        val intent = packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Не удалось открыть Termux", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSetup() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
    }

    private fun checkOpenCodeStatus() {
        lifecycleScope.launch {
            val version = TermuxCommandExecutor.getOpenCodeVersion(this@MainActivity)
            val deepseekConfigured = TermuxCommandExecutor.isDeepSeekConfigured(this@MainActivity)
            
            runOnUiThread {
                val status = buildString {
                    appendLine("=== Статус Opros ===")
                    appendLine()
                    appendLine("Termux: ${if (isTermuxInstalled()) "✓ установлен" else "✗ не установлен"}")
                    appendLine("OpenCode: ${version ?: "✗ не установлен"}")
                    appendLine("DeepSeek V4.1 Flash: ${if (deepseekConfigured) "✓ настроен" else "✗ не настроен"}")
                    appendLine()
                    if (deepseekConfigured) {
                        appendLine("Модель: opencode/deepseek-v4.1-flash-free")
                        appendLine("Стоимость: \$0 (бесплатно)")
                        appendLine("Контекст: 200K токенов")
                    }
                }
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Статус системы")
                    .setMessage(status)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showInfo() {
        val info = """
            Opros Launcher v1.0.0
            
            Мобильная среда разработки с AI-ассистентом.
            
            Как это работает:
            1. Установите Termux из F-Droid
            2. Запустите Setup (установит OpenCode)
            3. Нажмите "Запустить OpenCode"
            
            DeepSeek V4.1 Flash Free:
            • Полностью бесплатный AI
            • 200K контекст
            • Работает через OpenCode Zen
            
            OpenCode устанавливается нативно в Termux
            (без proot, без виртуализации).
        """.trimIndent()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("О приложении")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }
}
