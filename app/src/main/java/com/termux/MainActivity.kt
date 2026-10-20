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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var termuxService: TermuxService? = null
    private var isBound = false
    private var terminalSession: TerminalEmulator? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TermuxService.LocalBinder
            termuxService = binder.getService()
            isBound = true
            setupTerminal()
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
        startForegroundService(intent)
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
        // OpenCode session button
        binding.btnLaunchOpenCode.setOnClickListener {
            launchOpenCodeSession()
        }

        // Debian shell button
        binding.btnLaunchDebian.setOnClickListener {
            launchDebianShell()
        }

        // Termux shell button
        binding.btnLaunchTermux.setOnClickListener {
            launchTermuxShell()
        }

        // AI info button
        binding.btnShowAiPrompt.setOnClickListener {
            showAiSystemPrompt()
        }

        // Terminal keyboard extras
        binding.btnTab.setOnClickListener {
            terminalSession?.writeToTerminal("\t")
        }
        binding.btnCtrl.setOnClickListener {
            terminalSession?.toggleCtrlMode()
        }
        binding.btnEsc.setOnClickListener {
            terminalSession?.writeToTerminal("\u001b")
        }
    }

    private fun checkFirstRun() {
        val prefs = getSharedPreferences("opros_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("first_run", true)

        if (isFirstRun) {
            // Check if Termux bootstrap is installed
            val bootstrapInstalled = TermuxInstaller.isBootstrapInstalled(this)
            if (!bootstrapInstalled) {
                startSetup()
            } else {
                prefs.edit().putBoolean("first_run", false).apply()
            }
        }
    }

    private fun startSetup() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
    }

    private fun setupTerminal() {
        termuxService?.let { service ->
            terminalSession = service.getCurrentSession()
            terminalSession?.let { session ->
                binding.terminalView.attachSession(session)
                updateStatus("Terminal ready")
            }
        }
    }

    private fun launchOpenCodeSession() {
        lifecycleScope.launch {
            updateStatus("Launching OpenCode in Debian...")
            termuxService?.executeCommand(
                "proot-distro login debian -- opencode\n"
            )
            updateStatus("OpenCode session active")
        }
    }

    private fun launchDebianShell() {
        lifecycleScope.launch {
            updateStatus("Entering Debian shell...")
            termuxService?.executeCommand("proot-distro login debian\n")
            updateStatus("Debian shell active")
        }
    }

    private fun launchTermuxShell() {
        lifecycleScope.launch {
            updateStatus("Termux shell ready")
            termuxService?.executeCommand("bash\n")
        }
    }

    private fun showAiSystemPrompt() {
        val prompt = AiSystemPromptGenerator.generatePrompt(this)
        binding.tvTerminalOutput.text = prompt
        binding.tvTerminalOutput.visibility = View.VISIBLE
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.tvStatus.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
