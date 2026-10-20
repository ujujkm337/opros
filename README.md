# Opros — OpenCode + DeepSeek на Android

**Единый APK** с встроенным Termux + OpenCode + DeepSeek V4.1 Flash Free.

Без установки Termux отдельно, без настройки allow-external-apps, без обходов.
Просто установи APK и нажми "OpenCode" — AI-агент с DeepSeek запустится.

## Как это работает

```
┌─────────────────────────────────────┐
│        Opros APK (единый файл)       │
│        applicationId = com.termux    │
│                                      │
│  ┌────────────────────────────────┐  │
│  │  Встроенный Termux bootstrap   │  │
│  │  (rootfs, bash, pkg, node...)  │  │
│  │                                │  │
│  │  OpenCode (native aarch64)     │  │
│  │  └─ DeepSeek V4.1 Flash Free  │  │
│  │     ($0, 200K контекст)        │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │  Терминал + UI                 │  │
│  │  [OpenCode] [Shell] [Статус]  │  │
│  │  [TAB] [CTRL] [ESC] [/] [|]  │  │
│  └────────────────────────────────┘  │
└─────────────────────────────────────┘
```

## Установка

### Вариант 1: Готовый APK (когда будет собран)

```bash
adb install opros-v2.0.0.apk
```

### Вариант 2: Сборка из исходников

```bash
# 1. Клонировать
git clone https://github.com/ujujkm337/opros.git
cd opros

# 2. Скачать Termux bootstrap
bash scripts/download-bootstrap.sh

# 3. Собрать (нужен JDK 17 + Android SDK)
./gradlew assembleDebug

# 4. Установить
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Первый запуск

1. Открой Opros
2. Нажми "▶ Начать установку" в Setup-мастере
3. Подожди — установится Termux + OpenCode + DeepSeek
4. Нажми "🚀 Запустить"
5. Нажми "🤖 OpenCode" — AI-агент запустится!

## DeepSeek V4.1 Flash Free

- **Модель**: `opencode/deepseek-v4.1-flash-free`
- **Цена**: $0 (бесплатно, ограниченное время)
- **Контекст**: 200K токенов
- **Провайдер**: OpenCode Zen (zero-retention)

## Почему applicationId = com.termux?

Termux bootstrap использует захардкоженный путь `/data/data/com.termux/files/usr`.
Это зашито в бинарниках bootstrap (ld-linux, libc).

Единственный способ встроить Termux в APK — использовать этот applicationId.
Без этого bootstrap не сможет найти свои файлы.

## Структура проекта

```
opros/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/termux/
│       │   ├── MainActivity.kt          # Главный экран
│       │   ├── SetupActivity.kt         # Мастер установки
│       │   ├── TermuxService.kt         # Сервис Termux
│       │   ├── BootstrapManager.kt      # Установка bootstrap
│       │   ├── TerminalSession.kt       # Терминальная сессия
│       │   └── TerminalView.kt          # View терминала
│       ├── res/
│       └── assets/
│           └── bootstrap-arm64.zip      # ← Скачать перед сборкой!
├── scripts/
│   ├── download-bootstrap.sh           # Скрипт загрузки bootstrap
│   └── generate-icons.py
└── docs/
    ├── ARCHITECTURE.md
    └── DEEPSEEK_SETUP.md
```

## Требования для сборки

- JDK 17+
- Android SDK (API 34)
- Gradle 8.2+
- ~150 MB для bootstrap

## Лицензия

MIT
