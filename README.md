# finanzas-app

App Android personal (local-first) que captura gastos desde notificaciones y SMS bancarios, sin depender de terceros.

## Estado actual (v0.6.0)
- Room (SQLite) v3: `transactions`, `unrecognized_messages`, `health_events`, `templates`, `category_rules` (migraciones 1→2 y 2→3).
- `FinanceNotificationListener` y `SmsReceiver` -> `CaptureProcessor` -> `MessageParser` (plantillas regex).
  Las plantillas del usuario (tabla `templates`) tienen prioridad sobre las de ejemplo.
- Deduplicación por huella (monto + comercio + ventana de 5 min).
- Pantallas: **Movimientos**, **Sin reconocer** (asistente para crear plantillas seleccionando el monto y el comercio
  en el mensaje, vista previa y reprocesado de pendientes del mismo remitente), **Salud del servicio**, **Ajustes**.
- **Rediseño** (v0.6.0): Inicio con la fecha en que quedas libre de deudas, edición de movimientos en un panel, simulador de abonos con barra, límites por categoría en Presupuesto y pestaña Más con Captura, Recurrentes, Por revisar y Respaldo. Fuente Instrument Sans (OFL, ver `licenses/`).
- **Cargos recurrentes** (v0.5.0): detecta comercios que cobran semanal, quincenal o mensualmente (3 cobros o más), estima el costo mensual, la próxima fecha y marca cambios de precio. Está en Más → Recurrentes.
- **Deudas** (v0.4.0): tarjetas, préstamos y deudas informales (yo debo / me deben), tabla de amortización con tasa EA,
  simulador de abonos extra, comparación avalancha vs bola de nieve, descuento automático de pagos detectados por palabra clave,
  y avisos diarios de vencimientos (3 días antes) y cupo de tarjeta alto (>=80 %). Base de datos en esquema v4. Pestaña "Más" = Salud + Ajustes.
- Categorías: al guardar una transacción se asigna categoría con (1) la regla aprendida del comercio, (2) "Ingresos" si el monto es positivo,
  (3) palabras clave por defecto (Rappi, Uber, Netflix, etc.). Al tocar un movimiento y cambiar su categoría se crea la regla
  y se recategorizan los anteriores del mismo comercio (`MerchantNormalizer` genera la clave: "RAPPI*COL 123" -> "rappi col").
- Presupuesto: límite de gasto por periodo y día de pago; el periodo va del último pago al próximo (el día 31 se ajusta en meses cortos).
  Muestra cuánto puedes gastar por día, lo gastado, lo disponible y el gasto por categoría.
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
- Al cambiar el esquema, actualizar `version` en `@Database` y `AppDatabase.SCHEMA_VERSION` (la usa `BackupManager` al restaurar).

## Firma del APK (GitHub Secrets)
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Sin ellos se genera un APK release sin firmar.
Guarda tu keystore en un lugar seguro: si cambia, Android obliga a desinstalar y se pierden los datos.

## Siguientes tareas
1. Módulo de deudas con amortización, simulador de abonos y alertas de pago.
2. Detección de suscripciones y gastos recurrentes, alertas de cobros inusuales.
3. Bloqueo biométrico y autoactualización desde GitHub Releases.
