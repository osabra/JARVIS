from pathlib import Path

p = Path("app/src/main/java/com/jarvis/ai/MainActivity.kt")
if not p.exists():
    raise SystemExit(f"MainActivity not found: {p}")

s = p.read_text(encoding="utf-8")

# The futuristic UI may already be present. In that case this step must be a
# safe no-op instead of trying to find an obsolete marker and failing.
if "NEURAL FEED" in s and "HudMetric(" in s and "VOICE LINK" in s:
    print("Futuristic UI already present; nothing to patch")
    raise SystemExit(0)

# For older versions, locate the composable UI by stable structural markers.
start_marker = "    MaterialTheme(colorScheme = darkColorScheme"
end_marker = "\n        if (showSettings) {"

start = s.find(start_marker)
end = s.find(end_marker, start if start >= 0 else 0)

if start < 0 or end < 0:
    raise SystemExit("Could not locate JarvisApp UI markers; refusing to modify the file")

# Keep the source intact for versions that predate the current UI. The current
# repository already contains the futuristic implementation, so reaching this
# branch indicates an unexpected source version and should fail clearly.
raise SystemExit("Unsupported MainActivity UI version: futuristic UI markers are missing")
