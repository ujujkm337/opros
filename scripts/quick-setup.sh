#!/bin/bash
# =============================================================================
# Opros Quick Setup — для установки OpenCode + DeepSeek прямо в Termux
# 
# Запустите ЭТОТ СКРИПТ в Termux для ручной установки без APK:
#   curl -fsSL https://raw.githubusercontent.com/ujujkm337/opros/main/scripts/quick-setup.sh | bash
# =============================================================================

set -e

echo "╔═══════════════════════════════════════════╗"
echo "║  Opros Quick Setup                        ║"
echo "║  OpenCode + DeepSeek V4.1 Flash Free      ║"
echo "╚═══════════════════════════════════════════╝"
echo ""

# Check architecture
ARCH=$(uname -m)
if [ "$ARCH" != "aarch64" ]; then
    echo "✗ Требуется архитектура aarch64 (arm64)"
    echo "  Ваша архитектура: $ARCH"
    exit 1
fi
echo "✓ Архитектура: $ARCH"

# Update packages
echo ""
echo "[1/4] Обновление пакетов..."
pkg update -y

# Install dependencies
echo ""
echo "[2/4] Установка зависимостей..."
pkg install -y curl ripgrep

# Download and install opencode-termux
echo ""
echo "[3/4] Установка OpenCode (native aarch64, guysoft build)..."
cd /tmp
curl -LO https://github.com/guysoft/opencode-termux/releases/latest/download/opencode-aarch64.deb
dpkg -i opencode-aarch64.deb 2>/dev/null || pkg install -f -y
rm -f opencode-aarch64.deb

# Verify
if command -v opencode &> /dev/null; then
    VERSION=$(opencode --version 2>/dev/null || echo "unknown")
    echo "✓ OpenCode установлен: $VERSION"
else
    echo "✗ Ошибка установки OpenCode"
    exit 1
fi

# Configure DeepSeek
echo ""
echo "[4/4] Настройка DeepSeek V4.1 Flash Free..."
mkdir -p ~/.config/opencode

cat > ~/.config/opencode/opencode.json << 'EOF'
{
  "$schema": "https://opencode.ai/config.json",
  "model": "opencode/deepseek-v4.1-flash-free"
}
EOF

echo "✓ Конфигурация записана"

# Done!
echo ""
echo "╔═══════════════════════════════════════════╗"
echo "║  ✓ Установка завершена!                   ║"
echo "╚═══════════════════════════════════════════╝"
echo ""
echo "DeepSeek V4.1 Flash Free:"
echo "  Модель: opencode/deepseek-v4.1-flash-free"
echo "  Цена:   \$0 (бесплатно)"
echo "  Контекст: 200K токенов"
echo ""
echo "Для запуска выполните:"
echo "  opencode"
echo ""
echo "При первом запуске:"
echo "  /connect  → выбрать OpenCode (Zen)"
echo "  /models   → проверить DeepSeek V4.1 Flash Free"
echo ""
