# finanzas-app

App Android personal (local-first) que captura gastos desde notificaciones y SMS bancarios, sin depender de terceros.

## Estado actual (v0.2.0)
- Room (SQLite) v2: `transactions`, `unrecognized_messages`, `health_events`, `templates` (migración 1→2 incluida).
- `FinanceNotificationListener` y `SmsReceiver` -> `CaptureProcessor` -> `MessageParser` (plantillas regex).
  Las plantillas del usuario (tabla `templates`) tienen prioridad sobre las de ejemplo.
- Deduplicación por huella (monto + comercio + ventana de 5 min).
- Pantallas: **Movimientos**, **Sin reconocer** (asistente para crear plantillas seleccionando el monto y el comercio
  en el mensaje, vista previa y reprocesado de pendientes del mismo remitente), **Salud del servicio**, **Ajustes**.
- Backup cifrado: AES-GCM con clave PBKDF2-SHA256 (210.000 iteraciones), carpeta elegida con el Storage Access Framework,
  tarea diaria con WorkManager (batería no baja), últimos 7 backups, verificación tras escribir y restauración validada
  (integridad GCM + `PRAGMA integrity_check` + versión de esquema). La contraseña se guarda cifrada con el Android Keystore.
- GitHub Actions: pruebas unitarias + APK en cada push a `main`.

## Pendiente de verificar
- El código **no se ha compilado ni ejecutado en un dispositivo**: revisar el primer build de Actions.
- Las plantillas de `MessageParser.DEFAULT_TEMPLATES` son de ejemplo; ajustarlas a los mensajes reales.
- El backup nunca se ha probado en un Redmi/HyperOS real (permisos SAF y tarea en segundo plano).

## Reglas del proyecto
- Nunca usar `fallbackToDestructiveMigration()`: cada cambio de esquema lleva su `Migration` (y subir la versión de `AppDatabase`).
- Subir `versionCode` en cada release (misma firma + mismo `applicationId` para actualizar encima).
- Al cambiar el esquema, actualizar `MAX_SCHEMA_VERSION` en `BackupManager`.

## Firma del APK (GitHub Secrets)
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Sin ellos se genera un APK release sin firmar.
Guarda tu keystore en un lugar seguro: si cambia, Android obliga a desinstalar y se pierden los datos.

## Siguientes tareas
1. Aprendizaje de categorías (reglas comercio → categoría) y presupuesto diario.
2. Módulo de deudas con amortización y alertas de pago.
3. Bloqueo biométrico y autoactualización desde GitHub Releases.
