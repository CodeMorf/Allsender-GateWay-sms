# Allsender SMS Gateway v9 — Diagnostic Compile Fix

Corrige el error de Kotlin de la tarjeta Diag.

## Qué se corrigió

- Las variables de permisos estaban insertadas por error dentro de PanelScreen.
- Se movieron a DiagnosticsScreen.
- Se eliminó la declaración duplicada de isServerActive en PanelScreen.
- Se conserva:
  - botón para revisar permisos,
  - botón notificación,
  - botón batería,
  - activar servidor + notificación permanente,
  - ForegroundService,
  - WakeLock,
  - BootReceiver.

## Subir a GitHub

```powershell
git init
git config user.name "CodeMorf"
git config user.email "it@codemorf.tech"
git branch -M main
git remote remove origin 2>$null
git remote add origin git@github.com:CodeMorf/Allsender-GateWay-sms.git
git add -A
git commit -m "Fix final compilacion diag SMS Gateway"
git push -u origin main --force
```
