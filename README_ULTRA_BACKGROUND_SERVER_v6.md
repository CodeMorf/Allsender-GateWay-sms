# Allsender SMS Gateway — ULTRA BACKGROUND SERVER v6

## Objetivo

Hacer el modo servidor SMS más fuerte para trabajar en segundo plano.

## Qué agrega este v6

- `ForegroundService` tipo `remoteMessaging`.
- `android:stopWithTask="false"`.
- `WAKE_LOCK` con `PARTIAL_WAKE_LOCK` mientras el servidor está activo.
- `RECEIVE_BOOT_COMPLETED`.
- `BootReceiver` para reactivar el gateway después de reiniciar el teléfono o actualizar la app.
- Auto-recuperación si Android destruye el servicio y el modo servidor sigue marcado como activo.
- Coroutines en `Dispatchers.IO` con `SupervisorJob`, no atadas a la pantalla.
- La terminal registra:
  - energía / wakelock,
  - segundo plano,
  - auto-recuperación,
  - arranque.

## Respuesta a la pregunta técnica

Sí, se usan corrutinas, pero **una corrutina sola no mantiene vivo el proceso en segundo plano**.

Para segundo plano real se necesita:

```kotlin
ForegroundService + Notification permanente + CoroutineScope(SupervisorJob() + Dispatchers.IO)
```

Y para mayor estabilidad:

```kotlin
Partial WakeLock + batería sin restricciones + BootReceiver
```

## Limitación honesta

Android y fabricantes como Xiaomi, Samsung, Oppo, Vivo, Tecno/Infinix pueden matar servicios por batería.

Para máxima estabilidad el cliente debe:

1. Permitir notificaciones.
2. Quitar ahorro de batería.
3. Permitir inicio automático si el teléfono tiene esa opción.
4. No cerrar la notificación permanente.
5. Dejar internet activo.
6. Dejar SIM con saldo/plan SMS.

## Flujo recomendado

1. Escanear QR.
2. Activar servidor en segundo plano.
3. Ver notificación permanente.
4. Salir de la app o bloquear teléfono.
5. Allsender sigue con heartbeat + polling + envío SMS.
6. Si reinicia el teléfono, el servicio intenta volver a arrancar automáticamente.

## Qué no se tocó

- Backend.
- QR.
- Token.
- Endpoints.
- Diseño base.
