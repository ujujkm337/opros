# Opros Dev Environment

**Автономная среда мобильной разработки на Android** — объединяет Termux, Debian (proot) и OpenCode AI в единое Android-приложение.

![Android](https://img.shields.io/badge/Android-API%2028+-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 🏗 Архитектура

```
┌─────────────────────────────────────────────────┐
│          Android App (Kotlin)                     │
│          applicationId = com.termux               │
│                                                   │
│  ┌─────────────────┐  ┌────────────────────────┐ │
│  │   Terminal View  │  │   Action Buttons        │ │
│  │   (emulator)     │  │   OpenCode|Debian|Termux│ │
│  └────────┬─────────┘  └────────────────────────┘ │
│           │                                        │
│  ┌────────▼─────────────────────────────────────┐ │
│  │         TermuxService (foreground)            │ │
│  │         - PTY management                       │ │
│  │         - Process lifecycle                    │ │
│  └────────┬─────────────────────────────────────┘ │
│           │                                        │
│  ┌────────▼─────────────────────────────────────┐ │
│  │         Termux Bootstrap (rootfs)             │ │
│  │  ┌──────────────────────────────────────────┐ │ │
│  │  │   /data/data/com.termux/files/usr/       │ │ │
│  │  │   ├── bin/ (bash, pkg, node, python...)  │ │ │
│  │  │   ├── lib/                               │ │ │
│  │  │   └── ...                                │ │ │
│  │  └──────────────────────────────────────────┘ │ │
│  │                                               │ │
│  │  ┌──────────────────────────────────────────┐ │ │
│  │  │   Debian (proot-distro)                  │ │ │
│  │  │   ├── Full apt package manager           │ │ │
│  │  │   ├── OpenCode AI assistant              │ │ │
│  │  │   └── termux-mcp bridge                  │ │ │
│  │  └──────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

## 📋 Что внутри

### Слой 1: Android-приложение (Kotlin)
- `applicationId = com.termux` — необходимо для работы Termux bootstrap
- Foreground service для поддержания терминала
- UI с терминалом, кнопками и клавиатурными дополнениями

### Слой 2: Termux Bootstrap
- Встроенный rootfs (bootstrap-arm64.zip в assets)
- При первом запуске автоматически распаковывается
- Включает: bash, pkg, nodejs, python, git и базовые утилиты

### Слой 3: Debian (proot)
- Устанавливается через `proot-distro install debian`
- Полноценный Debian с apt
- OpenCode AI устанавливается внутри Debian
- MCP-мост (termux-mcp) для доступа ИИ к системе

### Слой 4: OpenCode + MCP
- AI-ассистент для помощи в разработке
- MCP (Model Context Protocol) предоставляет инструменты:
  - Выполнение команд в Termux и Debian
  - Работа с файлами
  - Доступ к Android API (скриншоты, уведомления, буфер обмена)

## 🚀 Быстрый старт

### Требования
- Android 9.0 (API 28) или выше
- Архитектура: arm64-v8a
- 2+ GB свободного места
- Без root

### Сборка APK

```bash
# 1. Клонируйте репозиторий
git clone https://github.com/ujujkm337/opros.git
cd opros

# 2. Скачайте Termux bootstrap
bash scripts/download-bootstrap.sh

# 3. Соберите APK (нужен Android SDK + JDK 17)
./gradlew assembleDebug

# 4. APK будет в app/build/outputs/apk/debug/
```

Или откройте проект в Android Studio и нажмите Build → Build APK.

### Установка

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📂 Структура проекта

```
opros/
├── app/
│   ├── build.gradle.kts          # Конфигурация сборки
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/termux/
│       │   ├── MainActivity.kt         # Главный экран
│       │   ├── SetupActivity.kt        # Мастер первого запуска
│       │   ├── TermuxService.kt        # Foreground service
│       │   ├── TermuxInstaller.kt      # Установка bootstrap
│       │   ├── TerminalEmulator.kt     # Эмуляция терминала
│       │   ├── TerminalView.kt         # View для терминала
│       │   └── AiSystemPromptGenerator.kt  # Генератор системного промпта
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   └── activity_setup.xml
│           ├── values/
│           ├── drawable/
│           └── mipmap/
├── mcp-bridge/
│   ├── termux_mcp.py             # MCP-сервер для Termux
│   ├── setup.py
│   └── README.md
├── scripts/
│   ├── download-bootstrap.sh     # Загрузка Termux bootstrap
│   └── setup-environment.sh      # Пост-инсталляционный скрипт
├── docs/
│   ├── ARCHITECTURE.md           # Детальное описание архитектуры
│   ├── BUILD.md                  # Инструкция по сборке
│   └── SYSTEM_PROMPT.md          # Системный промпт для ИИ
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🤖 Системный промпт для ИИ

Встроенный в приложение системный промпт объясняет OpenCode:

- Где он работает (Debian proot внутри Termux на Android)
- Какие инструменты доступны (apt, pkg, MCP endpoints)
- Как работать с файловой системой
- Какие ограничения существуют (нет root, нет Docker, нет systemd)

Полный текст: [docs/SYSTEM_PROMPT.md](docs/SYSTEM_PROMPT.md)

## ⚠️ Ограничения

| Ограничение | Причина |
|-------------|---------|
| Нет root | proot, не виртуализация |
| Нет Docker | Container-in-container невозможен |
| Нет systemd | proot не поддерживает init-системы |
| Нет kernel modules | Доступ к ядру Android ограничен |
| Ограниченная производительность | ARM + proot overhead |

## 📦 Релизы

Готовый APK доступен в [GitHub Releases](https://github.com/ujujkm337/opros/releases).

## 📄 Лицензия

MIT License. См. [LICENSE](LICENSE).

## 🙏 Благодарности

- [Termux](https://github.com/termux/termux-app) — за Termux и bootstrap
- [proot-distro](https://github.com/termux/proot-distro) — за Linux-дистрибутивы в Termux
- [OpenCode](https://opencode.ai) — за AI-ассистент
- [Cody](https://github.com/nicehash/Cody) — за идею applicationId=com.termux
