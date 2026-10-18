# finanzas-app

App Android personal (local-first) que captura gastos desde notificaciones y SMS bancarios, sin depender de terceros.

## Estado actual
- Room (SQLite) con tablas: `transactions`, `unrecognized_messages`, `health_events`.
- `FinanceNotificationListener` y `SmsReceiver` -> `CaptureProcessor` -> `MessageParser` (plantillas regex).
- Deduplicación por huella (monto + comercio + ventana de 5 min).
- Pantalla **Salud del servicio** (permisos, listener, última captura, conteos 24 h).
- GitHub Actions compila el APK en cada push a `main`.

## Reglas del proyecto
- Nunca usar `fallbackToDestructiveMigration()`: cada cambio de esquema lleva su `Migration`.
- Subir `versionCode` en cada release (misma firma + mismo `applicationId` para actualizar encima).
- Las plantillas de `MessageParser.DEFAULT_TEMPLATES` son de ejemplo; ajustarlas a los mensajes reales.

## Firma del APK (GitHub Secrets)
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Sin ellos se genera un APK release sin firmar.
Guarda tu keystore en un lugar seguro: si cambia, Android obliga a desinstalar y se pierden los datos.

## Siguientes tareas
1. Bandeja "Sin reconocer" con asistente de plantillas.
2. Backup cifrado automático.
3. Aprendizaje de categorías y presupuesto diario.
