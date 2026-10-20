#!/bin/bash
# =============================================================================
# Post-install setup script for Opros Dev Environment
# This runs inside Termux after bootstrap is installed
# =============================================================================

set -e

echo "=== Opros Dev Environment - Post-Install Setup ==="
echo ""

# 1. Update packages
echo "[1/6] Updating package repositories..."
pkg update -y

# 2. Install essential packages
echo "[2/6] Installing essential packages..."
pkg install -y proot-distro nodejs git curl python openssh

# 3. Install Debian
echo "[3/6] Installing Debian via proot-distro..."
proot-distro install debian

# 4. Setup Debian environment
echo "[4/6] Configuring Debian environment..."
proot-distro login debian -- bash -c '
    apt update
    apt install -y python3 python3-pip python3-venv nodejs npm git curl wget
    
    # Create project directory
    mkdir -p /root/Projects
    mkdir -p /root/.config/opencode
'

# 5. Install OpenCode
echo "[5/6] Installing OpenCode..."
proot-distro login debian -- bash -c '
    # Try npm installation
    npm install -g opencode 2>/dev/null || {
        echo "npm install failed, trying curl method..."
        curl -fsSL https://opencode.ai/install | bash 2>/dev/null || {
            echo "WARNING: OpenCode installation failed."
            echo "You can install it manually later."
        }
    }
'

# Also install in Termux for direct access
npm install -g opencode-termux 2>/dev/null || echo "OpenCode Termux install skipped"

# 6. Install MCP bridge
echo "[6/6] Installing termux-mcp bridge..."
pip install termux-mcp 2>/dev/null || {
    npm install -g termux-mcp 2>/dev/null || {
        echo "WARNING: MCP bridge installation failed."
        echo "You can install it manually later."
    }
}

# Write OpenCode configuration
echo ""
echo "Writing OpenCode configuration..."
CONFIG_DIR="$HOME/.config/opencode"
mkdir -p "$CONFIG_DIR"

cat > "$CONFIG_DIR/config.json" << 'EOFCONFIG'
{
    "mcp": {
        "termux": {
            "command": "python3",
            "args": ["-m", "termux_mcp"],
            "env": {}
        }
    }
}
EOFCONFIG

echo ""
echo "=== Setup Complete! ==="
echo ""
echo "Available commands:"
echo "  proot-distro login debian    - Enter Debian shell"
echo "  opencode                     - Launch OpenCode AI"
echo "  termux-mcp                   - Start MCP bridge"
echo ""
