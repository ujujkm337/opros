# Архитектура Opros Launcher

## Почему НЕ applicationId=com.termux

Ранний подход (в v0.1) предполагал форк Termux с `applicationId=com.termux`,
чтобы встроить bootstrap. Это **плохое решение**:

1. Конфликт с оригинальным Termux (нельзя установить оба)
2. Нельзя опубликовать в Play Store
3. Сложно обновлять bootstrap
4. Юридически спорно

## Правильный подход: RUN_COMMAND Intent

Termux предоставляет официальный API для внешних приложений:

```
com.termux.RUN_COMMAND
```

Это Intent-сервис (`com.termux.app.RunCommandService`), который позволяет
любому приложению выполнять команды внутри Termux.

**Требование**: Пользователь должен один раз включить `allow-external-apps`
в настройках Termux (Opros Setup делает это автоматически).

## Схема работы

```
Opros Launcher                    Termux (F-Droid)
dev.opros.launcher                com.termux
┌─────────────────┐              ┌──────────────────┐
│                 │   Intent     │                  │
│  UI Buttons     │─────────────→│ RunCommandService│
│  • Launch       │ RUN_COMMAND  │                  │
│  • Setup        │              │  → bash -c       │
│  • Status       │              │    "opencode"    │
│                 │              │                  │
│  TermuxCommand  │              │  Termux Session  │
│  Executor       │              │  ┌────────────┐  │
│                 │              │  │ OpenCode   │  │
│  File checks:   │  (read /data │  │ DeepSeek   │  │
│  ~/.config/     │   direct)    │  │ Flash Free │  │
│  opencode/      │←─────────────│  └────────────┘  │
│                 │              │                  │
└─────────────────┘              └──────────────────┘
```

## OpenCode: Нативная установка

### Почему не npm install?

OpenCode баг #21043: установщик определяет Termux как обычный Linux
и ставит glibc-бинарник, который не работает на Android Bionic libc.

### Решение: guysoft/opencode-termux

Проект cross-компилирует Bun + WebKit/JSC для Android aarch64:
- Нативный бинарник (не glibc, не proot)
- Работает прямо в Termux
- Устанавливается через .deb/.pkg.tar.xz
- Размер: ~66 MB (UPX compressed)

### Альтернативные подходы (НЕ используем)

| Подход | Почему не подходит |
|--------|-------------------|
| proot-distro + Debian | Медленно (ptrace overhead), избыточно |
| npm install -g opencode-ai | Баг #21043, ставит не тот бинарник |
| curl install скрипт | Тоже ставит glibc бинарник |
| sebastianl1/opencode-termux | glibc wrapper, менее надёжный |

## DeepSeek V4.1 Flash Free

OpenCode Zen предоставляет бесплатный доступ:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "model": "opencode/deepseek-v4.1-flash-free"
}
```

**Важно**: Эта модель работает ТОЛЬКО внутри OpenCode TUI.
Нельзя использовать через обычный API-вызов (вернёт 403).

## Файлы конфигурации

Opros записывает в Termux:

```
~/.termux/termux.properties       → allow-external-apps = true
~/.config/opencode/opencode.json  → модель DeepSeek по умолчанию
```

## Компоненты

### MainActivity
- Проверка установки Termux
- Кнопка запуска OpenCode
- Статус системы
- Навигация в Setup

### SetupActivity
- Пошаговая установка:
  1. Включение allow-external-apps
  2. Установка OpenCode (guysoft .deb)
  3. Конфигурация DeepSeek

### TermuxCommandExecutor
- Обёртка над RUN_COMMAND intent
- Проверка файлов в /data/data/com.termux/
- Генерация bash-скриптов установки
