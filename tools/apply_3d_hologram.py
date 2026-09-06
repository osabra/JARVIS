from pathlib import Path

p = Path('app/src/main/java/com/jarvis/ai/MainActivity.kt')
s = p.read_text(encoding='utf-8')
start = '            Box(Modifier.size(272.dp).scale(pulse), contentAlignment = Alignment.Center) {'
end = '            Text(if (wakeMode) "WAKE WORD  /  «JARVIS»" else "VOICE SYSTEM READY", color = CYAN2, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)'
if 'HolographicCore3D(' in s:
    print('3D hologram already applied')
else:
    a = s.find(start)
    b = s.find(end, a)
    if a < 0 or b < 0:
        raise SystemExit('3D patch anchors not found')
    replacement = '''            HolographicCore3D(\n                status = status,\n                pulse = pulse,\n                angle = angle,\n                reverseAngle = reverseAngle,\n                wave = wave\n            )\n\n'''
    s = s[:a] + replacement + s[b:]
    p.write_text(s, encoding='utf-8')
    print('3D hologram applied')
