termux-mcp
==========

Model Context Protocol (MCP) bridge for Termux/Android.
Provides AI assistants (like OpenCode) with tools to interact with the Android system.

Installation
------------

    pip install .
    # or
    pip install termux-mcp

Usage
-----

The MCP server runs via stdio and is typically configured in OpenCode's config:

    {
        "mcp": {
            "termux": {
                "command": "python3",
                "args": ["-m", "termux_mcp"]
            }
        }
    }

Available Tools
---------------

- execute_command: Run shell commands in Termux or Debian
- read_file / write_file: File operations
- list_directory: List directory contents
- delete_file / move_file: File management
- get_device_info: Android device information
- get_battery_status: Battery level and status
- send_notification: Send Android notifications
- get_clipboard / set_clipboard: Clipboard operations
- vibrate: Device vibration
- open_url: Open URLs in browser
- share_text: Share via Android share sheet
- take_screenshot: Capture screen (requires Termux:API)
