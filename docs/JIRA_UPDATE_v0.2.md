# Jira · actualización v0.2.0 (pendiente de sesión)

La sesión del navegador de Jira caducó durante el trabajo. Este documento deja preparado el cambio para aplicarlo en cuanto se vuelva a iniciar sesión (o con un API token). El código y la APK ya están listos.

## Pasar a Done

| Jira | Historia | Evidencia |
| --- | --- | --- |
| FIX-25 | Notificaciones locales | Worker diario, canal, permiso Android 13+, digest y test desde Ajustes. |
| FIX-19 | Editar servicio | Formulario reutilizado con `recordId`, recálculo de avisos. |
| FIX-46 | Importar JSON | Importación JSON/ZIP con validación y confirmación. |
| FIX-47 | Copia automática | Copia local antes de cada escritura + ZIP completo. |
| FIX-21 | Adjuntar factura | Hasta 3 fotos por servicio y visor. |
| FIX-27 | Posponer aviso | Una semana o un mes, con indicador. |
| FIX-14 | Historial de kilometraje | Sparkline, km/mes estimado y aviso de actualización. |
| FIX-32 | Desglose por tipo | Tarjeta de gastos del año por categoría. |
| FIX-33 | Presupuesto anual | Ajuste y progreso presupuesto vs. gasto. |
| FIX-37 | Compartir ficha PDF | Informe PDF con historial, plan, documentos y combustible. |
| FIX-22 | Exportar CSV | Servicios y repostajes. |
| FIX-41 | Onboarding | Tres decisiones: nombre, demo/cero y avisos. |
| FIX-49 | Migraciones | Esquema v2 con valores por defecto y copia de seguridad. |

## Pasar a In Progress

| Jira | Historia | Nota |
| --- | --- | --- |
| FIX-51 | Tests | Unitarios de insights/formatos y CI; faltan UI tests. |
| FIX-26 | Avisos por kilometraje | Avisos por km y estimación km/mes; falta previsión de fecha por ritmo. |
| FIX-40 | Accesibilidad | Etiquetas y tamaños; falta auditoría TalkBack/fuente grande. |
| FIX-43 | Animaciones | Fundido de pantallas; falta respetar reducir movimiento. |

## Nuevas historias

| Épica | Summary | Descripción | Prioridad | Puntos | Estado |
| --- | --- | --- | --- | --- | --- |
| FIX-4 · Gastos | Registro de combustible y consumo | Repostajes con litros, coste, odómetro y depósito lleno. Consumo L/100, coste/km y gasto mensual. | P0 | 8 | Done |
| FIX-1 · Garaje | Salud del vehículo | Estado Al día / Próximo / Vencido combinando avisos, documentos, ITV y km. | P1 | 2 | Done |
| FIX-6 · Diseño | Menú rápido de registro | Botón central con servicio, repostaje, recordatorio y vehículo. | P1 | 2 | Done |
| FIX-7 · Datos | Copia completa con fotos | ZIP con JSON + adjuntos y restauración con confirmación. | P0 | 3 | Done |
| FIX-7 · Datos | Bloqueo biométrico | Huella/cara o PIN del dispositivo al abrir la app, opcional. | P1 | 3 | Done |
| FIX-5 · Documentos | Impuesto de circulación | Fecha, ficha y aviso automático junto a ITV y seguro. | P1 | 2 | Done |
| FIX-1 · Garaje | Aviso de kilometraje | Aviso suave si el cuentakilómetros lleva más de 45 días sin actualizar. | P2 | 1 | Done |
| FIX-3 · Avisos | Resumen semanal | Digest de lunes incluido en el chequeo diario. | P2 | 2 | Done |

## Al aplicar

1. Transicionar las historias de la primera tabla a `Done` y las de la segunda a `In Progress`.
2. Crear las 7 historias nuevas bajo su épica con los puntos y prioridad indicados.
3. Pasar esas 7 historias a `Done`.
4. Añadir un comentario a cada épica con el progreso v0.2.0.
5. Adjuntar `dist/FixTracker-0.2.0-boceto.apk` a `FIX-55` como build de revisión.
