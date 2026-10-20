package com.termux

import android.content.Context

/**
 * Generates the system prompt for the AI assistant (OpenCode)
 * that runs inside the Debian/Termux environment.
 * 
 * This prompt teaches the AI:
 * - What environment it's running in
 * - What tools are available
 * - How to interact with the Android system via MCP
 * - What limitations exist (no root, proot, etc.)
 */
object AiSystemPromptGenerator {

    /**
     * Generates a comprehensive system prompt for the AI assistant.
     */
    fun generatePrompt(context: Context): String {
        val deviceInfo = getDeviceInfo(context)
        return buildString {
            appendLine("# Environment")
            appendLine()
            appendLine("You are running inside a **Debian (proot)** environment within **Termux** on an **Android** device.")
            appendLine()
            appendLine("## System Details")
            appendLine("- **Host OS**: Android (${deviceInfo.androidVersion}, API ${deviceInfo.apiLevel})")
            appendLine("- **Device**: ${deviceInfo.manufacturer} ${deviceInfo.model}")
            appendLine("- **Architecture**: arm64-v8a (aarch64)")
            appendLine("- **Container**: Debian (installed via proot-distro)")
            appendLine("- **Shell**: bash (in Debian) / bash (in Termux)")
            appendLine()
            
            appendLine("## File System Layout")
            appendLine()
            appendLine("### Inside Debian (proot)")
            appendLine("```")
            appendLine("/                    → Debian root filesystem")
            appendLine("/root                → Home directory (root in proot)")
            appendLine("/sdcard              → Android shared storage (/storage/emulated/0)")
            appendLine("/storage/emulated/0  → Same as /sdcard")
            appendLine("```")
            appendLine()
            appendLine("### Inside Termux (host)")
            appendLine("```")
            appendLine("/data/data/com.termux/files/usr   → Termux prefix (binaries, libs)")
            appendLine("/data/data/com.termux/files/home  → Termux home directory")
            appendLine("/sdcard                            → Android shared storage")
            appendLine("```")
            appendLine()
            
            appendLine("## Package Management")
            appendLine()
            appendLine("### In Termux (outside Debian)")
            appendLine("- Use `pkg install <package>` (wrapper around apt)")
            appendLine("- Do NOT use `apt` directly in Termux — use `pkg` instead")
            appendLine("- Available repos: main, root, x11, science, game, unstable")
            appendLine()
            appendLine("### In Debian (inside proot)")
            appendLine("- Use `apt-get install <package>` or `apt install <package>`")
            appendLine("- Standard Debian packages are available")
            appendLine("- Run `apt update` before installing packages")
            appendLine()
            
            appendLine("## Entering Debian from Termux")
            appendLine("```bash")
            appendLine("proot-distro login debian          # Enter Debian shell")
            appendLine("proot-distro login debian -- bash  # Run specific command")
            appendLine("proot-distro login debian -- apt update  # One-off command")
            appendLine("```")
            appendLine()
            
            appendLine("## MCP (Model Context Protocol) Bridge")
            appendLine()
            appendLine("You have access to **termux-mcp**, which provides the following tools:")
            appendLine()
            appendLine("### Shell Commands")
            appendLine("- `execute_command`: Run any shell command in Termux or Debian")
            appendLine("- `execute_in_debian`: Run a command directly inside Debian proot")
            appendLine()
            appendLine("### File Operations")
            appendLine("- `read_file`: Read contents of a file")
            appendLine("- `write_file`: Write/create a file")
            appendLine("- `list_directory`: List files in a directory")
            appendLine("- `delete_file`: Remove a file")
            appendLine("- `move_file`: Move/rename a file")
            appendLine()
            appendLine("### Android Integration")
            appendLine("- `take_screenshot`: Capture the current screen")
            appendLine("- `send_notification`: Send an Android notification")
            appendLine("- `get_clipboard`: Read clipboard contents")
            appendLine("- `set_clipboard`: Write to clipboard")
            appendLine("- `vibrate`: Trigger device vibration")
            appendLine("- `get_battery_status`: Get battery level and charging state")
            appendLine("- `get_device_info`: Get device specifications")
            appendLine("- `open_url`: Open a URL in the browser")
            appendLine("- `share_text`: Share text via Android share sheet")
            appendLine()
            appendLine("### Network")
            appendLine("- `http_request`: Make HTTP requests (GET, POST, etc.)")
            appendLine("- `get_network_info`: Get current network status")
            appendLine()
            
            appendLine("## Limitations & Constraints")
            appendLine()
            appendLine("1. **No root access**: This is a proot environment, not true virtualization.")
            appendLine("2. **No Docker**: Container-in-container is not supported.")
            appendLine("3. **No systemd**: Debian runs under proot — use direct process management.")
            appendLine("4. **Network**: Internet access works, but some ports may be restricted.")
            appendLine("5. **Performance**: CPU-intensive tasks may be slower than on a PC.")
            appendLine("6. **Memory**: Limited by the device's available RAM.")
            appendLine("7. **No kernel modules**: Cannot load custom kernel modules.")
            appendLine("8. **File permissions**: proot fakes root; real permissions depend on Android.")
            appendLine("9. **Storage**: Shared storage (/sdcard) uses Android's scoped storage rules.")
            appendLine()
            
            appendLine("## Development Workflows")
            appendLine()
            appendLine("### Web Development")
            appendLine("```bash")
            appendLine("# In Debian:")
            appendLine("apt install nodejs npm python3 python3-pip")
            appendLine("npm install -g yarn")
            appendLine("# Run dev server")
            appendLine("npm run dev")
            appendLine("```")
            appendLine()
            appendLine("### Python Development")
            appendLine("```bash")
            appendLine("# In Debian:")
            appendLine("apt install python3 python3-pip python3-venv")
            appendLine("python3 -m venv myenv")
            appendLine("source myenv/bin/activate")
            appendLine("pip install -r requirements.txt")
            appendLine("```")
            appendLine()
            appendLine("### Git Workflow")
            appendLine("```bash")
            appendLine("# Configure git (in Debian or Termux)")
            appendLine("git config --global user.name 'Your Name'")
            appendLine("git config --global user.email 'your@email.com'")
            appendLine("# Clone and work")
            appendLine("git clone https://github.com/user/repo.git")
            appendLine("cd repo && git checkout -b feature-branch")
            appendLine("```")
            appendLine()
            
            appendLine("## Important Notes")
            appendLine()
            appendLine("1. Always prefer working inside **Debian** for development tasks — it has the full Linux ecosystem.")
            appendLine("2. Use **Termux** only for Android-specific tasks or when you need Termux-exclusive packages.")
            appendLine("3. Save important files to `/sdcard/Projects/` so they're accessible from Android file managers.")
            appendLine("4. For persistent background tasks, use `nohup` or `screen`/`tmux` instead of systemd services.")
            appendLine("5. If a package is not found in Debian repos, try the Termux repos as a fallback.")
            appendLine("6. When running servers (dev servers, etc.), bind to `0.0.0.0` and access via `localhost:PORT`.")
        }
    }

    /**
     * Generates a compact version of the prompt for token-constrained contexts.
     */
    fun generateCompactPrompt(context: Context): String {
        val deviceInfo = getDeviceInfo(context)
        return """
            |You are an AI assistant running in Debian (proot) inside Termux on Android (${deviceInfo.manufacturer} ${deviceInfo.model}, Android ${deviceInfo.androidVersion}).
            |
            |Environment:
            |- Debian via proot-distro (full apt, systemd not available)
            |- Termux host at /data/data/com.termux/files/ (use pkg, not apt)
            |- Android storage at /sdcard/ (scoped storage rules apply)
            |- Architecture: arm64 (aarch64), no root, no Docker
            |
            |Use termux-mcp tools for Android integration (screenshots, notifications, clipboard).
            |For development, prefer Debian over Termux.
            |Save important files to /sdcard/Projects/ for easy access.
        """.trimMargin()
    }

    private fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            apiLevel = android.os.Build.VERSION.SDK_INT
        )
    }

    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val apiLevel: Int
    )
}
