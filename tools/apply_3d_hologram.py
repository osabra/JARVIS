from pathlib import Path

p = Path('app/src/main/java/com/jarvis/ai/MainActivity.kt')
s = p.read_text(encoding='utf-8')
start = '            Box(Modifier.size(272.dp).scale(pulse), contentAlignment = Alignment.Center) {'
end = '            Text(if (wakeMode) "WAKE WORD  /  «JARVIS»" else "VOICE SYSTEM READY", color = CYAN2, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)'

replacement = '''            HolographicCore3D(
                status = status,
                pulse = pulse,
                angle = angle,
                reverseAngle = reverseAngle,
                wave = wave
            )

'''

a = s.find(start)
b = s.find(end, a)
if a < 0 or b < 0:
    raise SystemExit('3D patch anchors not found')

s = s[:a] + replacement + s[b:]
p.write_text(s, encoding='utf-8')
print('3D hologram forced into main screen')
