# Как собрать APK Opros

## Почему APK не собран в репозитории?

В песочнице Arena нет доступа к:
- JDK 17 (необходим для Android-сборки)
- Android SDK (необходим для компиляции)
- Внешним ресурсам для их установки

**APK нужно собрать локально** на вашей машине. Это займёт 5-10 минут.

## Требования

- **Linux/macOS/Windows** с установленным Git
- **JDK 17** (OpenJDK или Oracle JDK)
- **Android SDK** (API level 34)
- **~500 MB** свободного места

## Пошаговая инструкция

### Шаг 1: Установить JDK 17

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version  # Должно показать: openjdk version "17.x.x"
```

**macOS (Homebrew):**
```bash
brew install openjdk@17
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
java -version
```

**Windows:**
Скачайте и установите JDK 17 с https://adoptium.net/

### Шаг 2: Установить Android SDK

**Вариант A: Через Android Studio (рекомендуется)**

1. Скачайте Android Studio: https://developer.android.com/studio
2. Установите и запустите
3. Tools → SDK Manager → SDK Platforms → выберите "Android 14 (API 34)"
4. Tools → SDK Manager → SDK Tools → выберите "Android SDK Build-Tools 34"
5. Запомните путь к SDK (обычно `~/Android/Sdk` или `C:\Users\YourName\AppData\Local\Android\Sdk`)

**Вариант B: Command-line tools (без Android Studio)**

```bash
# Скачать command-line tools
cd ~/android-sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
mkdir -p cmdline-tools
mv latest cmdline-tools/

# Установить SDK
export ANDROID_SDK_ROOT=~/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin
sdkmanager --licenses  # Принять все лицензии (нажать 'y' несколько раз)
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

### Шаг 3: Клонировать репозиторий

```bash
git clone https://github.com/ujujkm337/opros.git
cd opros
```

### Шаг 4: Скачать Termux bootstrap

```bash
bash scripts/download-bootstrap.sh
```

Это скачает bootstrap (~130 MB) в `app/src/main/assets/bootstrap-arm64.zip`.

**Если скрипт не работает**, скачайте вручную:
1. Перейдите на https://github.com/termux/termux-packages/releases
2. Найдите последний релиз с `bootstrap-XXXX.XX.XX-rX-aarch64.zip`
3. Скачайте и сохраните как `app/src/main/assets/bootstrap-arm64.zip`

### Шаг 5: Создать local.properties

```bash
# Linux/macOS
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# Windows (в PowerShell)
echo "sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk" > local.properties

# Или укажите ваш путь к SDK
```

### Шаг 6: Собрать APK

```bash
# Сделать gradlew исполняемым (Linux/macOS)
chmod +x gradlew

# Собрать debug APK
./gradlew assembleDebug
```

**Первая сборка займёт 3-5 минут** (Gradle скачает зависимости).

### Шаг 7: Найти APK

```bash
ls -lh app/build/outputs/apk/debug/
# app-debug.apk (~150-250 MB)
```

### Шаг 8: Установить на телефон

**Вариант A: Через ADB (USB-кабель)**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Вариант B: Скопировать на телефон**
```bash
# Скопируйте APK на телефон любым способом:
# - USB-кабель
# - Облако (Google Drive, Dropbox)
# - Telegram/WhatsApp (отправьте себе)
# Затем откройте APK на телефоне и установите
```

## Решение проблем

### Ошибка: "SDK location not found"
Создайте файл `local.properties` с путём к SDK:
```
sdk.dir=/path/to/your/android/sdk
```

### Ошибка: "Could not find or load main class org.gradle.wrapper.GradleWrapperMain"
Gradle wrapper повреждён. Перегенерируйте:
```bash
gradle wrapper --gradle-version 8.2
```

### Ошибка: "Bootstrap not found"
Убедитесь, что файл существует:
```bash
ls -lh app/src/main/assets/bootstrap-arm64.zip
```
Если нет — запустите `bash scripts/download-bootstrap.sh`

### Ошибка: "JAVA_HOME is not set"
Установите переменную:
```bash
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
```

### Сборка зависает на "Downloading..."
Первая сборка скачивает ~200 MB зависимостей. Подождите 5-10 минут.

### APK слишком большой (~250 MB)
Это нормально. Bootstrap Termux весит ~130 MB, OpenCode ~66 MB.

## Альтернатива: Сборка в Android Studio

1. Откройте Android Studio
2. File → Open → выберите папку `opros`
3. Дождитесь синхронизации Gradle (3-5 минут)
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK будет в `app/build/outputs/apk/debug/app-debug.apk`

## Готово!

После установки APK на телефон:
1. Откройте Opros
2. Нажмите "▶ Начать установку"
3. Подождите 2-5 минут (установка Termux + OpenCode + DeepSeek)
4. Нажмите "🚀 Запустить"
5. Нажмите "🤖 OpenCode" — AI-агент готов!
