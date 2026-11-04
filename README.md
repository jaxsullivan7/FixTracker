# FixTracker

AplicaciÃ³n Android para llevar el mantenimiento de tus vehÃ­culos sin ruido: servicios, avisos, documentos, combustible y gasto, en un solo lugar.

> **VersiÃ³n 0.2.0 â€” â€œGaraje completoâ€.** Segunda entrega del boceto: avisos reales, ediciÃ³n, copias de seguridad, facturas, combustible, plan de mantenimiento, informe PDF y bloqueo biomÃ©trico. Todo sigue siendo local y sin cuentas.

---

## La idea

La mayorÃ­a de apps de mantenimiento se sienten como un taller: formularios, tablas y avisos rojos. FixTracker quiere sentirse como el cuaderno bien cuidado de alguien que quiere a su coche: sobrio, legible y agradable de abrir.

Tres decisiones guÃ­an el diseÃ±o:

1. **Una sola pregunta al abrir:** Â¿cÃ³mo estÃ¡ mi vehÃ­culo ahora mismo? La portada responde con kilometraje, prÃ³ximo servicio, salud y avisos.
2. **El dato justo, bien presentado.** Un anillo de servicio, una barra de gasto, un plan claro y un historial con fotos.
3. **Calma visual.** Fondo hueso, verde bosque profundo y cobre. TipografÃ­a editorial (Fraunces) para los momentos importantes y Manrope para todo lo demÃ¡s.

## Novedades de la versiÃ³n 0.2.0

- **Avisos locales** con permiso en Android 13+, resumen diario/semanal y acceso directo desde la notificaciÃ³n.
- **Onboarding**: elige nombre, datos de ejemplo o garaje vacÃ­o, y activa los avisos.
- **Editar servicios** y **deshacer** la eliminaciÃ³n desde el aviso inferior.
- **Copias de seguridad**: ZIP con datos y fotos, JSON solo datos, importaciÃ³n con confirmaciÃ³n y copia local automÃ¡tica.
- **Fotos de facturas** en cada servicio, hasta tres por registro.
- **Combustible**: repostajes, precio por litro, consumo real L/100 km, coste por km y gasto mensual.
- **Plan de mantenimiento** por tipo de vehÃ­culo, con estado vencido/prÃ³ximo/al dÃ­a.
- **Kilometraje**: media mensual, evoluciÃ³n y aviso suave para actualizar el cuentakilÃ³metros.
- **Impuesto de circulaciÃ³n** junto a ITV y seguro, con sus avisos.
- **Informe PDF** del vehÃ­culo para compartir o guardar.
- **Bloqueo biomÃ©trico** opcional (huella, cara o PIN del dispositivo).
- **ExportaciÃ³n CSV** del historial y **desglose de gastos** por categorÃ­a.
- **Posponer avisos** una semana o un mes.
- **BÃºsqueda de vehÃ­culo activo** desde la cabecera, estado de salud en el garaje y menÃº rÃ¡pido desde el botÃ³n central.
- Pruebas unitarias y workflow de GitHub Actions.

## QuÃ© incluye

| Pantalla | QuÃ© hace |
| --- | --- |
| **Inicio** | Saludo, vehÃ­culo activo con selector, salud, kilometraje, anillo de prÃ³ximo aceite, accesos rÃ¡pidos (servicio, combustible, km, avisos), avisos, gasto de 6 meses y actividad reciente. |
| **Garaje** | Coches y motos con kilometraje, prÃ³ximo servicio, gasto total y estado de salud. |
| **Ficha de vehÃ­culo** | Datos, km/mes y evoluciÃ³n, documentaciÃ³n (ITV, seguro, impuesto), avisos, plan, combustible, desglose anual, historial, informe PDF y acciones. |
| **Combustible** | Consumo medio y Ãºltimo, precio medio, gasto del aÃ±o, coste/km, grÃ¡fico de 6 meses y repostajes agrupados. |
| **Plan de mantenimiento** | Aceite, filtros, frenos, neumÃ¡ticos, revisiÃ³n, ITV, seguro e impuesto con progreso y estado. |
| **Historial** | BÃºsqueda, filtros por tipo y agrupaciÃ³n mensual. |
| **Nuevo/editar servicio** | Tipo, concepto, fecha, km, coste, taller, notas y fotos. Reprograma avisos de aceite, ITV y seguro. |
| **Recordatorios** | Agrupados por urgencia, posponer, completar y crear. |
| **Ajustes** | Tema, moneda, avisos, informe PDF, copias ZIP/JSON/CSV, restauraciÃ³n, presupuesto, biometrÃ­a y privacidad. |

## Sistema visual

**Paleta**

| Uso | Claro | Oscuro |
| --- | --- | --- |
| Fondo | `#F5F3EE` hueso | `#0F1312` |
| Superficie | `#FFFFFF` | `#171C1A` |
| Tinta | `#171B1A` | `#ECEFEA` |
| Verde bosque | `#1E4A3D` | `#9AC7B1` |
| Cobre | `#B8733D` | `#D79B6C` |
| LÃ­nea | `#E3DED4` | `#2A322E` |

**TipografÃ­a**

- **Fraunces** (SemiBold / Medium, instancia Ã³ptica 72) para titulares, cifras grandes y nombre de vehÃ­culo.
- **Manrope** (400/500/600/700) para cuerpo, etiquetas y navegaciÃ³n.

**Formas y ritmo**

- Tarjetas de 20â€“28 dp, bordes de 1 dp casi invisibles y sombras solo en la barra flotante.
- Barra inferior flotante con cuatro pestaÃ±as y un botÃ³n central de cobre que abre el menÃº rÃ¡pido.
- IconografÃ­a lineal de Material, tintada por tipo de servicio.

## Stack tÃ©cnico

- **Kotlin 2.2** + **Jetpack Compose** (Material 3, BOM 2025.09).
- **minSdk 26**, **target/compileSdk 36**.
- Persistencia local con `org.json` y un repositorio reactivo (`StateFlow`).
- **WorkManager** para el chequeo diario de avisos y **NotificationCompat** para los canales.
- **BiometricPrompt** para el bloqueo opcional.
- **PdfDocument** para el informe; **SAF** para importar/exportar copias.
- **ExifInterface** y compresiÃ³n JPEG para las fotos de facturas.
- Sin permiso de INTERNET ni cuentas. Solo se pide el permiso de notificaciones (Android 13+).

## Compilar

Requisitos: JDK 17+ y Android SDK 36 con Build Tools 36.0.0.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK resultante:

```
app/build/outputs/apk/debug/app-debug.apk
```

Copia lista para instalar:

```
dist/FixTracker-0.2.0-boceto.apk
```

### Instalar en el mÃ³vil

1. Copia `dist/FixTracker-0.2.0-boceto.apk` al telÃ©fono.
2. Ãbrelo desde el gestor de archivos y acepta Â«Instalar apps desconocidasÂ» para esa app.
3. La APK estÃ¡ firmada en modo debug: sirve para revisiÃ³n, no para publicar en Play Store.

## Estructura

```
app/src/main/java/com/fixtracker/app/
â”œâ”€â”€ MainActivity.kt        onboarding, bloqueo y arranque
â”œâ”€â”€ FixTrackerApplication.kt
â”œâ”€â”€ data/                 modelos, repositorio JSON, copias, semillas e insights
â”œâ”€â”€ notifications/        canal, worker y planificador de avisos
â”œâ”€â”€ ui/
â”‚   â”œâ”€â”€ FixTrackerApp.kt  navegaciÃ³n, menÃº rÃ¡pido y snackbar
â”‚   â”œâ”€â”€ components/       tarjetas, campos, chips, grÃ¡ficos y sparkline
â”‚   â”œâ”€â”€ screens/          inicio, garaje, ficha, combustible, plan, ajustesâ€¦
â”‚   â””â”€â”€ theme/            color, tipografÃ­a y formas
â””â”€â”€ util/                 formato, fotos, CSV y PDF
```

## Datos y privacidad

- Todo vive en `filesDir/fixtracker.json`; las fotos, en `filesDir/attachments`.
- Cada escritura guarda una copia local automÃ¡tica en `fixtracker.backup.json`.
- La copia ZIP incluye datos y fotos; la JSON solo los datos.
- No hay red, analÃ­tica ni cuentas. La exportaciÃ³n es siempre manual.

## Siguiente iteraciÃ³n

1. Widget de prÃ³ximo servicio y kilometraje.
2. Archivos PDF/fotos para los documentos del vehÃ­culo, no solo fechas.
3. Presupuesto anual visible en la portada y avisos de desvÃ­o.
4. Notificaciones con acciones (Â«HechoÂ», Â«PosponerÂ») sin abrir la app.
5. SincronizaciÃ³n y vehÃ­culo compartido (requiere backend y cuentas).
6. OCR de facturas e importaciÃ³n de datos de talleres.
7. PublicaciÃ³n en Play Store: firma de release, ficha y polÃ­tica de privacidad.

## Jira

El backlog vivo estÃ¡ en el proyecto **FIX**: [FixTracker en Jira](https://jaxsullivan7.atlassian.net/jira/software/projects/FIX/summary). Este repo mantiene `docs/BACKLOG.md` y `docs/jira-issues.json` como referencia local.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

Cubren cÃ¡lculo de consumo, estimaciÃ³n de kilometraje, plan de mantenimiento, salud del vehÃ­culo y formatos. El workflow `.github/workflows/android.yml` ejecuta tests y compila la APK en cada push.

