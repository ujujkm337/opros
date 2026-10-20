package com.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.termux.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Главный экран Opros.
 * Встроенный Termux + OpenCode + DeepSeek V4.1 Flash Free.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var termuxService: TermuxService? = null
    private var isBound = false
    private var terminalSession: TerminalSession? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TermuxService.LocalBinder
            termuxService = binder.getService()
            isBound = true
            onServiceReady()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            termuxService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkFirstRun()
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, TermuxService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun setupUI() {
        // Запуск OpenCode с DeepSeek
        binding.btnLaunchOpenCode.setOnClickListener {
            launchOpenCode()
        }

        // Открыть shell Termux
        binding.btnOpenShell.setOnClickListener {
            openShell()
        }

        // Проверить статус
        binding.btnCheckStatus.setOnClickListener {
            checkStatus()
        }

        // Клавиатурные дополнения
        binding.btnTab.setOnClickListener { terminalSession?.write("\t") }
        binding.btnCtrl.setOnClickListener { terminalSession?.toggleCtrl() }
        binding.btnEsc.setOnClickListener { terminalSession?.write("\u001b") }
    }

    private fun checkFirstRun() {
        val prefs = getSharedPreferences("opros", Context.MODE_PRIVATE)
        val firstRun = prefs.getBoolean("first_run", true)

        if (firstRun) {
            val bootstrapInstalled = BootstrapManager.isInstalled(this)
            if (!bootstrapInstalled) {
                startActivity(Intent(this, SetupActivity::class.java))
            }
        }
    }

    private fun onServiceReady() {
        termuxService?.let { service ->
            if (service.isBootstrapReady()) {
                terminalSession = service.getOrCreateSession()
                terminalSession?.let { session ->
                    binding.terminalView.attachSession(session)
                    updateStatus("Готов к работе")
                    
                    // Проверить, установлен ли OpenCode
                    checkOpenCodeInstalled()
                }
            } else {
                updateStatus("Требуется Setup")
            }
        }
    }

    private fun checkOpenCodeInstalled() {
        lifecycleScope.launch {
            val installed = termuxService?.isOpenCodeInstalled() ?: false
            runOnUiThread {
                if (installed) {
                    binding.btnLaunchOpenCode.isEnabled = true
                    binding.tvOpenCodeStatus.text = "✓ OpenCode + DeepSeek готовы"
                    binding.tvOpenCodeStatus.setTextColor(getColor(R.color.success))
                } else {
                    binding.btnLaunchOpenCode.isEnabled = false
                    binding.tvOpenCodeStatus.text = "⚠ Запустите Setup для установки OpenCode"
                    binding.tvOpenCodeStatus.setTextColor(getColor(R.color.warning))
                }
            }
        }
    }

    private fun launchOpenCode() {
        lifecycleScope.launch {
            updateStatus("Запуск OpenCode...")
            termuxService?.executeCommand("opencode\n")
            runOnUiThread {
                updateStatus("OpenCode агент активен (DeepSeek V4.1 Flash Free)")
            }
        }
    }

    private fun openShell() {
        lifecycleScope.launch {
            updateStatus("Termux shell")
            termuxService?.executeCommand("bash\n")
        }
    }

    private fun checkStatus() {
        lifecycleScope.launch {
            val service = termuxService ?: return@launch
            val bootstrap = service.isBootstrapReady()
            val opencode = service.isOpenCodeInstalled()
            val deepseek = service.isDeepSeekConfigured()

            runOnUiThread {
                val status = buildString {
                    appendLine("=== Статус Opros ===")
                    appendLine()
                    appendLine("Termux bootstrap: ${if (bootstrap) "✓" else "✗"}")
                    appendLine("OpenCode: ${if (opencode) "✓ установлен" else "✗ не установлен"}")
                    appendLine("DeepSeek V4.1 Flash: ${if (deepseek) "✓ настроен" else "✗ не настроен"}")
                    appendLine()
                    if (opencode && deepseek) {
                        appendLine("Модель: opencode/deepseek-v4.1-flash-free")
                        appendLine("Стоимость: \$0 (бесплатно)")
                        appendLine("Контекст: 200K токенов")
                    }
                }

                com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Статус системы")
                    .setMessage(status)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.tvStatus.text = message
        }
    }
}
