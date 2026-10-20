#!/usr/bin/env python3
"""Setup file for termux-mcp package."""

from setuptools import setup

setup(
    name="termux-mcp",
    version="1.0.0",
    description="Model Context Protocol bridge for Termux/Android",
    author="Opros Dev",
    py_modules=["termux_mcp"],
    python_requires=">=3.8",
    entry_points={
        "console_scripts": [
            "termux-mcp=termux_mcp:main",
        ]
    }
)
