# Allsender SMS Gateway — NOTIFICATION DIAG SERVER v8 COMPILE FIX

## Qué corrige

La versión v7 agregaba la tarjeta de diagnóstico, pero faltaban variables declaradas dentro de `DiagnosticsScreen`, por eso Codemagic fallaba con error Kotlin.

Este v8 corrige:

- `allCriticalReady`
- `sendSmsGranted`
- `receiveSmsGranted`
- `readPhoneGranted`
- `cameraGranted`
- `notificationsGranted`
- `batteryUnrestricted`
- `permissionLauncher`
- `openNotificationSettings`
- `openBatterySettings`
- `isServerActive`

## Mantiene

- Diagnóstico real de permisos.
- Botón para revisar permisos.
- Botón para notificación.
- Botón para batería.
- Botón para activar servidor + notificación permanente.
- Foreground service.
- WakeLock.
- BootReceiver.
- Modo servidor SMS en segundo plano.

## Comando Git

```powershell
git init
git config user.name "CodeMorf"
git config user.email "it@codemorf.tech"
git branch -M main
git remote remove origin 2>$null
git remote add origin git@github.com:CodeMorf/Allsender-GateWay-sms.git
git add -A
git commit -m "Fix compilacion diagnostico notificacion SMS Gateway"
git push -u origin main --force
```
