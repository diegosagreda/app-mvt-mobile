# AGENTS.md

## Proyecto

Este proyecto es la aplicación mobile **My Virtual Trainer** en Android/Kotlin/Jetpack Compose.

La aplicación está enfocada en atletas y entrenadores, con funcionalidades relacionadas con rutinas deportivas, seguimiento de progreso, comunicación, notificaciones, integración con Firebase y experiencia mobile premium.

## Stack principal

* Kotlin
* Android Studio
* Jetpack Compose
* Material 3
* Navigation Compose
* ViewModel
* StateFlow / Flow
* Firebase Auth
* Firestore
* Firebase Realtime Database
* Firebase Storage
* Firebase Cloud Messaging
* Gradle Kotlin DSL si aplica
* WorkManager si aplica
* Coil si aplica
* Media3 si aplica
* MPAndroidChart si aplica

## Objetivo de diseño

La aplicación debe sentirse:

* Profesional
* Moderna
* Limpia
* Deportiva
* Premium
* Mobile-first
* Fácil de usar
* Visualmente consistente
* Orientada al acompañamiento del atleta

Evitar interfaces saturadas, textos excesivos, componentes desordenados o diseños que parezcan improvisados.

La experiencia debe transmitir que el atleta tiene una guía clara, constante y confiable.

## Reglas generales de trabajo

* Antes de modificar archivos importantes, explicar brevemente qué se va a cambiar.
* No modificar archivos sensibles sin autorización explícita.
* No tocar credenciales, llaves privadas, keystores, archivos `.env`, archivos de firma ni configuraciones de producción.
* No hacer commits automáticamente.
* No cambiar de rama sin autorización.
* No ejecutar comandos destructivos sin aprobación explícita.
* No ejecutar:

    * `rm -rf`
    * `git reset --hard`
    * `git clean -fd`
    * `git push --force`
    * migraciones destructivas
    * cambios masivos sin revisión
* Mantener el estilo actual del proyecto.
* Preferir soluciones limpias, simples y mantenibles.
* Evitar sobreingeniería.
* No introducir dependencias nuevas sin justificar su necesidad.
* Después de modificar código, ejecutar validaciones de compilación cuando sea posible.
* Si no es posible compilar, explicar claramente el motivo.

## Reglas para Android/Kotlin

* Mantener código idiomático en Kotlin.
* Usar nombres claros para variables, funciones, estados y eventos.
* Evitar lógica pesada dentro de composables.
* Separar responsabilidades entre:

    * UI
    * Estado
    * ViewModel
    * Repositorios
    * Modelos
    * Servicios
* Evitar duplicación de código.
* Crear funciones o componentes reutilizables cuando aplique.
* Mantener compatibilidad con el minSdk actual del proyecto.
* No cambiar versiones de Gradle, Kotlin, Compose o Firebase sin autorización.
* No modificar configuración global del proyecto salvo que sea necesario y esté justificado.

## Reglas para Jetpack Compose

* Preferir Material 3.
* Usar `MaterialTheme` para colores, tipografía y formas.
* No hardcodear colores si ya existen tokens o tema definido.
* Crear composables pequeños, claros y reutilizables.
* Separar pantallas en estructura similar a:

    * `Screen`
    * `Content`
    * `Components`
    * `State`
    * `Events`
* Evitar composables gigantes.
* Evitar lógica de negocio dentro de composables.
* Usar `remember` y `derivedStateOf` solo cuando tenga sentido.
* Cuidar recomposiciones innecesarias.
* Usar `LazyColumn`, `LazyRow` o componentes lazy cuando haya listas.
* Agregar `Preview` cuando sea útil y no rompa el proyecto.
* Incluir estados visuales:

    * Loading
    * Empty
    * Error
    * Success
* Cuidar padding, espaciados, alineaciones y jerarquía visual.
* Diseñar primero para pantallas mobile entre 360dp y 430dp de ancho.

## Reglas de diseño profesional

* Priorizar claridad sobre decoración.
* Cada pantalla debe tener una acción principal evidente.
* Reducir texto innecesario.
* Usar cards, chips, iconos y estados visuales de forma consistente.
* Mantener jerarquía visual clara:

    * Título
    * Contexto breve
    * Acción principal
    * Información secundaria
* Usar espaciados consistentes.
* Preferir una escala de spacing:

    * 4dp
    * 8dp
    * 12dp
    * 16dp
    * 20dp
    * 24dp
    * 32dp
* Evitar colores sin propósito.
* Evitar sombras exageradas.
* Evitar componentes visualmente pesados.
* El diseño debe sentirse premium, no recargado.
* Cualquier rediseño debe mejorar usabilidad, no solo apariencia.

## Reglas para My Virtual Trainer

Cuando se trabaje en pantallas relacionadas con rutinas, atletas, entrenadores, progreso, notificaciones o conexión con servicios externos:

* El atleta debe entender qué debe hacer en menos de 3 segundos.
* Las rutinas deben mostrar claramente:

    * Estado
    * Fecha
    * Objetivo
    * Tipo de esfuerzo
    * Tipo de medición
    * Fases
    * Progreso
* Las pantallas deben reforzar acompañamiento, motivación y claridad.
* Usar feedback visual para estados:

    * Rutina realizada
    * Rutina parcial
    * Rutina pendiente
    * Rutina no realizada
* Evitar que la app parezca una tabla administrativa.
* Priorizar experiencia mobile antes que cantidad de información.
* Los textos deben ser breves, útiles y accionables.
* Las notificaciones deben ser claras, personalizadas y no invasivas.

## Reglas para Firebase

* No modificar reglas de seguridad sin autorización explícita.
* No cambiar estructura de colecciones o paths sin explicar impacto.
* No tocar configuración de producción sin aprobación.
* Evitar lecturas innecesarias de Firestore.
* Optimizar consultas cuando sea posible.
* Cuidar listeners en tiempo real para evitar consumo excesivo.
* Remover listeners correctamente cuando aplique.
* No exponer tokens, IDs sensibles o información privada.
* Para Cloud Messaging, validar:

    * Token FCM
    * Permisos de notificación
    * Canal de notificación Android
    * Ícono de notificación
    * Payload esperado

## Reglas para navegación

* Mantener rutas claras y consistentes.
* No duplicar destinos de navegación.
* Evitar pasar objetos complejos por argumentos de navegación.
* Preferir IDs o argumentos simples.
* Validar comportamiento del botón atrás.
* Cuidar estados cuando el usuario vuelve a una pantalla anterior.
* No romper flujos existentes sin explicación.

## Reglas para rendimiento

* Evitar consultas innecesarias a Firebase.
* Evitar cargar datos de meses, usuarios o rutinas que no se están mostrando.
* Usar paginación cuando aplique.
* Evitar recomposiciones innecesarias.
* Evitar imágenes pesadas sin optimización.
* Evitar listas no lazy para muchos elementos.
* Cuidar consumo de batería en procesos en background.
* Usar WorkManager solo cuando tenga sentido.

## Reglas para seguridad

Archivos que no deben modificarse sin autorización explícita:

* `google-services.json`
* Keystores
* Archivos `.jks`
* Archivos `.keystore`
* `.env`
* Archivos de firma
* Configuraciones de producción
* Reglas de Firebase
* Configuraciones CI/CD
* Archivos con credenciales
* Tokens
* Llaves privadas
* Configuración de billing
* Configuración de releases

No imprimir ni copiar credenciales en logs, comentarios o documentación.

## Reglas para dependencias

Antes de agregar una dependencia:

1. Explicar para qué se necesita.
2. Verificar si ya existe una solución en el proyecto.
3. Preferir librerías estables y mantenidas.
4. Evitar dependencias innecesarias solo por diseño visual.
5. No cambiar versiones globales sin autorización.

## Reglas para cambios visuales

Cuando se solicite mejorar una pantalla:

1. Revisar la estructura actual.
2. Detectar problemas de UX/UI.
3. Proponer mejora concreta.
4. Aplicar cambios manteniendo el estilo del proyecto.
5. Crear componentes reutilizables si aplica.
6. Validar compilación.
7. Resumir archivos modificados.

No hacer rediseños masivos sin dividirlos en pasos controlados.

## Reglas para componentes

Preferir componentes reutilizables como:

* `RoutineCard`
* `RoutineStatusBadge`
* `TrainingProgressCard`
* `CoachMessageCard`
* `AthleteSummaryCard`
* `EmptyState`
* `ErrorState`
* `LoadingState`
* `PrimaryActionButton`
* `SectionHeader`
* `InfoChip`
* `MetricCard`
* `StreakCard`
* `AchievementBadge`

Evitar duplicar diseños similares en varias pantallas.

## Reglas para estados de UI

Cada pantalla importante debe considerar:

* Cargando datos
* Sin datos
* Error de conexión
* Error de permisos
* Datos disponibles
* Acción completada
* Acción fallida

Los errores deben mostrarse con mensajes claros y accionables.

## Reglas para copywriting mobile

* Usar textos cortos.
* Evitar párrafos largos.
* Usar lenguaje claro y directo.
* Evitar mensajes técnicos para el usuario final.
* Los botones deben indicar acción concreta:

    * Ver rutina
    * Iniciar entrenamiento
    * Reintentar
    * Guardar cambios
    * Enviar mensaje
* Evitar textos genéricos como:

    * Click aquí
    * Aceptar
    * Continuar, cuando no sea claro qué continúa

## Reglas para notificaciones

Las notificaciones deben ser:

* Claras
* Cortas
* Personalizadas cuando sea posible
* Útiles para el atleta
* No invasivas
* Coherentes con el estado de la rutina

Validar siempre:

* Canal de notificación Android
* Ícono
* Título
* Cuerpo
* Acción al tocar la notificación
* Token FCM
* Permisos Android 13+
* Comportamiento en foreground y background

## Reglas para pruebas y validación

Después de cambios relevantes, intentar ejecutar:

```bash
./gradlew assembleDebug
```

Para tests:

```bash
./gradlew test
```

Para revisar errores detallados:

```bash
./gradlew build --stacktrace
```

Si la compilación falla:

* Mostrar el error principal.
* Explicar posible causa.
* Proponer corrección.
* No ocultar errores pendientes.

## Comandos útiles

### Compilar debug

```bash
./gradlew assembleDebug
```

### Ejecutar tests

```bash
./gradlew test
```

### Revisar errores de Gradle

```bash
./gradlew build --stacktrace
```

### Limpiar build

Usar solo con autorización si puede afectar tiempos o estado del proyecto:

```bash
./gradlew clean
```

## Rama actual

Trabajar sobre la rama actual del repositorio.

No cambiar de rama sin autorización explícita.

Antes de sugerir una nueva rama, explicar el motivo.

## Flujo recomendado

1. Analizar el código.
2. Identificar archivos involucrados.
3. Explicar brevemente qué se va a cambiar.
4. Proponer cambios si son relevantes.
5. Aplicar cambios.
6. Compilar o ejecutar validaciones disponibles.
7. Mostrar resumen de archivos modificados.
8. Indicar si hubo errores pendientes.
9. Sugerir próximos pasos solo si son necesarios.

## Flujo para tareas de diseño

1. Revisar pantalla actual.
2. Detectar problemas visuales o de experiencia.
3. Proponer mejora concreta.
4. Validar consistencia con el diseño general de la app.
5. Implementar componentes reutilizables.
6. Revisar responsive mobile.
7. Validar estados de loading, empty, error y success.
8. Compilar.
9. Resumir cambios.

## Flujo para tareas de Firebase

1. Identificar servicio involucrado:

    * Auth
    * Firestore
    * Realtime Database
    * Storage
    * Cloud Messaging
    * Cloud Functions
2. Revisar estructura actual.
3. Evitar cambios destructivos.
4. Optimizar lecturas y escrituras.
5. Validar errores comunes.
6. Mantener seguridad y privacidad.
7. Compilar o probar cuando sea posible.

## Criterios de calidad

Un cambio se considera correcto si:

* Compila.
* No rompe funcionalidades existentes.
* Mantiene estilo del proyecto.
* Mejora claridad o mantenibilidad.
* Evita duplicación innecesaria.
* Respeta arquitectura actual.
* No introduce credenciales ni riesgos de seguridad.
* Tiene una experiencia mobile clara.
* Es fácil de revisar.

## Respuesta esperada de Codex

Al finalizar una tarea, responder con:

* Resumen breve del cambio.
* Archivos modificados.
* Validaciones ejecutadas.
* Resultado de compilación.
* Errores pendientes si existen.
* Recomendación siguiente solo si aplica.

Ejemplo:

```md
Resumen:
- Se rediseñó la tarjeta de rutina para mejorar jerarquía visual.
- Se agregó estado vacío para días sin rutinas.
- Se reutilizó el tema actual del proyecto.

Archivos modificados:
- app/src/main/java/.../RoutinesScreen.kt
- app/src/main/java/.../components/RoutineCard.kt

Validación:
- ./gradlew assembleDebug

Resultado:
- Compilación exitosa.
```

## Prohibiciones importantes

No hacer lo siguiente sin autorización explícita:

* Commits
* Push
* Force push
* Cambios de rama
* Eliminación masiva de archivos
* Cambios en credenciales
* Cambios en configuración de producción
* Cambios en reglas Firebase
* Cambios de billing
* Cambios en keystores
* Actualizaciones grandes de dependencias
* Refactors masivos
* Migraciones de datos
* Reescrituras completas de pantallas sin aprobación

## Prioridad general

La prioridad es construir una aplicación mobile profesional, estable, mantenible y visualmente premium para My Virtual Trainer.

El código debe ser tan ordenado como la rutina de un atleta disciplinado: claro, repetible y sin peso muerto innecesario.
