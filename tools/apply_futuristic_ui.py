from pathlib import Path

p = Path("app/src/main/java/com/jarvis/ai/MainActivity.kt")
if not p.exists():
    raise SystemExit(f"MainActivity not found: {p}")

s = p.read_text(encoding="utf-8")

# Current MainActivity already contains the futuristic holographic core UI.
# Make this CI step idempotent: never try to inject a second UI or depend on
# obsolete source markers.
if "NEURAL FEED" in s and "HudMetric(" in s and "VOICE LINK" in s:
    print("Futuristic UI already present; nothing to patch")
    raise SystemExit(0)

raise SystemExit("Futuristic UI is not present in MainActivity.kt; refusing unsafe patch")
