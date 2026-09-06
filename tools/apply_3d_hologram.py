from pathlib import Path

p = Path('app/src/main/java/com/jarvis/ai/MainActivity.kt')
if not p.exists():
    raise SystemExit(f'MainActivity not found: {p}')

s = p.read_text(encoding='utf-8')

# The holographic core is maintained as a dedicated composable and is already
# wired into MainActivity. This CI step must be idempotent and must not search
# for obsolete pre-redesign Canvas anchors.
if 'HolographicCore3D(' in s:
    print('3D holographic core already wired into MainActivity; nothing to patch')
    raise SystemExit(0)

raise SystemExit('HolographicCore3D is not wired into MainActivity; refusing unsafe patch')
