# FixTracker

Aplicación Android para llevar el mantenimiento de tus vehículos sin ruido: kilometraje, servicios, documentación, avisos y gasto, en un solo lugar.

> **Versión 0.1.0 — primer boceto funcional.** Interfaz completa en español, datos de ejemplo y almacenamiento local. Pensada para revisar la dirección visual y la experiencia antes de añadir sincronización o notificaciones.

---

## La idea

La mayoría de apps de mantenimiento se sienten como un taller: formularios, tablas y avisos rojos. FixTracker quiere sentirse como el cuaderno bien cuidado de alguien que quiere a su coche: sobrio, legible y agradable de abrir.

Tres decisiones guían el diseño:

1. **Una sola pregunta al abrir:** ¿cómo está mi vehículo ahora mismo? La portada responde con kilometraje, próximo servicio y avisos.
2. **El dato justo, bien presentado.** Sin gráficos innecesarios; un anillo de servicio, una barra de gasto y un historial claro.
3. **Calma visual.** Fondo hueso, verde bosque profundo y cobre. Tipografía editorial (Fraunces) para los momentos importantes y Manrope para todo lo demás.

## Qué incluye este primer boceto

| Pantalla | Qué hace |
| --- | --- |
| **Inicio** | Saludo contextual, vehículo activo, kilometraje, próximo aceite con anillo de progreso, accesos rápidos (servicio, actualizar km, avisos), próximos avisos, gasto de los últimos 6 meses y actividad reciente. |
| **Garaje** | Lista de coches y motos con kilometraje, próximo servicio, inversión total y estado activo. Permite añadir vehículos. |
| **Ficha de vehículo** | Datos, gasto total/anual, documentación (ITV y seguro con vigencia), avisos, historial y acciones de edición, activación o borrado. |
| **Historial** | Búsqueda por servicio, taller o vehículo, filtros por tipo y agrupación mensual con totales. |
| **Nuevo servicio** | Tipo, concepto, fecha, kilometraje, coste, taller y notas. Al guardar aceite, ITV o seguro se reprograman automáticamente sus avisos. |
| **Recordatorios** | Agrupados por vencidos, próximos 30 días, más adelante y completados. Se pueden marcar como hechos y crear nuevos. |
| **Ajustes** | Nombre, tema claro/oscuro/sistema, moneda (EUR/USD/GBP), exportación JSON, datos de ejemplo y privacidad. |

Todo se guarda en el dispositivo en `fixtracker.json`; no hay red ni cuentas. El primer arranque carga un garaje de ejemplo (Alfa Romeo Giulietta + Vespa Primavera) con servicios y avisos fechados respecto a hoy para poder evaluar la interfaz con contenido real.

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
- Etiquetas en versalitas con tracking amplio para separar secciones sin gritar.

**Formas y ritmo**

- Tarjetas de 20–28 dp de radio, bordes de 1 dp casi invisibles y sombras solo en la barra flotante.
- Barra inferior flotante con cuatro pestañas y un botón central de cobre para añadir un servicio.
- Iconografía lineal de Material, tintada por tipo de servicio (aceite, frenos, neumáticos, ITV, seguro…).

## Stack técnico

- **Kotlin 2.2** + **Jetpack Compose** (Material 3, BOM 2025.09).
- **minSdk 26**, **target/compileSdk 36**.
- Persistencia local con `org.json` y un repositorio reactivo (`StateFlow`); sin base de datos ni dependencias de red.
- Navegación propia con pila de pantallas y `BackHandler` para mantener el APK ligero.
- Sin permisos: no hay notificaciones, cámara, ubicación ni acceso a internet.

## Compilar

Requisitos: JDK 17+ y Android SDK 36 con Build Tools 36.0.0.

```bash
./gradlew :app:assembleDebug
```

APK resultante:

```
app/build/outputs/apk/debug/app-debug.apk
```

Copia lista para instalar:

```
dist/FixTracker-0.1.0-boceto.apk
```

### Instalar en el móvil

1. Copia `dist/FixTracker-0.1.0-boceto.apk` al teléfono (cable, Drive, Telegram…).
2. Ábrelo desde el gestor de archivos y acepta «Instalar apps desconocidas» para esa app.
3. La APK está firmada en modo debug: sirve para revisión, no para publicar en Play Store.

Instalar en un dispositivo conectado:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> El proyecto se compiló con JDK 21 (Temurin) y Gradle 8.13. `local.properties` apunta al SDK local y está excluido del repositorio; Android Studio lo regenera.

## Estructura

```
app/src/main/java/com/fixtracker/app/
├── MainActivity.kt
├── data/            modelos, datos de ejemplo y repositorio JSON
├── ui/
│   ├── FixTrackerApp.kt   navegación y barra inferior
│   ├── components/        tarjetas, campos, chips, gráficos
│   ├── screens/           inicio, garaje, ficha, historial, formularios…
│   └── theme/             color, tipografía y formas
└── util/            formato de fechas, km y moneda (es-ES)
```

## Siguiente iteración (propuesta)

1. **Notificaciones locales** para avisos (WorkManager + permiso en Android 13+).
2. **Editar servicios** y adjuntar fotos de facturas.
3. **Importar** la copia JSON exportada y copia de seguridad automática.
4. **Recordatorios por kilometraje** con estimación de km/mes.
5. **Multi-vehículo compartido** (exportar/importar entre dispositivos antes de plantear cuenta y nube).
6. **Accesibilidad**: revisar contrastes en modo oscuro, tamaños de toque y escalado de fuente.
7. **Widget** con próximo servicio y kilometraje.

Los detalles de producto para Jira están en [`docs/BACKLOG.md`](docs/BACKLOG.md).

## Privacidad

No se recopila ni se transmite ningún dato. La exportación es una acción manual que abre el selector de compartir del sistema con el JSON generado.
