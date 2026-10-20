package dev.opros.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.opros.launcher.databinding.ActivityTerminalBinding

/**
 * Embedded terminal activity (optional, for viewing output).
 * For full terminal experience, use the Termux app directly.
 */
class TerminalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val command = intent.getStringExtra("command") ?: "echo 'No command'"
        binding.tvTerminal.text = "Выполнение: $command\n\nОткройте Termux для просмотра вывода."
    }
}
