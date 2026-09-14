# JARVIS 🤖

Asistente de IA para Android con interfaz futurista tipo JARVIS y módulo **Video AI (imagen → vídeo)**.

## Características

- 🎙️ Entrada por voz
- 💬 Chat
- 🔊 Texto a voz
- 🌑 Interfaz oscura futurista
- 📱 Android
- 🖼️ Selección de imagen para Image-to-Video
- ✍️ Prompt de movimiento
- ⏱️ Duraciones 5 / 10 / 15 s
- 🔌 Configuración de endpoint y API key para proveedor de vídeo

## Generación real

La generación de vídeo depende de un proveedor de IA y su API. La app no simula una generación. El adaptador concreto debe implementar subida de imagen, creación del trabajo, consulta de estado y descarga del MP4.

## Abrir en Android Studio

1. Clona este repositorio.
2. Abre la carpeta del proyecto en Android Studio.
3. Espera a que Gradle sincronice.
4. Ejecuta en un móvil/emulador.
5. Para crear la APK: `Build > Generate App Bundle / APK > Generate APK`.

## Seguridad

No guardes claves privadas de IA dentro de la APK ni las subas a GitHub. En producción se recomienda un backend HTTPS que gestione las credenciales como secretos.

## GitHub Actions

El workflow de compilación se encuentra en `.github/workflows/build-apk.yml` y puede generar una APK como artefacto tras un push.

## Gradle Wrapper

El proyecto incluye `gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.properties`.
