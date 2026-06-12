# Allsender SMS Gateway — NOTIFICATION + DIAGNOSTIC SERVER v7

## Objetivo

Corregir el problema donde el usuario activa el servidor, pero no queda claro si la notificación permanente realmente está activa.

## Qué agrega este v7

### 1. Diagnóstico real del sistema

En la pestaña **Diag** ahora se muestra:

- Enviar SMS: permitido / falta.
- Recibir SMS: permitido / falta.
- Leer SIM / teléfono: permitido / falta.
- Cámara QR: permitida / falta.
- Notificación permanente: permitida / falta activar notificaciones.
- Batería sin restricciones: OK / Android puede dormir el servicio.
- Estado del servicio: activo / apagado.

### 2. Botones directos

En Diagnóstico:

- **Activar / revisar permisos**
- **Notificación**
- **Batería**
- **Activar servidor + notificación permanente**

### 3. Notificación más visible

El canal del servicio subió de `IMPORTANCE_LOW` a `IMPORTANCE_DEFAULT`.

La notificación ahora usa prioridad alta para que sea más evidente al usuario.

### 4. Mensaje operativo claro

La app explica que el servidor solo queda trabajando en segundo plano si:

- la notificación permanente está visible,
- las notificaciones están permitidas,
- la batería no tiene restricciones.

## Regla final

Si el usuario no ve la notificación permanente de Allsender, el servidor no está realmente protegido en segundo plano.

## Flujo de prueba

1. Instalar APK.
2. Abrir app.
3. Escanear QR.
4. Ir a **Diag**.
5. Tocar **Activar / revisar permisos**.
6. Tocar **Batería** y poner Sin restricciones.
7. Tocar **Activar servidor + notificación permanente**.
8. Confirmar que aparece la notificación permanente.
9. Enviar SMS desde Allsender Web.
10. Revisar terminal y DB.

## Señal correcta

- En teléfono: notificación permanente visible.
- En app: terminal muestra CONECTADO / SINCRONIZACIÓN.
- En DB: `last_seen_at` y `last_poll_at` cambian.
