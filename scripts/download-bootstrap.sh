#!/bin/bash
# Скачать Termux bootstrap для Opros

set -e

BOOTSTRAP_DIR="app/src/main/assets"
BOOTSTRAP_FILE="$BOOTSTRAP_DIR/bootstrap-arm64.zip"

echo "=== Opros: Загрузка Termux bootstrap ==="
echo ""

mkdir -p "$BOOTSTRAP_DIR"

if [ -f "$BOOTSTRAP_FILE" ]; then
    SIZE=$(du -h "$BOOTSTRAP_FILE" | cut -f1)
    echo "Bootstrap уже существует ($SIZE)"
    echo "Удалите его для повторной загрузки: rm $BOOTSTRAP_FILE"
    exit 0
fi

# URL последнего bootstrap от Termux
BOOTSTRAP_URL="https://github.com/termux/termux-packages/releases/download/bootstrap-2024.01.01-r1/bootstrap-2024.01.01-r1-aarch64.zip"

echo "Загрузка: $BOOTSTRAP_URL"
echo "Это займёт 1-3 минуты (~130 MB)..."
echo ""

if command -v curl &> /dev/null; then
    curl -L -o "$BOOTSTRAP_FILE" "$BOOTSTRAP_URL"
elif command -v wget &> /dev/null; then
    wget -O "$BOOTSTRAP_FILE" "$BOOTSTRAP_URL"
else
    echo "Ошибка: нужен curl или wget"
    exit 1
fi

if [ -f "$BOOTSTRAP_FILE" ]; then
    SIZE=$(du -h "$BOOTSTRAP_FILE" | cut -f1)
    echo ""
    echo "✓ Bootstrap загружен: $BOOTSTRAP_FILE ($SIZE)"
    echo ""
    echo "Теперь можно собрать APK:"
    echo "  ./gradlew assembleDebug"
else
    echo "✗ Ошибка загрузки"
    echo ""
    echo "Попробуйте скачать вручную:"
    echo "1. https://github.com/termux/termux-packages/releases"
    echo "2. Найдите bootstrap для aarch64"
    echo "3. Сохраните как: $BOOTSTRAP_FILE"
    exit 1
fi
