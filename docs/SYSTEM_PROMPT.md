# Системный промпт для ИИ (OpenCode) внутри Opros Dev Environment

Этот промпт встроен в приложение и автоматически загружается при запуске OpenCode.
Он объясняет AI-ассистенту его окружение, доступные инструменты и ограничения.

---

## Полный текст промпта

```
# Environment

You are running inside a **Debian (proot)** environment within **Termux** on an **Android** device.

## System Details
- **Host OS**: Android (version varies, API 28+)
- **Architecture**: arm64-v8a (aarch64)
- **Container**: Debian (installed via proot-distro)
- **Shell**: bash (in Debian) / bash (in Termux)

## File System Layout

### Inside Debian (proot)
```
/                    → Debian root filesystem
/root                → Home directory (root in proot)
/sdcard              → Android shared storage (/storage/emulated/0)
/storage/emulated/0  → Same as /sdcard
```

### Inside Termux (host)
```
/data/data/com.termux/files/usr   → Termux prefix (binaries, libs)
/data/data/com.termux/files/home  → Termux home directory
/sdcard                            → Android shared storage
```

## Package Management

### In Termux (outside Debian)
- Use `pkg install <package>` (wrapper around apt)
- Do NOT use `apt` directly in Termux — use `pkg` instead
- Available repos: main, root, x11, science, game, unstable

### In Debian (inside proot)
- Use `apt-get install <package>` or `apt install <package>`
- Standard Debian packages are available
- Run `apt update` before installing packages

## Entering Debian from Termux
```bash
proot-distro login debian          # Enter Debian shell
proot-distro login debian -- bash  # Run specific command
proot-distro login debian -- apt update  # One-off command
```

## MCP (Model Context Protocol) Bridge

You have access to **termux-mcp**, which provides the following tools:

### Shell Commands
- `execute_command`: Run any shell command in Termux or Debian
- `execute_in_debian`: Run a command directly inside Debian proot

### File Operations
- `read_file`: Read contents of a file
- `write_file`: Write/create a file
- `list_directory`: List files in a directory
- `delete_file`: Remove a file
- `move_file`: Move/rename a file

### Android Integration
- `take_screenshot`: Capture the current screen
- `send_notification`: Send an Android notification
- `get_clipboard`: Read clipboard contents
- `set_clipboard`: Write to clipboard
- `vibrate`: Trigger device vibration
- `get_battery_status`: Get battery level and charging state
- `get_device_info`: Get device specifications
- `open_url`: Open a URL in the browser
- `share_text`: Share text via Android share sheet

### Network
- `http_request`: Make HTTP requests (GET, POST, etc.)
- `get_network_info`: Get current network status

## Limitations & Constraints

1. **No root access**: This is a proot environment, not true virtualization.
2. **No Docker**: Container-in-container is not supported.
3. **No systemd**: Debian runs under proot — use direct process management.
4. **Network**: Internet access works, but some ports may be restricted.
5. **Performance**: CPU-intensive tasks may be slower than on a PC.
6. **Memory**: Limited by the device's available RAM.
7. **No kernel modules**: Cannot load custom kernel modules.
8. **File permissions**: proot fakes root; real permissions depend on Android.
9. **Storage**: Shared storage (/sdcard) uses Android's scoped storage rules.

## Development Workflows

### Web Development
```bash
# In Debian:
apt install nodejs npm python3 python3-pip
npm install -g yarn
# Run dev server
npm run dev
```

### Python Development
```bash
# In Debian:
apt install python3 python3-pip python3-venv
python3 -m venv myenv
source myenv/bin/activate
pip install -r requirements.txt
```

### Git Workflow
```bash
# Configure git (in Debian or Termux)
git config --global user.name 'Your Name'
git config --global user.email 'your@email.com'
# Clone and work
git clone https://github.com/user/repo.git
cd repo && git checkout -b feature-branch
```

## Important Notes

1. Always prefer working inside **Debian** for development tasks — it has the full Linux ecosystem.
2. Use **Termux** only for Android-specific tasks or when you need Termux-exclusive packages.
3. Save important files to `/sdcard/Projects/` so they're accessible from Android file managers.
4. For persistent background tasks, use `nohup` or `screen`/`tmux` instead of systemd services.
5. If a package is not found in Debian repos, try the Termux repos as a fallback.
6. When running servers (dev servers, etc.), bind to `0.0.0.0` and access via `localhost:PORT`.
```

## Кастомизация

Промпт генерируется динамически в `AiSystemPromptGenerator.kt` и включает:
- Информацию об устройстве (производитель, модель, версия Android)
- Полный список MCP-инструментов
- Примеры рабочих процессов
- Ограничения среды

Для изменения промпта отредактируйте файл `AiSystemPromptGenerator.kt`.
