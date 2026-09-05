# JARVIS 🤖

Asistente de IA para Android con interfaz futurista tipo JARVIS.

## Características

- 🎙️ Entrada por voz
- 💬 Chat
- 🔊 Texto a voz
- 🌑 Interfaz oscura futurista
- 📱 Android
- 🔌 Preparado para conectar un backend de IA

## Abrir en Android Studio

1. Descarga/clona este repositorio.
2. Abre la carpeta `JARVIS_GitHub` en Android Studio.
3. Espera a que Gradle sincronice.
4. Ejecuta la aplicación en un móvil/emulador.
5. Para crear la APK: `Build > Generate App Bundle / APK > Generate APK`.

## Seguridad

No guardes claves privadas de IA dentro de la APK. La aplicación debe comunicarse con un backend HTTPS y el backend debe guardar las credenciales como secretos.

## GitHub Actions

El workflow de compilación se encuentra en:

`.github/workflows/build-apk.yml`

Cada push puede generar una APK como artefacto de GitHub Actions.


## Gradle Wrapper

El proyecto incluye `gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.properties`.
El wrapper de JARVIS descarga Gradle 8.11.1 automáticamente la primera vez que se ejecuta.
