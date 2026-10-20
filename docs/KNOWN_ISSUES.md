# Известные подводные камни

## 1. Конфликт applicationId

**Проблема**: Использование `applicationId = com.termux` означает, что приложение
не может быть установлено одновременно с официальным Termux.

**Решение**: Документировать это ограничение. Пользователь должен удалить
официальный Termux перед установкой Opros (или наоборот).

## 2. Размер bootstrap

**Проблема**: Bootstrap-arm64.zip ~130 MB, что делает APK очень большим.

**Решения**:
- Загружать bootstrap при первом запуске из интернета (дополнительная библиотека)
- Использовать минимальный bootstrap и докачивать пакеты
- Использовать App Bundle (AAB) для Play Store

## 3. Ограничения Android 10+ (Scoped Storage)

**Проблема**: Начиная с Android 10, прямой доступ к `/sdcard` ограничен.

**Решение**:
- `requestLegacyExternalStorage="true"` в AndroidManifest (работает до API 29)
- Для API 30+: использовать Storage Access Framework (SAF)
- `MANAGE_EXTERNAL_STORAGE` permission (требует одобрения Google)

## 4. exec() из data-директории

**Проблема**: Начиная с Android 10 (API 29), исполнение файлов из W^X-защищённых
директорий ограничено. Флаг `android:extractNativeLibs="true"` обязателен.

**Решение**: `targetSdk=34` + `extractNativeLibs=true` + `minSdk=28`.

## 5. Производительность proot

**Проблема**: proot добавляет значительный overhead к каждому системному вызову
(через ptrace). Команды в Debian могут работать в 2-3x медленнее.

**Решения**:
- Предпочитать Termux-пакеты когда возможно
- Избегать операций с большим количеством файловых вызовов
- Использовать tmpfs для временных файлов

## 6. OpenCode и glibc

**Проблема**: OpenCode может быть скомпилирован с glibc, а Termux использует Bionic libc.

**Решения**:
- Устанавливать OpenCode внутри Debian (там есть glibc)
- Использовать npm-пакет opencode-termux (скомпилирован под Bionic)
- Использовать glibc-прослойку (termux-glibc)

## 7. OOM (Out of Memory)

**Проблема**: Android может убить процесс Termux при нехватке памяти.

**Решения**:
- Foreground service с высоким приоритетом
- `WAKE_LOCK` permission
- Рекомендовать пользователю отключить battery optimization
- Не запускать тяжёлые процессы параллельно

## 8. termux-mcp и Termux:API

**Проблема**: Некоторые MCP-инструменты (скриншоты, уведомления) требуют
установки Termux:API companion app.

**Решение**: В setup-скрипте проверить наличие и предложить установку:
```bash
pkg install termux-api
```

## 9. Конфликты PATH

**Проблема**: PATH может быть неправильно настроен, и команды из Debian
могут вызываться из Termux.

**Решение**: Явно указывать полные пути в скриптах:
```bash
$PREFIX/bin/proot-distro login debian -- /usr/bin/python3 script.py
```

## 10. Сетевые ограничения

**Проблема**: Некоторые порты могут быть заблокированы, VPN может мешать.

**Решение**: Использовать нестандартные порты для dev-серверов (8080, 3000, 5000).

## 11. Git credentials

**Проблема**: Хранение credentials в proot может быть ненадёжным.

**Решение**: Использовать SSH-ключи вместо HTTPS:
```bash
ssh-keygen -t ed25519
# Добавить ~/.ssh/id_ed25519.pub на GitHub
git config --global url."git@github.com:".insteadOf "https://github.com/"
```

## 12. Проблемы с Node.js

**Проблема**: Node.js в Termux может конфликтовать с Node.js в Debian.

**Решение**: Использовать Node.js только из одного окружения, или:
```bash
# В Debian — свой Node.js
proot-distro login debian -- node --version
# В Termux — свой Node.js  
node --version
```
