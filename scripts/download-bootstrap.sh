#!/bin/bash
# =============================================================================
# Termux Bootstrap Installer for Opros Dev Environment
# 
# This script downloads the Termux bootstrap for arm64 and places it
# in the correct location for the Android app to use.
#
# Usage: Run this from the project root before building the APK.
# =============================================================================

set -e

BOOTSTRAP_DIR="app/src/main/assets"
BOOTSTRAP_FILE="$BOOTSTRAP_DIR/bootstrap-arm64.zip"
BOOTSTRAP_URL="https://github.com/termux/termux-packages/releases/download/bootstrap-2024.01.01-r1/bootstrap-2024.01.01-r1-aarch64.zip"

echo "=== Opros Dev Environment - Bootstrap Setup ==="
echo ""

# Create assets directory
mkdir -p "$BOOTSTRAP_DIR"

if [ -f "$BOOTSTRAP_FILE" ]; then
    echo "Bootstrap already exists at $BOOTSTRAP_FILE"
    echo "Delete it first if you want to re-download."
    exit 0
fi

echo "Downloading Termux bootstrap for arm64..."
echo "URL: $BOOTSTRAP_URL"
echo ""

# Try to download the bootstrap
if command -v curl &> /dev/null; then
    curl -L -o "$BOOTSTRAP_FILE" "$BOOTSTRAP_URL" || {
        echo ""
        echo "WARNING: Download failed. This is expected if the URL has changed."
        echo ""
        echo "Manual bootstrap setup:"
        echo "1. Go to: https://github.com/termux/termux-packages/releases"
        echo "2. Download the latest bootstrap for aarch64"
        echo "3. Place it at: $BOOTSTRAP_FILE"
        echo ""
        echo "Alternative: Use the termux-app repository to build your own bootstrap:"
        echo "  git clone https://github.com/termux/termux-app"
        echo "  cd termux-app && ./scripts/build-bootstrap.sh"
        echo ""
        
        # Create a placeholder
        echo "Creating placeholder bootstrap..."
        cat > "$BOOTSTRAP_DIR/bootstrap-README.txt" << 'EOF'
BOOTSTRAP PLACEHOLDER
====================

This file is a placeholder. Before building the APK, you need to:

1. Download the Termux bootstrap for aarch64 from:
   https://github.com/termux/termux-packages/releases

2. Place the zip file here as: bootstrap-arm64.zip

The bootstrap contains the complete Termux rootfs including:
- BusyBox
- apt/pkg package manager
- bash shell
- Basic Linux utilities
- Library files for aarch64

Without the bootstrap, the app will not be able to set up the
Termux environment on first launch.
EOF
        rm -f "$BOOTSTRAP_FILE"
    }
elif command -v wget &> /dev/null; then
    wget -O "$BOOTSTRAP_FILE" "$BOOTSTRAP_URL" || {
        echo "Download failed. See instructions above."
        rm -f "$BOOTSTRAP_FILE"
    }
else
    echo "Neither curl nor wget found. Please download the bootstrap manually."
    echo "URL: $BOOTSTRAP_URL"
    echo "Save to: $BOOTSTRAP_FILE"
fi

echo ""
echo "Done!"
