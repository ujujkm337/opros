# Архитектура Opros

## Концепция

Opros — это **единый APK**, который содержит:
1. Встроенный Termux bootstrap (rootfs)
2. OpenCode AI-агент (native aarch64)
3. Настроенный DeepSeek V4.1 Flash Free
4. Терминал с UI

Всё работает из коробки — не нужно устанавливать Termux отдельно.

## Почему applicationId = com.termux

Termux bootstrap hardcoded на путь `/data/data/com.termux/files/usr`.

Этот путь зашит в:
- Динамический линкер (ld-linux-aarch64.so.1)
- libc и другие библиотеки
- Конфигурационные файлы пакетов

**Единственный способ** встроить Termux в APK — использовать `applicationId = com.termux`.

Альтернатива — пересобрать bootstrap с другим prefix, но это требует:
- Cross-compile toolchain
- Пересборку всех пакетов
- Постоянное поддержание форка

Мы выбрали путь с `com.termux` (как проект Cody).

## Схема работы

```
App Launch
    │
    ▼
MainActivity.onCreate()
    │
    ├── Bootstrap установлен?
    │   ├── Нет → SetupActivity
    │   │         ├── Распаковка bootstrap-arm64.zip из assets
    │   │         ├── Установка прав (chmod +x)
    │   │         ├── pkg update && pkg install ripgrep curl
    │   │         ├── Установка OpenCode (guysoft .deb)
    │   │         └── Запись opencode.json с DeepSeek
    │   │
    │   └── Да → TermuxService
    │            ├── Создание TerminalSession (bash)
    │            ├── Подключение к TerminalView
    │            └── Готов к работе
    │
    ▼
User taps "OpenCode"
    │
    ▼
TerminalSession.write("opencode\n")
    │
    ▼
OpenCode TUI запускается в терминале
    ├── Загружает ~/.config/opencode/opencode.json
    ├── Модель: opencode/deepseek-v4.1-flash-free
    └── AI-агент готов к работе
```

## Компоненты

### BootstrapManager
- Распаковывает `assets/bootstrap-arm64.zip` в `/data/data/com.termux/files/`
- Устанавливает права на бинарники
- Создаёт .bashrc и .profile

### TermuxService
- Foreground service для поддержания сессии
- Управление терминальными сессиями
- Проверка установки OpenCode и DeepSeek

### TerminalSession
- Обёртка над Process (bash)
- Чтение stdout/stderr
- Запись stdin
- Поддержка escape-последовательностей

### TerminalView
- ScrollView с TextView
- Отображение вывода терминала
- Обработка клавиатуры (Enter, Tab, arrows)

### SetupActivity
- Пошаговый мастер:
  1. Bootstrap (распаковка)
  2. OpenCode (guysoft native .deb)
  3. DeepSeek (запись конфига)

## OpenCode: Нативная установка

Используем **guysoft/opencode-termux**:
- Cross-compiled Bun + WebKit/JSC для Android aarch64
- Нативный бинарник (не glibc, не proot)
- Размер: ~66 MB
- Устанавливается через .deb пакет

## DeepSeek V4.1 Flash Free

Конфигурация записывается автоматически:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "model": "opencode/deepseek-v4.1-flash-free"
}
```

При первом запуске OpenCode:
```
/connect  → выбрать OpenCode (Zen)
```

## Ограничения

- **Конфликт с Termux**: нельзя установить Opros и оригинальный Termux одновременно
- **Размер APK**: ~150-250 MB (bootstrap + OpenCode)
- **Нет root**: proot-режим (хотя нам это и не нужно)
- **DeepSeek Free**: "limited time" (дата окончания не опубликована)
