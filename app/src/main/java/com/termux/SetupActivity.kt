package com.termux

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.termux.databinding.ActivitySetupBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Setup wizard that runs on first launch.
 * Installs: Termux bootstrap → Debian → OpenCode → MCP bridge
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
        startSetup()
    }

    private fun setupSteps() {
        steps = listOf(
            SetupStep(
                "Bootstrap",
                "Installing Termux bootstrap (rootfs)...",
                { installBootstrap() }
            ),
            SetupStep(
                "Package Update",
                "Updating package repositories...",
                { updatePackages() }
            ),
            SetupStep(
                "Essential Packages",
                "Installing proot-distro, nodejs, git...",
                { installEssentialPackages() }
            ),
            SetupStep(
                "Debian",
                "Installing Debian (proot)...",
                { installDebian() }
            ),
            SetupStep(
                "OpenCode",
                "Installing OpenCode AI assistant...",
                { installOpenCode() }
            ),
            SetupStep(
                "MCP Bridge",
                "Setting up termux-mcp for AI access...",
                { installMcpBridge() }
            ),
            SetupStep(
                "Configuration",
                "Writing configuration files...",
                { writeConfiguration() }
            )
        )
    }

    private fun startSetup() {
        lifecycleScope.launch {
            binding.progressBar.max = steps.size
            binding.btnSkip.visibility = View.GONE

            for ((index, step) in steps.withIndex()) {
                currentStep = index
                runOnUiThread {
                    binding.tvStepTitle.text = "Step ${index + 1}/${steps.size}: ${step.name}"
                    binding.tvStepDescription.text = step.description
                    binding.progressBar.progress = index
                }

                val success = step.action()
                if (!success) {
                    runOnUiThread {
                        binding.tvStepDescription.text = "FAILED: ${step.name}"
                        binding.btnSkip.visibility = View.VISIBLE
                        binding.btnSkip.text = "Retry"
                        binding.btnSkip.setOnClickListener { startSetup() }
                    }
                    return@launch
                }

                delay(300) // Brief pause between steps
            }

            // All done!
            runOnUiThread {
                binding.progressBar.progress = steps.size
                binding.tvStepTitle.text = "Setup Complete!"
                binding.tvStepDescription.text = "Your development environment is ready."
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnSkip.text = "Launch"
                binding.btnSkip.setOnClickListener { finishSetup() }
            }

            // Mark setup as complete
            val prefs = getSharedPreferences("opros_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("first_run", false).apply()
        }
    }

    private suspend fun installBootstrap(): Boolean {
        return try {
            if (TermuxInstaller.isBootstrapInstalled(this)) {
                appendLog("Bootstrap already installed, skipping.")
                return true
            }
            
            var success = false
            TermuxInstaller.installBootstrap(
                this,
                onProgress = { msg -> appendLog(msg) },
                onComplete = { result -> success = result }
            )
            success
        } catch (e: Exception) {
            appendLog("Error: ${e.message}")
            false
        }
    }

    private suspend fun updatePackages(): Boolean {
        return runShellCommand("$USR/bin/pkg update -y 2>&1")
    }

    private suspend fun installEssentialPackages(): Boolean {
        val packages = listOf("proot-distro", "nodejs", "git", "curl", "python", "openssh")
        for (pkg in packages) {
            appendLog("Installing $pkg...")
            val success = runShellCommand("$USR/bin/pkg install -y $pkg 2>&1")
            if (!success) {
                appendLog("Warning: Failed to install $pkg")
                // Continue anyway - some packages might not be critical
            }
        }
        return true
    }

    private suspend fun installDebian(): Boolean {
        appendLog("Installing Debian via proot-distro...")
        return runShellCommand("$USR/bin/proot-distro install debian 2>&1")
    }

    private suspend fun installOpenCode(): Boolean {
        appendLog("Installing OpenCode...")
        // Try npm installation first
        val npmResult = runShellCommand(
            "$USR/bin/npm install -g opencode-termux 2>&1 || " +
            "$USR/bin/bash -c 'curl -fsSL https://opencode.ai/install | bash' 2>&1"
        )
        if (npmResult) {
            appendLog("OpenCode installed successfully via npm")
        } else {
            appendLog("Warning: OpenCode installation had issues. Manual setup may be required.")
        }
        return true // Non-critical, continue
    }

    private suspend fun installMcpBridge(): Boolean {
        appendLog("Setting up termux-mcp bridge...")
        return runShellCommand(
            "$USR/bin/pip install termux-mcp 2>&1 || " +
            "$USR/bin/npm install -g termux-mcp 2>&1 || echo 'MCP bridge install skipped'"
        )
    }

    private suspend fun writeConfiguration(): Boolean {
        return try {
            // Write OpenCode config
            val configDir = "$HOME_PATH/.config/opencode"
            runShellCommand("mkdir -p $configDir 2>&1")
            
            val configContent = """
                {
                    "mcp": {
                        "termux": {
                            "command": "python3",
                            "args": ["-m", "termux_mcp"],
                            "env": {}
                        }
                    }
                }
            """.trimIndent()
            
            runShellCommand("echo '$configContent' > $configDir/config.json 2>&1")
            
            // Write system prompt
            val promptDir = "$HOME_PATH/.config/opencode"
            val promptContent = AiSystemPromptGenerator.generatePrompt(this)
            runShellCommand("echo '${promptContent.replace("'", "'\\''")}' > $promptDir/system-prompt.txt 2>&1")
            
            appendLog("Configuration files written.")
            true
        } catch (e: Exception) {
            appendLog("Config write error: ${e.message}")
            true // Non-critical
        }
    }

    private suspend fun runShellCommand(command: String): Boolean {
        return try {
            val env = arrayOf(
                "HOME=$HOME_PATH",
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
                java.io.File(HOME_PATH)
            )
            
            // Read output in real-time
            val reader = process.inputStream.bufferedReader()
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
                appendLog(line ?: "")
            }
            
            val exitCode = process.waitFor()
            appendLog("Exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            appendLog("Command error: ${e.message}")
            false
        }
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            binding.tvLog.append("$message\n")
            // Auto-scroll
            val scrollAmount = binding.tvLog.layout?.getLineTop(binding.tvLog.lineCount) ?: 0
            binding.scrollView.scrollTo(0, scrollAmount - binding.scrollView.height)
        }
    }

    private fun finishSetup() {
        Toast.makeText(this, "Setup complete!", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val USR = "/data/data/com.termux/files/usr"
        private const val HOME_PATH = "/data/data/com.termux/files/home"
    }
}
