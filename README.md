# JARVIS 🤖

Asistente de IA para Android con módulo **Video AI (imagen → vídeo)**.

## Video AI gratuito

JARVIS usa un backend público basado en **Wan 2.2** para animar una imagen con un prompt. La arquitectura de NiftyVid usa un Worker como proxy hacia un Hugging Face Space con Wan 2.2, evitando que la app tenga que incluir una API de pago. El servicio público puede tener colas, límites o cambiar de disponibilidad. citeturn1search2

La versión gratuita está pensada para clips cortos. Un Space público de Wan 2.2 documenta Image-to-Video y límites de duración para mantenerse dentro del tiempo de GPU disponible. citeturn1search0turn1search10

## Características

- 🎙️ Entrada por voz
- 💬 Chat
- 🔊 Texto a voz
- 🌑 Interfaz oscura futurista
- 📱 Android
- 🖼️ Selección de imagen
- ✍️ Prompt de movimiento
- 🎬 Image-to-Video con Wan 2.2
- 💰 Sin Runway y sin API de pago en el modo gratuito

## Desarrollo

1. Clona este repositorio.
2. Ábrelo en Android Studio.
3. Sincroniza Gradle.
4. Ejecuta la app.
5. Genera una APK desde **Build > Generate App Bundle / APK > Generate APK**.

La pantalla de vídeo tiene un backend configurable y, por defecto, apunta al proxy público de NiftyVid. Si ese servicio deja de estar disponible, puedes cambiar el endpoint por otro backend compatible.

## Seguridad

No guardes claves privadas de IA dentro de la APK ni las subas a GitHub.

## Nota sobre el modelo

Wan 2.2 es un modelo abierto de generación de vídeo y soporta Image-to-Video; el modelo oficial documenta generación a 24 FPS en sus configuraciones compatibles. citeturn0search3
