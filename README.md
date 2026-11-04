# FixTracker

Aplicación Android para llevar el mantenimiento de tus vehículos sin ruido: servicios, avisos, documentos, combustible y gasto, en un solo lugar.

> **Versión 0.2.0 — “Garaje completo”.** Segunda entrega del boceto: avisos reales, edición, copias de seguridad, facturas, combustible, plan de mantenimiento, informe PDF y bloqueo biométrico. Todo sigue siendo local y sin cuentas.

---

## La idea

La mayoría de apps de mantenimiento se sienten como un taller: formularios, tablas y avisos rojos. FixTracker quiere sentirse como el cuaderno bien cuidado de alguien que quiere a su coche: sobrio, legible y agradable de abrir.

Tres decisiones guían el diseño:

1. **Una sola pregunta al abrir:** ¿cómo está mi vehículo ahora mismo? La portada responde con kilometraje, próximo servicio, salud y avisos.
2. **El dato justo, bien presentado.** Un anillo de servicio, una barra de gasto, un plan claro y un historial con fotos.
3. **Calma visual.** Fondo hueso, verde bosque profundo y cobre. Tipografía editorial (Fraunces) para los momentos importantes y Manrope para todo lo demás.

## Novedades de la versión 0.2.0

- **Avisos locales** con permiso en Android 13+, resumen diario/semanal y acceso directo desde la notificación.
- **Onboarding**: elige nombre, datos de ejemplo o garaje vacío, y activa los avisos.
- **Editar servicios** y **deshacer** la eliminación desde el aviso inferior.
- **Copias de seguridad**: ZIP con datos y fotos, JSON solo datos, importación con confirmación y copia local automática.
- **Fotos de facturas** en cada servicio, hasta tres por registro.
- **Combustible**: repostajes, precio por litro, consumo real L/100 km, coste por km y gasto mensual.
- **Plan de mantenimiento** por tipo de vehículo, con estado vencido/próximo/al día.
- **Kilometraje**: media mensual, evolución y aviso suave para actualizar el cuentakilómetros.
- **Impuesto de circulación** junto a ITV y seguro, con sus avisos.
- **Informe PDF** del vehículo para compartir o guardar.
- **Bloqueo biométrico** opcional (huella, cara o PIN del dispositivo).
- **Exportación CSV** del historial y **desglose de gastos** por categoría.
- **Posponer avisos** una semana o un mes.
- **Selector de vehículo activo**, estado de salud en el garaje y menú rápido desde el botón central.
- Pruebas unitarias y workflow de GitHub Actions.

## Qué incluye

| Pantalla | Qué hace |
| --- | --- |
| **Inicio** | Saludo, vehículo activo con selector, salud, kilometraje, anillo de próximo aceite, accesos rápidos (servicio, combustible, km, avisos), avisos, gasto de 6 meses y actividad reciente. |
| **Garaje** | Coches y motos con kilometraje, próximo servicio, gasto total y estado de salud. |
| **Ficha de vehículo** | Datos, km/mes y evolución, documentación (ITV, seguro, impuesto), avisos, plan, combustible, desglose anual, historial, informe PDF y acciones. |
| **Combustible** | Consumo medio y último, precio medio, gasto del año, coste/km, gráfico de 6 meses y repostajes agrupados. |
| **Plan de mantenimiento** | Aceite, filtros, frenos, neumáticos, revisión, ITV, seguro e impuesto con progreso y estado. |
| **Historial** | Búsqueda, filtros por tipo y agrupación mensual. |
| **Nuevo/editar servicio** | Tipo, concepto, fecha, km, coste, taller, notas y fotos. Reprograma avisos de aceite, ITV y seguro. |
| **Recordatorios** | Agrupados por urgencia, posponer, completar y crear. |
| **Ajustes** | Tema, moneda, presupuesto, avisos, informe PDF, copias ZIP/JSON/CSV, restauración, biometría y privacidad. |

## Sistema visual

**Paleta**

| Uso | Claro | Oscuro |
| --- | --- | --- |
| Fondo | `#F5F3EE` hueso | `#0F1312` |
| Superficie | `#FFFFFF` | `#171C1A` |
| Tinta | `#171B1A` | `#ECEFEA` |
| Verde bosque | `#1E4A3D` | `#9AC7B1` |
| Cobre | `#B8733D` | `#D79B6C` |
| Línea | `#E3DED4` | `#2A322E` |

**Tipografía**

- **Fraunces** (SemiBold / Medium, instancia óptica 72) para titulares, cifras grandes y nombre de vehículo.
- **Manrope** (400/500/600/700) para cuerpo, etiquetas y navegación.

**Formas y ritmo**

- Tarjetas de 20–28 dp, bordes de 1 dp casi invisibles y sombras solo en la barra flotante.
- Barra inferior flotante con cuatro pestañas y un botón central de cobre que abre el menú rápido.
- Iconografía lineal de Material, tintada por tipo de servicio.

## Stack técnico

- **Kotlin 2.2** + **Jetpack Compose** (Material 3, BOM 2025.09).
- **minSdk 26**, **target/compileSdk 36**.
- Persistencia local con `org.json` y un repositorio reactivo (`StateFlow`).
- **WorkManager** para el chequeo diario de avisos y **NotificationCompat** para los canales.
- **BiometricPrompt** para el bloqueo opcional.
- **PdfDocument** para el informe; **SAF** para importar/exportar copias.
- **ExifInterface** y compresión JPEG para las fotos de facturas.
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

### Instalar en el móvil

1. Copia `dist/FixTracker-0.2.0-boceto.apk` al teléfono.
2. Ábrelo desde el gestor de archivos y acepta «Instalar apps desconocidas» para esa app.
3. La APK está firmada en modo debug: sirve para revisión, no para publicar en Play Store.

## Estructura

```
app/src/main/java/com/fixtracker/app/
├── MainActivity.kt        onboarding, bloqueo y arranque
├── FixTrackerApplication.kt
├── data/                 modelos, repositorio JSON, copias, semillas e insights
├── notifications/        canal, worker y planificador de avisos
├── ui/
│   ├── FixTrackerApp.kt  navegación, menú rápido y snackbar
│   ├── components/       tarjetas, campos, chips, gráficos y sparkline
│   ├── screens/          inicio, garaje, ficha, combustible, plan, ajustes…
│   └── theme/            color, tipografía y formas
└── util/                 formato, fotos, CSV y PDF
```

## Datos y privacidad

- Todo vive en `filesDir/fixtracker.json`; las fotos, en `filesDir/attachments`.
- Cada escritura guarda una copia local automática en `fixtracker.backup.json`.
- La copia ZIP incluye datos y fotos; la JSON solo los datos.
- No hay acceso a internet, analítica ni cuentas. La exportación es siempre manual.

## Jira

El backlog vivo está en el proyecto **FIX**: [FixTracker en Jira](https://jaxsullivan7.atlassian.net/jira/software/projects/FIX/summary). Este repo mantiene `docs/BACKLOG.md`, `docs/jira-issues.json` y `docs/jira-execution.json` como referencia local.

### Plan de equipo

Equipo simulado de **5 personas**: 2 frontend, 2 backend y 1 QA. Las 54 tareas de ejecución llevan etiquetas `fe-1`, `fe-2`, `be-1`, `be-2` y `qa`, estimación original en horas y puntos (1 punto ≈ 1 día).

**Supuesto de capacidad:** 50 h productivas por persona y sprint (5 personas → 250 h/sprint). La carga neta planificada es de 162–196 h por sprint; el resto cubre coordinación, soporte y margen. Total: **54 tareas, 820 h ideales y 118 puntos**.

| Sprint | Fechas | Tareas | Horas | Puntos | Estado |
| --- | --- | ---: | ---: | ---: | --- |
| 1 · Fundaciones | 7–18 sep 2026 | 12 | 162 | 24 | Cerrado |
| 2 · Núcleo del garaje | 21 sep–2 oct 2026 | 11 | 188 | 27 | Cerrado |
| 3 · Confianza | 5–16 oct 2026 | 11 | 180 | 26 | Cerrado |
| 4 · Contexto y valor | 19–30 oct 2026 | 11 | 196 | 27 | Cerrado |
| 5 · Revisión | 2–13 nov 2026 | 9 + 5 historias | 94 | 14 | Activo |
| **Total** | | **54** | **820** | **118** | |

Reparto de horas por rol: FE-1 176 h · FE-2 178 h · BE-1 146 h · BE-2 148 h · QA 172 h.

> Los puntos de la tabla son los de las tareas de ejecución; las historias de producto conservan sus propios puntos. El Sprint 5 incluye además la revisión y las historias parciales de accesibilidad, tests y avisos por km.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

Cubren cálculo de consumo, estimación de kilometraje, plan de mantenimiento, salud del vehículo y formatos. El workflow `.github/workflows/android.yml` ejecuta tests y compila la APK en cada push.

## Siguiente iteración

1. Widget de próximo servicio y kilometraje.
2. Archivos PDF/fotos para los documentos del vehículo, no solo fechas.
3. Presupuesto anual visible también en la portada.
4. Notificaciones con acciones («Hecho», «Posponer») sin abrir la app.
5. Sincronización y vehículo compartido (requiere backend y cuentas).
6. OCR de facturas e importación de datos de talleres.
7. Publicación en Play Store: firma de release, ficha y política de privacidad.
