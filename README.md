# Opros Launcher

**Android-лаунчер для OpenCode AI + DeepSeek V4.1 Flash Free**

Работает вместе с **оригинальным Termux** (F-Droid) — без форка, без `applicationId=com.termux`, без костылей.

## 🧠 Как это работает

```
┌──────────────────────────┐
│   Opros Launcher (APK)   │  ← Наше приложение
│   applicationId:          │
│   dev.opros.launcher     │
│                           │
│   • UI (кнопки, статус)   │
│   • Setup wizard          │
│   • Конфиг DeepSeek       │
└───────────┬──────────────┘
            │ RUN_COMMAND intent
            │ (официальный API Termux)
            ▼
┌──────────────────────────┐
│   Termux (F-Droid)       │  ← Оригинал, НЕ форк
│   com.termux              │
│                           │
│   • bash, pkg, git        │
│   • OpenCode (native      │
│     aarch64, без proot)   │
│   • DeepSeek V4.1         │
│     Flash Free ($0)       │
└──────────────────────────┘
```

### Ключевые решения:

| Проблема | Решение |
|----------|---------|
| OpenCode не ставится в Termux (баг #21043) | Используем **guysoft/opencode-termux** — нативный aarch64 бинарник |
| Нужен ли форк Termux? | **НЕТ** — общаемся через `RUN_COMMAND` intent |
| DeepSeek бесплатно? | **Да** — модель `opencode/deepseek-v4.1-flash-free`, $0/токен |
| Нужен proot/Debian? | **НЕТ** — OpenCode работает нативно в Termux |

## 📋 Требования

- **Android 8.0+** (arm64/aarch64)
- **Termux из F-Droid** (НЕ из Google Play — там устаревшая версия!)
  - F-Droid: https://f-droid.org/packages/com.termux/
  - GitHub: https://github.com/termux/termux-app/releases
- Без root

## 🚀 Быстрый старт

### 1. Установите Termux (из F-Droid!)

### 2. Соберите Opros APK

```bash
git clone https://github.com/ujujkm337/opros.git
cd opros
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### 3. Установите Opros

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Запустите Setup

Откройте Opros → нажмите **"⚙ Setup"**

Setup автоматически:
1. Включит `allow-external-apps` в Termux
2. Установит OpenCode (нативный aarch64 бинарник)
3. Настроит DeepSeek V4.1 Flash Free

### 5. Запустите OpenCode

Нажмите **"🚀 Запустить OpenCode"** — Termux откроется с AI-ассистентом.

## 🤖 DeepSeek V4.1 Flash Free

| Параметр | Значение |
|----------|----------|
| Model ID | `opencode/deepseek-v4.1-flash-free` |
| Стоимость | **$0** (бесплатно, ограниченное время) |
| Контекст | 200K токенов |
| Выход | 32K токенов |
| Провайдер | OpenCode Zen |
| Данные | Zero-retention, не используется для обучения |

При первом запуске OpenCode:
```
/connect    → выбрать провайдер OpenCode (Zen)
/models     → выбрать DeepSeek V4.1 Flash Free
```

## 📂 Структура проекта

```
opros/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/dev/opros/launcher/
│       │   ├── MainActivity.kt          # Главный экран
│       │   ├── SetupActivity.kt         # Мастер установки
│       │   ├── TerminalActivity.kt      # Просмотр терминала
│       │   └── TermuxCommandExecutor.kt # RUN_COMMAND intent API
│       └── res/
│           ├── layout/
│           ├── values/
│           ├── drawable/
│           └── mipmap/
├── docs/
│   ├── ARCHITECTURE.md
│   └── DEEPSEEK_SETUP.md
├── scripts/
│   └── generate-icons.py
└── README.md
```

## ⚙ Как работает RUN_COMMAND

Opros общается с Termux через официальный API:

```kotlin
val intent = Intent("com.termux.RUN_COMMAND").apply {
    setClassName("com.termux", "com.termux.app.RunCommandService")
    putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
    putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", "opencode"))
    putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
}
startForegroundService(intent)
```

Для работы нужен `allow-external-apps = true` в `~/.termux/termux.properties`
(Setup включает это автоматически).

## ⚠️ Ограничения

- Termux должен быть из F-Droid/GitHub (Play Store версия устарела)
- OpenCode TUI отображается в Termux, а не внутри Opros
- DeepSeek V4.1 Flash Free работает только внутри OpenCode (не через API)
- Бесплатный период DeepSeek не имеет опубликованной даты окончания

## 📄 Лицензия

MIT License
