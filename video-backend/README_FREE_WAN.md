# JARVIS — Image to Video gratis con Wan 2.2

Backend local para generar vídeo sin Runway ni API de pago.

## Requisitos
- Python 3.10+
- NVIDIA GPU recomendada
- Wan2.2 TI2V-5B instalado en `WAN_ROOT`
- GPU con suficiente VRAM para el modelo; el proyecto oficial de Wan documenta los requisitos de memoria de sus configuraciones.

## Configuración
```bash
pip install -r requirements.txt
export WAN_ROOT=/ruta/Wan2.2
export WAN_CKPT=/ruta/Wan2.2/Wan2.2-TI2V-5B
python wan_server.py
```
En Windows usa `set` en lugar de `export`.

## API
POST `/v1/video` multipart: `image`, `prompt`, `duration` (5/10/15).
Devuelve `jobId`. Consulta `GET /v1/jobs/{jobId}` y, al terminar, reproduce `GET /v1/jobs/{jobId}/video`.

El modelo es local y gratuito; el coste de la generación depende del hardware donde se ejecute.
