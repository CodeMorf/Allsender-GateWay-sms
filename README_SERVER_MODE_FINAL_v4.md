# Allsender SMS Gateway — SERVER MODE FINAL v4

## Cambio principal

Se agregó el concepto **Modo Servidor SMS**.

La lógica queda así:

1. El cliente escanea el QR de Allsender.
2. Toca **Activar modo servidor SMS**.
3. La app inicia el ForegroundService.
4. La app fuerza `heartbeat`.
5. Cada ciclo de heartbeat también fuerza polling de la cola.
6. Si hay SMS pendientes en Allsender Web, el teléfono los toma y los envía.
7. La pantalla muestra una consola tipo terminal con eventos:
   - CONECTADO
   - SINCRONIZACIÓN
   - ENVIANDO
   - ENVIADO
   - FALLÓ

## Regla operativa para el cliente

Este teléfono funciona como un servidor SMS local.

Debe quedarse:

- Encendido.
- Con internet.
- Con SIM activa.
- Con permisos SMS concedidos.
- Con notificaciones permitidas.
- Sin restricción de batería.
- Con la app abierta o el servicio activo.

## Frecuencia

El botón **Activar modo servidor SMS** configura el ciclo en 10 segundos.

El servicio usa el intervalo configurado en Ajustes, con mínimo 5 segundos y máximo 60 segundos.

## Archivos modificados

- `GatewayRepository.kt`
- `GatewayService.kt`
- `GatewayViewModel.kt`
- `GatewayDashboard.kt`

## Qué NO se tocó

- Backend Allsender.
- Endpoints.
- QR.
- Token.
- Diseño principal.
- Codemagic.

## FAQ para cliente

### ¿Tengo que dejar el teléfono encendido?

Sí. Este teléfono es el servidor SMS. Si se apaga, Allsender no podrá enviar SMS.

### ¿Puedo cerrar la app?

Puede quedar en segundo plano si el servicio sigue activo y la notificación está visible. Para máxima estabilidad, dejar la app abierta.

### ¿Qué significa “Sincronizar ahora”?

Fuerza heartbeat + consulta de cola. Si hay SMS pendientes en el panel, el teléfono los toma.

### ¿Por qué debo quitar ahorro de batería?

Android puede dormir el servicio y cortar la sincronización automática. Por eso se debe configurar batería sin restricciones.

### ¿Qué debe verse en la terminal?

Debe verse algo similar a:

```text
[21:30:02] SERVIDOR ACTIVADO: teléfono encendido y sincronizando
[21:30:03] CONECTADO: heartbeat OK
[21:30:04] SINCRONIZACIÓN: 2 SMS tomados del panel
[21:30:05] ENVIANDO: SMS a 1809...
[21:30:08] ENVIADO: 1809...
```
