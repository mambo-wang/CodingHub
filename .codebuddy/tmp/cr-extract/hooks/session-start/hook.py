#!/usr/bin/env python3
"""Example SessionStart hook shipped by code-reviewer plugin."""
import json
import sys


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    out = {
        "continue": True,
        "hookSpecificOutput": {
            "hookEventName": "SessionStart",
            "additionalContext": "[code-reviewer] Session started. Use /review-code to kick off a review.",
        },
    }
    print(json.dumps(out, ensure_ascii=False))


if __name__ == "__main__":
    main()