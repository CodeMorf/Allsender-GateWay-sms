# Allsender SMS Gateway — BACKGROUND SERVER v5

## Objetivo

Dejar la app lista para trabajar como **servidor SMS en segundo plano**.

## Cómo debe funcionar

1. El cliente escanea el QR.
2. Toca **Activar servidor en segundo plano**.
3. La app inicia un Foreground Service de tipo `remoteMessaging`.
4. Aparece una notificación permanente.
5. El usuario puede salir de la pantalla o bloquear el teléfono.
6. Mientras la notificación siga visible, la app sigue:
   - enviando heartbeat,
   - consultando cola de SMS,
   - enviando SMS pendientes,
   - reportando sent/failed,
   - mostrando logs en la terminal al volver a abrir.

## Cambio Android importante

Se cambió el servicio a:

```xml
android:foregroundServiceType="remoteMessaging"
```

Y se agregó:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_REMOTE_MESSAGING" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

`remoteMessaging` es el tipo correcto para un gateway que transfiere mensajes entre Allsender Web y el teléfono.

## Limitación real de Android

Esto sí puede trabajar en segundo plano, pero no debe ser invisible.

Android exige:

- notificación permanente,
- servicio visible,
- permiso de notificaciones,
- batería sin restricciones para máxima estabilidad.

Si el usuario fuerza detención, cierra desde ajustes, quita permisos o el fabricante mata la app por batería, el servicio puede detenerse.

## Operación recomendada para el cliente

- Dejar el teléfono encendido.
- Dejar internet activo.
- Dejar SIM con saldo/plan SMS.
- Permitir SMS, teléfono, cámara y notificaciones.
- Configurar batería: **Sin restricciones**.
- No cerrar la notificación permanente.
- Si reinicia el teléfono, abrir la app y tocar activar otra vez.

## Señal de que funciona

En Allsender DB:

- `last_seen_at` cambia.
- `last_poll_at` cambia.
- SMS pasan de `queued` a `sent` o `failed`.

En la app:

- la terminal muestra CONECTADO, SINCRONIZACIÓN, ENVIANDO, ENVIADO/FALLÓ.
