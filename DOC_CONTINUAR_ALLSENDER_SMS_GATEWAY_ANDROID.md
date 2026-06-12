# Continuación — Allsender SMS Gateway Android

Fecha: 2026-06-12  
Proyecto: **Allsender SMS Gateway Android**  
Repo GitHub: `git@github.com:CodeMorf/Allsender-GateWay-sms.git`  
Dominio backend: `https://auth.allsender.tech`  
Package definitivo: `tech.allsender.smsgateway`

---

## 1. Estado general

La app Android ya está bastante avanzada, pero **todavía no está cerrada**.

El backend y la web de Allsender sí están funcionando para crear y entregar mensajes SMS pendientes.

El problema actual está en la app Android / compilación / servicio en segundo plano.

---

## 2. Lo que ya funciona

### Backend Allsender

Endpoints probados:

```text
GET  /api/v1/sms/gateway?token=...
POST /api/v1/sms/gateway/status
POST /api/v1/sms/gateway/heartbeat
POST /api/v1/sms/inbound
POST /api/v1/sms/send
```

El backend ya pudo:

```text
Crear gateway.
Guardar SMS desde la web.
Devolver mensajes pendientes con polling manual.
Recibir heartbeat.
Recibir inbound.
Recibir status sent/failed.
```

La prueba manual con `curl` confirmó que el backend devuelve mensajes:

```json
{
  "ok": true,
  "messages": [
    {
      "id": "4",
      "to": "18099919972",
      "message": "Hola"
    }
  ]
}
```

Eso significa:

```text
✅ Allsender Web crea SMS queued.
✅ API polling devuelve mensajes.
✅ Backend no es el problema principal.
```

---

## 3. Problema detectado

En base de datos se ve:

```text
last_seen_at = actualizado
last_poll_at = null
SMS = queued
```

Eso indica:

```text
La app manda heartbeat, pero no hace polling automático correctamente,
o el servicio no queda realmente activo en segundo plano,
o la APK instalada no está ejecutando el código nuevo.
```

Luego se probó el botón de forzar sincronización / heartbeat y ahí sí envió.  
Conclusión:

```text
Cuando se fuerza heartbeat manualmente, la app puede sincronizar.
El modo automático/segundo plano todavía no queda estable.
```

---

## 4. Objetivo funcional final

La app debe funcionar así:

```text
1. Cliente instala APK.
2. Escanea QR desde Allsender Web.
3. Da permisos.
4. Toca “Activar servidor en segundo plano”.
5. Aparece notificación permanente visible.
6. El teléfono queda como servidor SMS local.
7. Cada X segundos hace:
   - heartbeat
   - polling de cola
   - envío de SMS pendientes
   - reporte sent/failed
8. Si el usuario sale de la app o bloquea el teléfono, sigue funcionando mientras la notificación esté visible.
9. El cliente solo debe apagarlo tocando “Apagar servidor”.
```

---

## 5. Importante sobre segundo plano en Android

Una corrutina sola NO mantiene viva la app.

Esto:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    // trabajo en segundo plano
}
```

solo mueve trabajo a un hilo en segundo plano, pero no evita que Android mate el proceso.

Para este proyecto se necesita:

```text
ForegroundService visible
+ Notificación permanente
+ CoroutineScope(SupervisorJob() + Dispatchers.IO)
+ WakeLock parcial
+ Batería sin restricciones
+ BootReceiver
+ Permiso de notificaciones
```

La notificación permanente es obligatoria.  
Si no aparece la notificación, el servidor realmente no quedó protegido.

---

## 6. Versiones / ZIPs generados

Últimos ZIPs trabajados:

```text
Allsender-GateWay-sms_SERVER_MODE_FINAL_v4.zip
Allsender-GateWay-sms_BACKGROUND_SERVER_v5.zip
Allsender-GateWay-sms_ULTRA_BACKGROUND_SERVER_v6.zip
Allsender-GateWay-sms_NOTIFICATION_DIAG_SERVER_v7.zip
Allsender-GateWay-sms_NOTIFICATION_DIAG_SERVER_v8_COMPILE_FIX.zip
Allsender-GateWay-sms_v9_DIAG_COMPILE_FIX.zip
```

Estado:

```text
v4: agregó terminal y modo servidor.
v5: cambió a servidor en segundo plano con remoteMessaging.
v6: agregó WakeLock, BootReceiver y auto-recuperación.
v7: agregó diagnóstico de permisos y notificación, pero rompió compilación.
v8: intentó corregir variables del diagnóstico.
v9: intentó corregir compilación, pero Codemagic sigue fallando con error Kotlin genérico.
```

---

## 7. Bloqueo actual

Codemagic falla en:

```text
Step: Build debug APK
Error:
org.jetbrains.kotlin.gradle.tasks.CompilationErrorException: Compilation error. See log for more details
```

Las últimas 50 líneas NO muestran el error real.  
Falta ver las líneas de Kotlin que empiezan con:

```text
e:
Unresolved reference
Type mismatch
No value passed
Redeclaration
Conflicting declarations
```

Sin esas líneas exactas, se está corrigiendo a ciegas.

---

## 8. Próximo paso recomendado

Antes de crear otro ZIP, hay que cambiar `codemagic.yaml` para imprimir el error Kotlin real.

Usar este workflow en `codemagic.yaml`:

```yaml
workflows:
  android-debug:
    name: Android Debug APK
    max_build_duration: 60
    instance_type: linux_x2

    scripts:
      - name: Check project
        script: |
          export PATH="/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"
          pwd
          ls -la
          ls -la app
          java -version
          uname -a

      - name: Install Gradle 9.3.1
        script: |
          export PATH="/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"
          curl -L https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -o /tmp/gradle-9.3.1-bin.zip
          unzip -q /tmp/gradle-9.3.1-bin.zip -d /tmp
          export PATH="/tmp/gradle-9.3.1/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"
          gradle --version

      - name: Create debug keystore
        script: |
          export PATH="/tmp/gradle-9.3.1/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"
          keytool -genkeypair -v \
            -keystore debug.keystore \
            -storepass android \
            -alias androiddebugkey \
            -keypass android \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -dname "CN=Android Debug,O=Android,C=US"
          ls -la debug.keystore

      - name: Build debug APK and force visible error
        script: |
          export PATH="/tmp/gradle-9.3.1/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

          echo "=== BUILD START ==="
          gradle clean assembleDebug --no-daemon --stacktrace --info > /tmp/allsender_sms_build.log 2>&1
          BUILD_STATUS=$?

          echo ""
          echo "================ ERROR REAL KOTLIN / ANDROID ================"
          grep -nEi "e: |error:|Unresolved reference|None of the following|Type mismatch|No value passed|Cannot access|Only safe|Smart cast|Overload resolution|Conflicting|Redeclaration|Manifest merger failed|Execution failed|Compilation error" /tmp/allsender_sms_build.log | tail -240 || true

          echo ""
          echo "================ ULTIMAS 320 LINEAS COMPLETAS ================"
          tail -320 /tmp/allsender_sms_build.log || true

          echo ""
          echo "================ BUILD STATUS: $BUILD_STATUS ================"

          exit $BUILD_STATUS

    artifacts:
      - app/build/outputs/**/*.apk
      - app/build/outputs/**/*.aab
      - app/build/reports/**/*
      - /tmp/allsender_sms_build.log
```

Luego ejecutar Codemagic otra vez y descargar/subir:

```text
Allsender-GateWay-sms_x_artifacts.zip
```

Ese artifact debe traer:

```text
/tmp/allsender_sms_build.log
```

Ahí saldrá el error exacto.

---

## 9. Comando Git para subir cualquier versión nueva

Desde PowerShell dentro de la carpeta del proyecto:

```powershell
git init
git config user.name "CodeMorf"
git config user.email "it@codemorf.tech"
git branch -M main
git remote remove origin 2>$null
git remote add origin git@github.com:CodeMorf/Allsender-GateWay-sms.git
git add -A
git commit -m "Fix SMS Gateway Android"
git push -u origin main --force
```

---

## 10. Comando servidor para revisar estado

En el servidor:

```bash
cd /www/wwwroot/auth.allsender.tech || exit 1

node - <<'NODE'
const postgres = require('postgres');
require('dotenv').config();
const sql = postgres(process.env.DATABASE_URL, { ssl: false });

(async () => {
  const gateways = await sql`
    SELECT id, team_id, name, status, is_active, send_enabled, polling_enabled,
           last_seen_at, last_poll_at, last_error
    FROM sms_gateways
    WHERE team_id = 41
    ORDER BY id DESC
  `;

  const sms = await sql`
    SELECT id, gateway_id, to_number, message, status, updated_at
    FROM sms_messages
    WHERE team_id = 41
    ORDER BY id DESC
    LIMIT 20
  `;

  console.log('=== GATEWAYS ===');
  console.table(gateways);
  console.log('=== SMS ===');
  console.table(sms);

  await sql.end();
})();
NODE
```

La señal correcta es:

```text
last_seen_at = actualizado
last_poll_at = actualizado
SMS = sent o failed
```

Si `last_seen_at` cambia pero `last_poll_at` queda null:

```text
La app hace heartbeat pero no está haciendo polling.
```

---

## 11. Comando para limpiar cola y dejar gateway activo

Ajustar `gatewayId` si cambia. Último gateway usado: `4`.

```bash
cd /www/wwwroot/auth.allsender.tech || exit 1

node - <<'NODE'
const postgres = require('postgres');
require('dotenv').config();
const sql = postgres(process.env.DATABASE_URL, { ssl: false });

(async () => {
  const gatewayId = 4;
  const teamId = 41;

  await sql`
    UPDATE sms_gateways
    SET is_active=false, status='offline', is_default=false, updated_at=NOW()
    WHERE team_id=${teamId} AND id<>${gatewayId}
  `;

  await sql`
    UPDATE sms_gateways
    SET is_active=true, status='online', is_default=true,
        send_enabled=true, polling_enabled=true, updated_at=NOW()
    WHERE id=${gatewayId}
  `;

  await sql`
    UPDATE sms_messages
    SET gateway_id=${gatewayId}, status='queued',
        processing_at=NULL, updated_at=NOW()
    WHERE team_id=${teamId}
      AND direction='outbound'
      AND status IN ('queued','processing')
  `;

  console.log('OK: gateway activo y cola lista.');
  await sql.end();
})();
NODE
```

---

## 12. Flujo de prueba final esperado

Después de compilar APK correcto:

```text
1. Desinstalar app vieja del teléfono.
2. Reiniciar teléfono.
3. Instalar APK nuevo.
4. Abrir app.
5. Escanear QR.
6. Ir a Diag.
7. Activar/revisar permisos.
8. Activar notificaciones.
9. Ir a batería y poner Sin restricciones.
10. Tocar “Activar servidor + notificación permanente”.
11. Verificar que aparece notificación permanente.
12. Enviar SMS desde Allsender Web.
13. Ver terminal en app:
    CONECTADO
    SINCRONIZACIÓN
    ENVIANDO
    ENVIADO o FALLÓ
14. Ver DB:
    last_poll_at actualizado.
```

---

## 13. Reglas importantes

No tocar ahora:

```text
Backend Allsender.
Endpoints.
QR.
Token.
Panel web.
Tablas.
WhatsApp.
Evolution.
Zernio.
```

El bloqueo actual es:

```text
Compilación Kotlin de app Android.
```

---

## 14. Prompt para continuar en otro chat

```text
Actúa como desarrollador senior Android Kotlin.

Estoy terminando la app Allsender SMS Gateway. El backend ya funciona. El problema actual es que Codemagic falla con Kotlin CompilationErrorException, pero las últimas 50 líneas no muestran el error real.

Necesito primero modificar codemagic.yaml para imprimir las líneas exactas e: de Kotlin y generar /tmp/allsender_sms_build.log como artifact.

Luego, con el log exacto, corregir solo los errores Kotlin sin rehacer la app.

Contexto:
- Repo: git@github.com:CodeMorf/Allsender-GateWay-sms.git
- Package: tech.allsender.smsgateway
- App usa ForegroundService remoteMessaging.
- Debe quedar como servidor SMS en segundo plano con notificación permanente.
- Tiene pantalla Diag para permisos reales.
- Backend funciona y no debe tocarse.
- No tocar QR, token ni endpoints.

Objetivo:
1. Hacer que compile APK.
2. Instalar en teléfono.
3. Activar servidor con notificación permanente.
4. Confirmar last_poll_at en DB.
```

---

## 15. Conclusión

Estado actual:

```text
Backend: OK.
Web: OK.
Polling endpoint: OK.
App diseño: avanzado.
Modo servidor: diseñado.
Notificación/Diag: en proceso.
APK final: pendiente por error Kotlin.
```

Próximo paso real:

```text
Obtener log Kotlin completo.
Corregir error exacto.
Compilar APK.
Probar notificación permanente y last_poll_at.
```
