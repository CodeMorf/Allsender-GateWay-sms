# Allsender SMS Gateway — HEARTBEAT POLLING FORCE v3

## Qué corrige

Esta versión agrega un failsafe crítico:

- Si la app logra enviar heartbeat al backend, también ejecuta polling inmediatamente desde `sendHeartbeat()`.
- Esto evita que los SMS web queden en `queued` cuando Android no arranca bien el loop separado de polling.
- Mantiene el polling loop normal.
- Mantiene el polling inmediato al activar gateway.
- No toca backend, QR, token ni diseño.

## Archivo modificado

`app/src/main/java/com/example/data/GatewayRepository.kt`

## Señal esperada

Después de instalar el APK nuevo:

- `last_seen_at` debe actualizarse.
- `last_poll_at` también debe actualizarse.
- Los SMS deben cambiar de `queued` a `sent` o `failed`.

## Importante

Desinstalar la app vieja antes de instalar este APK.
