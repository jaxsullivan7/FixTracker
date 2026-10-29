# FixTracker · Backlog propuesto (Jira FIX)

> **Importado en Jira el 29/10/2026.** El backlog vivo está en el proyecto FIX: [tablero](https://jaxsullivan7.atlassian.net/jira/software/projects/FIX/boards/5/backlog). Se crearon 8 épicas, 46 historias y la tarea de revisión `FIX-55`. Este documento sigue siendo la fuente editable; `docs/jira-issues.json` guarda el mapeo de claves.
>
> Prioridades: **P0** imprescindible para el MVP, **P1** siguiente iteración, **P2** más adelante. Puntos en escala Fibonacci.

## Épicas

| ID | Épica | Objetivo | Estado v0.1 |
| --- | --- | --- | --- |
| FIX-E1 | Garaje y vehículos | Dar de alta y mantener los vehículos del usuario. | Base hecha |
| FIX-E2 | Mantenimiento e historial | Registrar servicios y consultar la vida del vehículo. | Base hecha |
| FIX-E3 | Avisos y notificaciones | Que el usuario no olvide una fecha o un kilometraje. | Avisos en app |
| FIX-E4 | Gastos y analítica | Entender cuánto cuesta mantener cada vehículo. | Base hecha |
| FIX-E5 | Documentación y archivos | ITV, seguro y facturas siempre a mano. | Fechas ITV/seguro |
| FIX-E6 | Diseño y accesibilidad | Una app sobria, clara y usable por todo el mundo. | Primera pasada |
| FIX-E7 | Datos, respaldo y sincronización | Que los datos no se pierdan y viajen con el usuario. | Local + export |
| FIX-E8 | Plataforma y publicación | Llevar la app a producción con calidad. | — |

---

## FIX-E1 · Garaje y vehículos

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-101 | Alta de vehículo | Formulario con marca, modelo, matrícula, año, km, combustible, tipo, color, ITV y seguro. Validación de marca/modelo. El vehículo aparece en el garaje y se marca como activo. | P0 | 3 |
| FIX-102 | Cambiar vehículo activo | Desde la ficha, un toque lo convierte en activo; Inicio e insignias se actualizan al instante. | P0 | 1 |
| FIX-103 | Editar vehículo | Se conservan servicios, avisos e identificador. Cambios visibles al volver. | P0 | 2 |
| FIX-104 | Eliminar vehículo | Confirmación explícita; se borran sus servicios y avisos y se activa otro vehículo si existe. | P0 | 3 |
| FIX-105 | Foto o ilustración del vehículo | Imagen opcional con recorte; sustituye el monograma en garaje y ficha. | P2 | 5 |
| FIX-106 | Historial de kilometraje | Gráfica de evolución y registro rápido de km con fecha. | P1 | 5 |

## FIX-E2 · Mantenimiento e historial

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-201 | Registrar servicio | Tipo, concepto, fecha, km, coste, taller y notas. El registro aparece en historial e Inicio; el km del vehículo sube si procede. | P0 | 5 |
| FIX-202 | Reprogramación automática | Al guardar aceite/ITV/seguro se actualiza la próxima fecha (y km de aceite) y se reemplaza el aviso correspondiente. | P0 | 3 |
| FIX-203 | Historial filtrable | Búsqueda por texto + filtro por tipo + agrupación mensual con total. | P0 | 3 |
| FIX-204 | Detalle de servicio | Muestra todos los campos, vehículo y coste; permite eliminar con confirmación. | P0 | 2 |
| FIX-205 | Editar servicio | Reutiliza el formulario; si cambia el tipo o la fecha, recalcula avisos. | P1 | 5 |
| FIX-206 | Plantillas de mantenimiento | Paquetes predefinidos por tipo de vehículo (aceite, filtros, correa…). | P1 | 8 |
| FIX-207 | Adjuntar factura | Foto o PDF asociado al servicio, guardado local y visible en el detalle. | P1 | 8 |
| FIX-208 | Exportar historial a CSV | Descarga con fecha, tipo, km, coste y taller. | P2 | 3 |

## FIX-E3 · Avisos y notificaciones

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-301 | Panel de avisos | Agrupados en vencidos, próximos 30 días, más adelante y completados. | P0 | 3 |
| FIX-302 | Completar y crear avisos | Marcar/desmarcar como hecho y crear aviso con título, vehículo y fecha. | P0 | 2 |
| FIX-303 | Notificaciones locales | Aviso a 30/7/1 días y el día del vencimiento; permiso en Android 13+; canal y ajustes. | P1 | 8 |
| FIX-304 | Avisos por kilometraje | Estimación de km/mes a partir de registros para avisar «te quedan ~500 km». | P1 | 5 |
| FIX-305 | Posponer aviso | «Recordar más tarde» 1 semana/1 mes sin marcarlo como hecho. | P2 | 2 |
| FIX-306 | Resumen semanal | Notificación opcional los lunes con el estado del garaje. | P2 | 3 |

## FIX-E4 · Gastos y analítica

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-401 | Gasto mensual | Total del mes y barras de los últimos 6 meses por vehículo. | P0 | 3 |
| FIX-402 | Gasto total y anual | En ficha y ajustes, sin duplicar servicios. | P0 | 1 |
| FIX-403 | Comparativa mensual | Porcentaje frente al mes anterior con semántica clara (sube = cobre, baja = verde). | P0 | 1 |
| FIX-404 | Desglose por tipo | Donut/barras por aceite, frenos, seguro… y coste medio por km. | P1 | 5 |
| FIX-405 | Presupuesto anual | Objetivo de gasto con progreso y aviso al superarlo. | P2 | 5 |

## FIX-E5 · Documentación y archivos

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-501 | Vigencia de ITV y seguro | Fechas editables y chip Vigente / Próximo / Vencido. | P0 | 2 |
| FIX-502 | Avisos de documentación | Generar/actualizar avisos al guardar las fechas. | P0 | 2 |
| FIX-503 | Guardar documentos | Adjuntar imagen/PDF de póliza e ITV con visor básico. | P2 | 8 |
| FIX-504 | Compartir ficha del vehículo | Exportar PDF con datos, historial y próximos servicios. | P2 | 5 |

## FIX-E6 · Diseño y accesibilidad

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-601 | Sistema de diseño | Tokens de color, tipografía, formas y componentes reutilizables. | P0 | 3 |
| FIX-602 | Tema claro/oscuro/sistema | Contraste AA, iconos de barra correctos y persistencia de la preferencia. | P0 | 3 |
| FIX-603 | Accesibilidad | Etiquetas semánticas, targets ≥48 dp, escalado de fuente sin cortes, TalkBack revisado. | P1 | 5 |
| FIX-604 | Onboarding | Tres pasos: qué hace, añade vehículo, permite avisos. Solo primer arranque. | P1 | 3 |
| FIX-605 | Estados vacíos y errores | Copy claro y acción siguiente en cada pantalla. | P1 | 2 |
| FIX-606 | Animaciones | Transiciones de pantalla y microinteracciones respetando «reducir movimiento». | P2 | 3 |

## FIX-E7 · Datos, respaldo y sincronización

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-701 | Persistencia local | Todo sobrevive al cierre y reinicio; escritura atómica. | P0 | 3 |
| FIX-702 | Exportar JSON | Compartir copia con esquema versionado. | P0 | 2 |
| FIX-703 | Importar JSON | Validación, previsualización y fusión o reemplazo. | P1 | 5 |
| FIX-704 | Copia automática | Backup diario local + exportación periódica opcional. | P1 | 5 |
| FIX-705 | Sincronización multi-dispositivo | Cuenta, resolución de conflictos y borrado seguro. | P2 | 13 |
| FIX-706 | Migraciones de esquema | Versionado y migración sin pérdida de datos. | P1 | 3 |

## FIX-E8 · Plataforma y publicación

| ID | Historia | Criterio de aceptación | Prio | Pts |
| --- | --- | --- | --- | --- |
| FIX-801 | Firma de release | Keystore seguro, versionCode automático, APK/AAB firmados. | P1 | 3 |
| FIX-802 | Tests | Unitarios de repositorio y fechas; UI tests de flujos críticos. | P1 | 8 |
| FIX-803 | Widget | Próximo servicio y km en la pantalla de inicio. | P2 | 5 |
| FIX-804 | Ficha de Play Store | Icono, capturas, descripción, política de privacidad y clasificación. | P2 | 5 |
| FIX-805 | Crash reporting y analítica | Opt-in, sin datos personales; eventos de uso mínimos. | P2 | 3 |

---

## Flujo recomendado

1. Cerrar el **MVP** con FIX-E1, E2, E3 (sin notificaciones), E4 (base), E6 (base) y E7 (local + export). Es lo que ya cubre el boceto.
2. Iteración 1.1: edición de servicios, importación JSON, notificaciones locales y accesibilidad.
3. Iteración 1.2: facturas, analítica avanzada y widget.
4. Solo después: cuentas y sincronización; antes conviene validar que el uso local es suficiente.

## Definición de hecho

- Criterio de aceptación verificado en dispositivo real (Android 10 y 14+).
- Funciona en tema claro y oscuro, con fuente al 130 %.
- Sin permisos nuevos sin justificación y sin datos enviados a red.
- Casos vacíos y error contemplados; textos en español revisados.
- `assembleRelease` firmado y migración de esquema probada si aplica.
