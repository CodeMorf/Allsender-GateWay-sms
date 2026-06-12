# Allsender SMS Gateway - Patch Polling Final

Correcciones aplicadas:

- `GatewayRepository.isPollingEnabled()` ahora inicia en `true`.
- `GatewayService.startGatewayService()` ya no continúa si `startForeground()` falla.
- El servicio fuerza `repository.setPollingEnabled(true)` al activarse.
- El servicio ejecuta `pollPendingSMS()` inmediatamente al activar gateway.
- `startPollingLoop()` ya no retorna silenciosamente cuando una preferencia vieja tenía polling apagado.
- Si el polling falla, guarda error en diagnóstico.

Este patch corrige el caso donde el teléfono manda heartbeat pero `last_poll_at` queda null y los mensajes creados desde la web se quedan en queued/processing.
