# Инструкция по сборке APK

## Требования

- **JDK 17+** (рекомендуется: OpenJDK 17)
- **Android SDK** (API 34, build-tools 34.0.0)
- **Gradle** 8.2+ (или используйте Gradle Wrapper)
- **Android Studio** (опционально, для IDE)

## Шаги сборки

### 1. Подготовка окружения

```bash
# Установите JDK 17 (Ubuntu/Debian)
sudo apt install openjdk-17-jdk

# Или используйте SDKMAN
sdk install java 17.0.9-tem

# Установите Android SDK
# Скачайте Android Studio или command-line tools с:
# https://developer.android.com/studio#command-line-tools-only

# Установите переменные окружения
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### 2. Установите необходимые компоненты SDK

```bash
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "platform-tools"
```

### 3. Скачайте Termux bootstrap

```bash
cd opros
bash scripts/download-bootstrap.sh
```

**ВАЖНО**: Файл `app/src/main/assets/bootstrap-arm64.zip` должен существовать
перед сборкой. Если скрипт не смог его скачать — скачайте вручную:

1. Перейдите на https://github.com/termux/termux-packages/releases
2. Найдите последний релиз с bootstrap
3. Скачайте файл `bootstrap-XXXX.XX.XX-rX-aarch64.zip`
4. Сохраните как `app/src/main/assets/bootstrap-arm64.zip`

### 4. Создайте local.properties

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### 5. Соберите APK

#### Debug APK (для тестирования):
```bash
./gradlew assembleDebug
```
Результат: `app/build/outputs/apk/debug/app-debug.apk`

#### Release APK (подписанный):
```bash
# Создайте keystore (один раз)
keytool -genkey -v -keystore release-key.jks -keyalg RSA \
    -keysize 2048 -validity 10000 -alias opros-key

# Соберите release
./gradlew assembleRelease \
    -Pandroid.injected.signing.store.file=release-key.jks \
    -Pandroid.injected.signing.store.password=YOUR_PASSWORD \
    -Pandroid.injected.signing.key.alias=opros-key \
    -Pandroid.injected.signing.key.password=YOUR_PASSWORD
```
Результат: `app/build/outputs/apk/release/app-release.apk`

### 6. Установите на устройство

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Сборка через Android Studio

1. Откройте Android Studio
2. File → Open → выберите директорию opros
3. Дождитесь синхронизации Gradle
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK будет в `app/build/outputs/apk/debug/`

## Решение проблем

### "Bootstrap not found"
Убедитесь что файл `app/src/main/assets/bootstrap-arm64.zip` существует
и является валидным ZIP-архивом.

### "SDK location not found"
Создайте файл `local.properties` с путём к SDK:
```
sdk.dir=/path/to/android/sdk
```

### "Could not determine the dependencies of task"
Обновите Gradle wrapper:
```bash
./gradlew wrapper --gradle-version 8.4
```

### Ошибка подписи Release APK
Создайте keystore или используйте Debug-сборку для тестирования.

### "NDK not configured"
NDK не требуется для этого проекта — пропустите или установите:
```bash
sdkmanager "ndk;25.2.9519653"
```

## Размер APK

Ожидаемый размер:
- Без bootstrap: ~15-20 MB
- С bootstrap: ~150-250 MB (bootstrap ~130 MB)

Для уменьшения размера можно:
- Включить R8/ProGuard минификацию
- Удалить неиспользуемые библиотеки из bootstrap
- Загружать bootstrap при первом запуске из сети (вместо assets)
