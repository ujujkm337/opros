#!/usr/bin/env python3
"""
termux-mcp: Model Context Protocol bridge for Termux/Android
Provides AI assistants with tools to interact with the Android system.

This module implements an MCP server that exposes the following tools:
- execute_command: Run shell commands
- read_file/write_file: File operations
- list_directory: Directory listing
- take_screenshot: Capture screen
- send_notification: Android notifications
- clipboard operations
- device info and battery status
"""

import json
import sys
import os
import subprocess
import base64
from typing import Any, Dict, List, Optional

class TermuxMCPServer:
    """MCP Server implementation for Termux/Android integration."""
    
    def __init__(self):
        self.tools = {
            "execute_command": {
                "description": "Execute a shell command in Termux or Debian",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "command": {
                            "type": "string",
                            "description": "The command to execute"
                        },
                        "use_debian": {
                            "type": "boolean",
                            "description": "If true, run inside Debian proot",
                            "default": False
                        },
                        "timeout": {
                            "type": "integer",
                            "description": "Timeout in seconds",
                            "default": 30
                        }
                    },
                    "required": ["command"]
                }
            },
            "read_file": {
                "description": "Read the contents of a file",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Absolute path to the file"
                        }
                    },
                    "required": ["path"]
                }
            },
            "write_file": {
                "description": "Write content to a file",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Absolute path to the file"
                        },
                        "content": {
                            "type": "string",
                            "description": "Content to write"
                        },
                        "append": {
                            "type": "boolean",
                            "description": "Append to file instead of overwriting",
                            "default": False
                        }
                    },
                    "required": ["path", "content"]
                }
            },
            "list_directory": {
                "description": "List files in a directory",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "Directory path"
                        },
                        "show_hidden": {
                            "type": "boolean",
                            "default": False
                        }
                    },
                    "required": ["path"]
                }
            },
            "delete_file": {
                "description": "Delete a file or directory",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "path": {"type": "string"},
                        "recursive": {"type": "boolean", "default": False}
                    },
                    "required": ["path"]
                }
            },
            "move_file": {
                "description": "Move or rename a file",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "source": {"type": "string"},
                        "destination": {"type": "string"}
                    },
                    "required": ["source", "destination"]
                }
            },
            "get_device_info": {
                "description": "Get Android device information",
                "parameters": {"type": "object", "properties": {}}
            },
            "get_battery_status": {
                "description": "Get battery level and charging status",
                "parameters": {"type": "object", "properties": {}}
            },
            "send_notification": {
                "description": "Send an Android notification",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "title": {"type": "string"},
                        "content": {"type": "string"},
                        "priority": {"type": "string", "default": "default"}
                    },
                    "required": ["title", "content"]
                }
            },
            "get_clipboard": {
                "description": "Get clipboard contents",
                "parameters": {"type": "object", "properties": {}}
            },
            "set_clipboard": {
                "description": "Set clipboard contents",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "text": {"type": "string"}
                    },
                    "required": ["text"]
                }
            },
            "vibrate": {
                "description": "Vibrate the device",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "duration_ms": {"type": "integer", "default": 200}
                    }
                }
            },
            "open_url": {
                "description": "Open a URL in the browser",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "url": {"type": "string"}
                    },
                    "required": ["url"]
                }
            },
            "share_text": {
                "description": "Share text via Android share sheet",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "text": {"type": "string"},
                        "title": {"type": "string", "default": "Share"}
                    },
                    "required": ["text"]
                }
            },
            "take_screenshot": {
                "description": "Take a screenshot (requires Termux:API)",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "save_path": {"type": "string", "default": "/sdcard/screenshot.png"}
                    }
                }
            }
        }

    def handle_request(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """Handle an incoming MCP request."""
        method = request.get("method", "")
        request_id = request.get("id")
        
        if method == "initialize":
            return self._handle_initialize(request_id)
        elif method == "tools/list":
            return self._handle_list_tools(request_id)
        elif method == "tools/call":
            return self._handle_call_tool(request_id, request.get("params", {}))
        else:
            return self._error_response(request_id, f"Unknown method: {method}")

    def _handle_initialize(self, request_id: Any) -> Dict[str, Any]:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {"listChanged": False}
                },
                "serverInfo": {
                    "name": "termux-mcp",
                    "version": "1.0.0"
                }
            }
        }

    def _handle_list_tools(self, request_id: Any) -> Dict[str, Any]:
        tool_list = []
        for name, info in self.tools.items():
            tool_list.append({
                "name": name,
                "description": info["description"],
                "inputSchema": info["parameters"]
            })
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {"tools": tool_list}
        }

    def _handle_call_tool(self, request_id: Any, params: Dict) -> Dict[str, Any]:
        tool_name = params.get("name", "")
        arguments = params.get("arguments", {})
        
        handler = getattr(self, f"_tool_{tool_name}", None)
        if handler is None:
            return self._error_response(request_id, f"Unknown tool: {tool_name}")
        
        try:
            result = handler(**arguments)
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [{"type": "text", "text": str(result)}]
                }
            }
        except Exception as e:
            return self._error_response(request_id, f"Tool error: {str(e)}")

    def _error_response(self, request_id: Any, message: str) -> Dict[str, Any]:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {"code": -32603, "message": message}
        }

    # ===== Tool Implementations =====

    def _tool_execute_command(self, command: str, use_debian: bool = False, timeout: int = 30) -> str:
        if use_debian:
            full_cmd = f"proot-distro login debian -- bash -c '{command}'"
        else:
            full_cmd = command
        
        try:
            result = subprocess.run(
                full_cmd,
                shell=True,
                capture_output=True,
                text=True,
                timeout=timeout
            )
            output = ""
            if result.stdout:
                output += result.stdout
            if result.stderr:
                output += f"\n[STDERR]: {result.stderr}"
            output += f"\n[Exit code: {result.returncode}]"
            return output
        except subprocess.TimeoutExpired:
            return f"Command timed out after {timeout}s"
        except Exception as e:
            return f"Error: {str(e)}"

    def _tool_read_file(self, path: str) -> str:
        with open(path, 'r', encoding='utf-8') as f:
            return f.read()

    def _tool_write_file(self, path: str, content: str, append: bool = False) -> str:
        mode = 'a' if append else 'w'
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, mode, encoding='utf-8') as f:
            f.write(content)
        return f"Written {len(content)} bytes to {path}"

    def _tool_list_directory(self, path: str, show_hidden: bool = False) -> str:
        entries = os.listdir(path)
        if not show_hidden:
            entries = [e for e in entries if not e.startswith('.')]
        
        result = []
        for entry in sorted(entries):
            full_path = os.path.join(path, entry)
            if os.path.isdir(full_path):
                result.append(f"📁 {entry}/")
            else:
                size = os.path.getsize(full_path)
                result.append(f"📄 {entry} ({size} bytes)")
        
        return "\n".join(result) if result else "(empty directory)"

    def _tool_delete_file(self, path: str, recursive: bool = False) -> str:
        import shutil
        if os.path.isdir(path):
            if recursive:
                shutil.rmtree(path)
                return f"Deleted directory: {path}"
            else:
                os.rmdir(path)
                return f"Deleted empty directory: {path}"
        else:
            os.remove(path)
            return f"Deleted file: {path}"

    def _tool_move_file(self, source: str, destination: str) -> str:
        import shutil
        shutil.move(source, destination)
        return f"Moved {source} → {destination}"

    def _tool_get_device_info(self) -> str:
        info = {}
        try:
            # Try termux-info
            result = subprocess.run(["termux-info"], capture_output=True, text=True, timeout=5)
            info["termux_info"] = result.stdout.strip()
        except:
            pass
        
        try:
            # Get system info
            with open("/proc/cpuinfo", "r") as f:
                cpuinfo = f.read()
                info["cpu_model"] = [l for l in cpuinfo.split("\n") if "model name" in l or "Hardware" in l]
        except:
            pass
        
        try:
            with open("/proc/meminfo", "r") as f:
                meminfo = f.read().split("\n")[:3]
                info["memory"] = meminfo
        except:
            pass
        
        return json.dumps(info, indent=2)

    def _tool_get_battery_status(self) -> str:
        try:
            result = subprocess.run(
                ["termux-battery-status"],
                capture_output=True, text=True, timeout=5
            )
            return result.stdout
        except:
            return "Battery status unavailable (Termux:API not installed)"

    def _tool_send_notification(self, title: str, content: str, priority: str = "default") -> str:
        try:
            subprocess.run(
                ["termux-notification", "--title", title, "--content", content],
                timeout=5
            )
            return f"Notification sent: {title}"
        except:
            return "Notification failed (Termux:API not installed)"

    def _tool_get_clipboard(self) -> str:
        try:
            result = subprocess.run(
                ["termux-clipboard-get"],
                capture_output=True, text=True, timeout=5
            )
            return result.stdout
        except:
            return "Clipboard unavailable"

    def _tool_set_clipboard(self, text: str) -> str:
        try:
            subprocess.run(
                ["termux-clipboard-set"],
                input=text, text=True, timeout=5
            )
            return "Clipboard updated"
        except:
            return "Clipboard unavailable"

    def _tool_vibrate(self, duration_ms: int = 200) -> str:
        try:
            subprocess.run(
                ["termux-vibrate", "-d", str(duration_ms)],
                timeout=5
            )
            return f"Vibrated for {duration_ms}ms"
        except:
            return "Vibration unavailable"

    def _tool_open_url(self, url: str) -> str:
        try:
            subprocess.run(
                ["termux-open-url", url],
                timeout=5
            )
            return f"Opened: {url}"
        except:
            return "Failed to open URL"

    def _tool_share_text(self, text: str, title: str = "Share") -> str:
        try:
            subprocess.run(
                ["termux-share", "-a", "send", "-t", "text/plain"],
                input=text, text=True, timeout=10
            )
            return "Share sheet opened"
        except:
            return "Share unavailable"

    def _tool_take_screenshot(self, save_path: str = "/sdcard/screenshot.png") -> str:
        try:
            subprocess.run(
                ["termux-screenshot", "-o", save_path],
                timeout=10
            )
            return f"Screenshot saved to {save_path}"
        except:
            return "Screenshot unavailable (Termux:API not installed)"

    def run_stdio(self):
        """Run the MCP server using stdio transport."""
        print("Termux MCP Server v1.0.0 starting...", file=sys.stderr)
        
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            
            try:
                request = json.loads(line)
                response = self.handle_request(request)
                print(json.dumps(response), flush=True)
            except json.JSONDecodeError:
                error = {
                    "jsonrpc": "2.0",
                    "id": None,
                    "error": {"code": -32700, "message": "Parse error"}
                }
                print(json.dumps(error), flush=True)


def main():
    server = TermuxMCPServer()
    server.run_stdio()


if __name__ == "__main__":
    main()
