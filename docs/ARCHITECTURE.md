# Архитектура Opros Dev Environment

## Обзор

Opros — это Android-приложение, которое объединяет три слоя:

1. **Termux** — Linux-окружение на Android (без root)
2. **Debian** — полноценный Linux-дистрибутив через proot
3. **OpenCode** — AI-ассистент с доступом к системе через MCP

## Почему applicationId = com.termux?

Termux bootstrap использует захардкоженный путь `/data/data/com.termux/files/usr`.
Этот путь зашит в скомпилированные бинарники bootstrap-а (ld-linux, libc и т.д.).

Есть два варианта:
1. **Наш подход**: Сделать `applicationId = com.termux`, чтобы bootstrap работал из коробки.
2. **Альтернатива**: Пересобрать bootstrap с другим prefix — трудоёмко и требует cross-compile toolchain.

Мы выбрали вариант 1 (как проект Cody), потому что:
- Не требует перекомпиляции bootstrap
- Совместим с готовыми bootstrap-архивами от Termux
- Проще поддерживать

**Минус**: нельзя опубликовать в Play Store (конфликт ID).

## Схема данных

```
/data/data/com.termux/
├── files/
│   ├── usr/                    ← Termux prefix (bootstrap)
│   │   ├── bin/                ← Исполняемые файлы
│   │   │   ├── bash
│   │   │   ├── pkg
│   │   │   ├── node
│   │   │   ├── python3
│   │   │   ├── proot-distro
│   │   │   └── ...
│   │   ├── lib/                ← Библиотеки
│   │   ├── etc/                ← Конфигурация
│   │   ├── var/
│   │   │   └── lib/proot-distro/
│   │   │       └── installed-rootfs/
│   │   │           └── debian/ ← Debian rootfs
│   │   │               ├── bin/
│   │   │               ├── usr/
│   │   │               ├── root/
│   │   │               │   ├── Projects/
│   │   │               │   └── .config/opencode/
│   │   │               │       ├── config.json
│   │   │               │       └── system-prompt.txt
│   │   │               └── ...
│   │   └── tmp/
│   └── home/                   ← Termux home
│       ├── .bashrc
│       └── .profile
├── cache/
└── shared_prefs/
```

## Потоки выполнения

### Первый запуск

```
App Launch
    │
    ▼
Check first_run flag
    │
    ├── false → MainActivity (normal)
    │
    └── true → SetupActivity
                │
                ▼
        Install Bootstrap
        (extract zip from assets)
                │
                ▼
        pkg update
                │
                ▼
        pkg install proot-distro nodejs git curl python
                │
                ▼
        proot-distro install debian
                │
                ▼
        Install OpenCode (npm / curl)
                │
                ▼
        Install termux-mcp (pip / npm)
                │
                ▼
        Write configs
                │
                ▼
        MainActivity
```

### Сессия OpenCode

```
User taps "OpenCode"
    │
    ▼
TermuxService.executeCommand(
    "proot-distro login debian -- opencode"
)
    │
    ▼
Debian shell starts
    │
    ▼
OpenCode loads config from ~/.config/opencode/config.json
    │
    ▼
MCP bridge (termux-mcp) starts as child process
    │
    ▼
OpenCode ↔ MCP ↔ Termux/Android
    (bidirectional JSON-RPC over stdio)
```

## MCP Bridge

MCP-мост реализует Model Context Protocol, давая AI доступ к:

| Категория | Инструменты |
|-----------|-------------|
| Shell | execute_command, execute_in_debian |
| Файлы | read_file, write_file, list_directory, delete_file, move_file |
| Android | screenshot, notification, clipboard, vibrate, battery, device_info |
| Сеть | http_request, network_info |

Протокол: JSON-RPC 2.0 over stdio (stdin/stdout).

## Компоненты UI

### MainActivity
- **TerminalView** — отображение вывода терминала
- **Action buttons** — OpenCode, Debian, Termux, AI Info
- **Keyboard bar** — TAB, CTRL, ESC, /, -, |
- **Status bar** — текущее состояние

### SetupActivity
- Пошаговый wizard установки
- Progress bar
- Real-time log

## Безопасность

- Приложение НЕ требует root
- Все файлы в приватной директории app (кроме /sdcard)
- MCP-мост работает только локально (stdio, без сети)
- Нет удалённого доступа к shell

## Производительность

| Операция | Ожидаемое время |
|----------|-----------------|
| Установка bootstrap | 10-30 секунд |
| pkg update | 30-60 секунд |
| proot-distro install debian | 2-5 минут |
| OpenCode startup | 5-15 секунд |
| Команды в proot | +50-100% overhead |

## Альтернативные подходы

### 1. Использование WebView + SSH
Вместо встроенного терминала — WebView с веб-терминалом (xterm.js), подключающийся к локальному SSH-серверу.

**Плюсы**: Лучшая эмуляция терминала.
**Минусы**: Больше зависимостей, сложнее настройка.

### 2. Использование Termux:Widget
Запуск скриптов через виджеты Termux.

**Плюсы**: Проще архитектура.
**Минусы**: Нет интерактивного терминала.

### 3. Полная пересборка Termux
Форк Termux с другим applicationId и prefix.

**Плюсы**: Нет конфликта ID.
**Минусы**: Требует cross-compile toolchain, сложно в поддержке.
