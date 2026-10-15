# Auditoría de la fase 1 — Viaje a Dragos

Fecha: 14 de octubre de 2026.

> **Estado actual:** base publicada `79d15c7`. Combate completo A/B/C e invariantes de habilidades terminados en local. `mvn clean test`: 202 pruebas pasan, sin fallos ni errores. Solo el jugador ejecuta la limpieza final, incluso muerto. Sin commit ni push. Los apartados anteriores conservan los hallazgos históricos.

## Alcance y resultado

Se han leído los 33 archivos Java de `src/main/java`, el `pom.xml` y los 14 archivos Java de `src/test/java`. Se han buscado referencias a atributos, habilidades, eventos y equipo. La lectura de los archivos de pruebas se limita a localizar dependencias de las firmas actuales: no se han creado, modificado, compilado ni ejecutado pruebas.

El código principal **compila con JDK 23**, sin cambios en el código fuente. Esto no acredita que las mecánicas funcionen: el combate completo está desactivado y varias funcionalidades están incompletas.

No se han añadido dependencias, patrones ni persistencia. Este informe es el único archivo añadido; los `.class` generados están en `target`, que ya figura en `.gitignore`. Los cambios previos del autor se han conservado.

## Comprobación de compilación

- Entorno disponible: Oracle JDK 23 y Maven 3.9.11.
- `mvn -o compile`: no pudo resolver `maven-resources-plugin:3.3.1`, ausente de la caché local. El fallo ocurrió antes de compilar Java.
- Se solicitó ejecutar `mvn compile` con permiso para descargar los componentes faltantes. La solicitud fue rechazada y esa ejecución no llegó a iniciarse.
- Alternativa local ejecutada: `javac -encoding UTF-8 --release 23 -d target/auditoria-20261014-175523`, pasando todos los archivos de `src/main/java` obtenidos con `rg --files`. Resultado: código de salida **0**, sin errores del compilador.
- La compilación de Maven, el empaquetado y la ejecución del juego quedan sin verificar. No se ejecutó ninguna prueba.

El POM configura Java 23 y UTF-8. Su única dependencia declarada es JUnit Jupiter 5.14.1, con alcance `test`; el código principal no necesita bibliotecas externas para compilar con `javac`. No hay Maven Wrapper en el repositorio. No se cambió la configuración de Maven para ocultar los problemas de los archivos de pruebas.

## Inventario del código principal

| Área | Archivos inspeccionados | Estado por lectura |
| --- | --- | --- |
| Entrada | `app` | Construye dos armas y muestra su comparación; no inicia una aventura. |
| Modelo | `Combatiente`, `Jugador`, `Enemigo`, `Objeto` | Modelo en memoria, atributos, vida y mapas de equipo/habilidades. Hay fallos y operaciones pendientes detallados abajo. |
| Equipo | `Atributo`, `TipoEquipo`, `Equipable`, `Equipo`, `Arma` | Comparador escrito; aplicación y retirada de efectos incompletas. |
| Combate | `Combate`, `ContextoAtaque`, `Dado`, `TiradorDado`, `ResultadoCombate` | Iniciativa y cálculo parcial de ataque activos; flujo completo comentado. |
| Excepciones | `DadoInvalidoException` | Usada por los setters de dados de `ContextoAtaque`, que validan el rango 1–6. |
| Contratos de habilidades | `Habilidad`, `EstadoHabilidad`, `EventosHabilidad` | Igualdad por clase y estado aprendido/equipado definidos. Todos los eventos actuales reciben estado. |
| Habilidades de daño | `Caparazon`, `CastigoAlTorpe`, `DefensaPerfecta`, `GolpeCertero`, `GuardiaFerrea` | Efectos escritos, pero no conectados al ataque. |
| Otras habilidades | `DebilidadEnemiga`, `FuriaContenida`, `Impetu`, `InstintoSupervivencia`, `PrimerImpulso`, `Rabia`, `SedDeBatalla`, `SegundoAliento` | Efectos escritos; integración y migración de atributos/niveles pendientes. |
| Habilidad sin implementar | `FuriaMaximaBerserker` | Solo declara `nivelMinimo = 10`. |

## Problemas confirmados y referencias afectadas

Las conclusiones de esta sección proceden de la lectura del código y las búsquedas de referencias, no de ejecuciones de escenarios.

### 1. El combate completo no forma parte del código activo

En `src/main/java/org/viajedragos/combate/Combate.java`, el bloque entre las líneas 140 y 361 aproximadamente está comentado. Incluye `combatir`, `ordenHabilidades`, `dispararHabilidades`, `algunoMuerto`, `ambosVivos`, `resolverMuertes`, `personajeMuere` y `finalDelCombate`. Que el proyecto compile no significa que estos métodos compilen ni puedan llamarse.

Antes de activar ese bloque hay que resolver:

- `combatir` usa `determinarOponente(jugador)` para obtener `segundo`; si `primero` es el enemigo, ambos serían el mismo combatiente. La referencia pertinente es `determinarOponente(primero)`.
- La llamada de inicio usa una lambda de un parámetro, pero `dispararHabilidades` recibe un `BiConsumer<Habilidad, EstadoHabilidad>`. Además, `estado` no está definido en ese ámbito.
- Las cuatro llamadas a `alMorir` y la llamada a `alFinalCombate` omiten `EstadoHabilidad` y también usan lambdas de un parámetro. Reactivar el bloque tal cual produciría errores de compilación.
- Solo se disparan explícitamente las habilidades iniciales de `primero`.
- No existe bucle de turnos. El retorno final `AMBOS_MUEREN` es provisional.
- `finalDelCombate` solo procesa al jugador.
- Algunas ramas de `resolverMuertes` devuelven `AMBOS_MUEREN` sin procesar las habilidades del otro participante. Esto discrepa con la resolución descrita en el contexto; requiere confirmar la regla antes de cambiarla.

### 2. El ataque calcula daño, pero no completa su aplicación

`Combate.ataque` crea un `ContextoAtaque`, obtiene los dados y llama a `danioBruto`. No copia el daño bruto al final, no dispara los eventos de ataque y no aplica el daño normal al defensor. El contexto es local y el método devuelve `void`.

La excepción es `resolverContraataque`, que sí resta vida directamente al atacante. Esta ruta tampoco dispara eventos de daño ni resuelve muertes. Hay que integrar el flujo de ataque en una fase posterior, manteniendo o decidiendo expresamente sus reglas.

### 3. Las habilidades concretas no encajan en el mapa de habilidades

Las 13 habilidades con efectos implementan `EventosHabilidad` directamente; ninguna extiende `Habilidad`. `FuriaMaximaBerserker` tampoco implementa ese contrato.

El mapa de `Combatiente` es `Map<Habilidad, EstadoHabilidad>`. `aprenderHabilidad`, `agregarHabilidadEquipo`, `quitarHabilidadEquipo`, `agregarHabilidad` y `eliminarHabilidad` exigen `Habilidad`; `Equipo` también declara `Map<Habilidad, Integer>`.

Por tanto, una instancia de esas habilidades concretas no se puede pasar directamente a estas APIs. No hay llamadas activas que intenten hacerlo, por eso la compilación principal pasa. La igualdad por clase de `Habilidad` tampoco se aplica a ellas. La migración debe revisar herencia, constructores y referencias, conservando los constructores actuales cuando corresponda.

### 4. Uso incorrecto del valor efectivo para escribir el atributo base

Los getters de fuerza, resistencia, agilidad e iniciativa devuelven `max(0, base + modificadorEquipo)`, mientras que sus setters escriben el valor base. Hay llamadas `setX(getX() + cantidad)` o `setX(getX() - cantidad)` en `Impetu`, `InstintoSupervivencia`, `PrimerImpulso` y `Rabia`.

Esto incorpora el modificador de equipo al valor base. Por ejemplo, base 10 y equipo +5 producen un getter de 15; `setFuerza(getFuerza() + 3)` guarda base 18 y el valor efectivo pasa a 23, en vez de 18. Con penalizaciones y el límite de cero también se puede perder información al intentar revertir el efecto.

`FuriaContenida` utiliza `aumentarAtributo`/`disminuirAtributo`, evitando esa lectura del valor efectivo, pero sigue alterando el atributo base para un efecto temporal. Debe decidirse cómo representar los efectos temporales de habilidades antes de migrarlos; no se han añadido mapas ni capas nuevas.

### 5. El estado de nivel no se utiliza de forma uniforme

Todos los eventos de las habilidades implementadas tienen las firmas actuales de `EventosHabilidad`. Sin embargo, solo `Caparazon` lee `estado.getNivel()`. `CastigoAlTorpe`, `FuriaContenida`, `GolpeCertero`, `GuardiaFerrea`, `PrimerImpulso`, `SedDeBatalla` y `SegundoAliento` usan campos de nivel propios.

Al sumar niveles aprendidos o de equipo en `EstadoHabilidad`, esos efectos seguirían usando sus niveles internos. Debe confirmarse qué valores son niveles y qué valores son constantes de cada habilidad antes de sustituirlos.

`FuriaContenida`, `Impetu` e `InstintoSupervivencia` almacenan además estado mutable de activación o contadores en la instancia. Si se compartiese una misma instancia entre combatientes, compartirían ese estado. Es un riesgo condicionado a cómo se construyan los futuros catálogos; no se encontró un catálogo que ya comparta instancias.

### 6. Operaciones de Combatiente con fallos o precondiciones sin proteger

- `setHabilidades(setHabilidades)` asigna `this.habilidades = habilidades`: se asigna el campo a sí mismo y se ignora el parámetro. No se encontraron llamadas en el repositorio.
- `agregarHabilidad` utiliza `habilidades.compute(habilidad, (k, v) -> v)`: conserva el valor existente y no añade una entrada ausente. No se encontraron llamadas; falta definir con qué estado debería añadirse.
- `quitarHabilidadEquipo` desreferencia el estado sin comprobar su existencia. Si se llama sin la habilidad correspondiente, fallaría; tampoco protege contadores/niveles frente a retiradas excesivas. No hay una llamada activa desde el equipo.
- `getAtributo(VIDA)` y `getAtributoBase(VIDA)` desempaquetan una entrada ausente y provocarían `NullPointerException`. La inicialización excluye `VIDA`; no se encontraron llamadas actuales con esa constante. `modificarAtributo` y los métodos de cambios permanentes tampoco excluyen `VIDA`. Falta decidir el contrato para ese caso.
- `disminuirAtributo` limita la base a cero. Para una entrada ausente, `merge` insertaría el decremento positivo en lugar de restarlo; los cinco atributos ordinarios sí se inicializan. Hay que tenerlo presente al definir el contrato de `VIDA`.
- Los mapas de habilidades, equipo y modificadores se exponen directamente mediante getters. Cambiarlos desde fuera puede saltarse la contabilidad de efectos.
- `recibirDanio` acepta daño negativo y `curarse` acepta curación negativa. Los setters/constructores de vida no validan coherencia. Son entradas sin proteger; sus restricciones requieren decisión funcional.
- `curarse` sí dispara `alCurarse` pasando el estado de cada entrada. Es la única invocación activa de eventos de habilidades encontrada en el código principal.

### 7. Equipo incompleto

- `Equipo.aplicarEfectos`: la aplicación de atributos está comentada y usa el antiguo nombre inexistente `modificarAtributoEquipo`. El método actual es `Combatiente.modificarAtributo`. Solo se añaden habilidades.
- `Equipo.retirarEfectos`: cuerpo vacío.
- `Combatiente.setEquipo`: registra la pieza si la ranura está libre, pero no aplica efectos; rechaza una ranura ocupada y no sustituye la pieza. No hay métodos `equipar` ni `desequipar`.
- `equipoEquipado` **sí está inicializado**, a diferencia de la duda que figura en el contexto.
- `Equipo` conserva los mapas recibidos en el constructor y los expone con getters. Si cambian tras aplicar efectos, se perdería la referencia a las contribuciones originales necesarias para retirarlos.
- `compararCon` recorre la unión de atributos y calcula nuevo menos actual, sin equipar ni modificar combatientes. Usa `HashSet`, así que el orden de filas no está garantizado. Muestra `--` tanto para ausencia como para cero explícito; coincide con la convención del contexto mientras no se introduzcan ceros explícitos.

### 8. Iniciativa y selección de oponente

- `primerTurno` repite empates mediante recursión. Una cadena larga puede agotar la pila. Si ambos tienen iniciativa efectiva cero, siempre empatan y no puede terminar con la regla actual; hace falta decidir ese caso.
- `atacante` aplica **+2 a iniciativa**, no a agilidad. Es una discrepancia entre el código y el documento de contexto; no se ha cambiado ninguna de las dos reglas.
- `determinarOponente` devuelve el jugador para cualquier argumento que no sea el jugador, incluso si no es el enemigo. Su uso correcto presupone recibir uno de los dos participantes.

## Referencias antiguas en los archivos de pruebas

No se han compilado ni ejecutado. La inspección identifica incompatibilidades concretas que el autor podrá corregir:

- `CombateTest` llama dos veces a `determinarDefensor`, que no existe; el código actual expone `determinarOponente`.
- Diez archivos de habilidades llaman a eventos sin el nuevo argumento `EstadoHabilidad`: `CastigoAlTorpeTest`, `DebilidadEnemigaTest`, `DefensaPerfectaTest`, `GolpeCerteroTest`, `GuardiaFerreaTest`, `ImpetuTest`, `PrimerImpulsoTest`, `RabiaTest`, `SedBatallaTest` y `SegundoAlientoTest`.

Por ello, la compilación principal con `javac` no permite afirmar que el proyecto completo, incluyendo `src/test`, compile. No se añadieron sobrecargas para encubrir las llamadas antiguas.

## Lista real de lo que falta

1. Integrar las habilidades concretas con `Habilidad` y decidir el uso de nivel y estado por combatiente.
2. Corregir la escritura de atributos base desde valores efectivos y definir la representación de efectos temporales.
3. Corregir `setHabilidades`; definir y completar `agregarHabilidad` y las precondiciones de retirada de habilidades.
4. Completar el ataque: daño final, eventos, aplicación de daño y resolución de muertes.
5. Corregir las llamadas del bloque comentado antes de activar el combate, implementar el bucle de turnos y retirar el resultado provisional.
6. Confirmar la resolución de muertes y los destinatarios de eventos iniciales/finales.
7. Completar aplicación/retirada de efectos, equipar/desequipar y sustitución por ranura.
8. Decidir los empates con iniciativa cero y confirmar el atributo que recibe el +2 del turno.
9. Implementar o definir `FuriaMaximaBerserker`.
10. Crear catálogos Java en memoria y un flujo jugable de consola. Actualmente solo hay una demostración de comparación de armas.
11. Definir e implementar encuentros, huida, objetos consumibles, inventario, comercio y transición/recompensas cuando el autor concrete sus reglas. Los resultados de huida ya están en el enum, pero no hay mecánica.
12. Añadir instrucciones de ejecución al README y completar la comprobación con Maven cuando estén disponibles sus componentes.
13. Adaptar las referencias antiguas de las pruebas, tarea reservada al autor.

No se encontró acceso a datos ni persistencia en el código principal. No se han iniciado las fases posteriores ni actualizado el documento de Downloads: esta auditoría registra el estado comprobado dentro del repositorio.

## Revalidación tras autorizar la comprobación de las pruebas

Se ha vuelto a leer el contexto y a inspeccionar los 33 archivos principales, los 14 de pruebas y sus referencias. El código fuente sigue siendo el del commit `9a9f274`; no se han modificado Java, POM ni pruebas.

- `mvn test` compiló las 33 fuentes principales y se detuvo en `testCompile`. Las llamadas incompatibles identificadas en la primera auditoría son ahora errores confirmados por el compilador: falta `determinarDefensor` y falta el argumento `EstadoHabilidad` en diez archivos de habilidades. Ninguna prueba llegó a ejecutarse.
- `mvn compile`, ejecutado con acceso fuera del aislamiento tras un fallo de resolución dentro de él, terminó con **BUILD SUCCESS**. La comprobación de compilación con Maven ya no queda pendiente.
- `java -cp target/classes org.viajedragos.app` terminó con código 0. La comparación imprimió RESISTENCIA 3 → -4 (diferencia -7), AGILIDAD ausente → 3 (+3), FUERZA 5 → 8 (+3) e INICIATIVA 2 → ausente (-2). No existe una partida que ejecutar desde ese punto de entrada.
- Se actualizó `CONTEXTO_CODEX_VIAJE_A_DRAGOS.md` en el repositorio y, con permiso de escritura fuera del espacio de trabajo, también el original en Downloads. La checklist distingue revisión completada, implementación incompleta y comprobaciones bloqueadas.

Orden recomendado para futuras correcciones, sujeto a autorización del autor y sin realizarlo en esta auditoría:

1. Recuperar la compatibilidad de las pruebas con las firmas actuales: actualizar llamadas a oponente y proporcionar el estado de cada habilidad, sin añadir sobrecargas que oculten la migración. Afecta a `CombateTest` y los diez tests de habilidades enumerados arriba. Conservar las reglas esperadas y revisar cualquier discrepancia que aparezca al ejecutar.
2. Corregir los fallos aislados de `Combatiente`: `setHabilidades`; decidir el estado inicial de `agregarHabilidad` y las precondiciones de retirada/VIDA antes de tocar esos comportamientos.
3. Resolver la compatibilidad del modelo de habilidades: herencia de `Habilidad`, constructores existentes, uso de `EstadoHabilidad.nivel` y estado mutable por combatiente. Impacto en las implementaciones, mapas de `Combatiente` y `Equipo` y sus llamadas/pruebas.
4. Corregir la mezcla de atributos base y efectivos en las habilidades afectadas, tras decidir cómo representar sus efectos temporales. No cambiar de forma global el significado de getters/setters, pues afectaría a constructores, combate y otras habilidades.
5. Completar la contabilidad de efectos de `Equipo` y las operaciones de equipar/retirar cuando se autorice ese alcance. Revisar alias de mapas y referencias a `modificarAtributo`, `agregarHabilidadEquipo` y `quitarHabilidadEquipo`.
6. Corregir firmas y participantes del borrador de `Combate`; después integrar daño, eventos, muerte, turnos y final en peticiones específicas. Activarlo tal cual no compilaría. Confirmar antes el +2 de turno, los empates con iniciativa cero y los destinatarios de los eventos finales.
7. Solo después, y con autorización separada, conectar un flujo jugable de consola y catálogos en memoria. Las reglas de huida, objetos y comercio siguen pendientes de decisión.

La alternativa menos invasiva ahora es conservar el código y usar esta lista como base para autorizar una corrección concreta cada vez. Ninguna corrección se ha aplicado, ni se ha realizado un nuevo commit o push.

## Primer bloque de correcciones autorizado

Cambios realizados después de la respuesta «procede» del autor:

- `Combatiente.setHabilidades` asigna el mapa recibido.
- Las 13 habilidades con efectos extienden `Habilidad` y llaman a su constructor con un nombre. Se conservan todos los constructores públicos preexistentes, campos de nivel y fórmulas. Ahora son compatibles con los mapas y APIs que exigen Habilidad; FuriaMaximaBerserker sigue sin implementar.
- `CombateTest` llama a `determinarOponente`; los diez tests de habilidades añaden el argumento `EstadoHabilidad`, con el nivel correspondiente al escenario. No se han cambiado aserciones ni añadido tests o sobrecargas.
- Las seis llamadas a `dispararHabilidades` del borrador de Combate reciben ambos parámetros y pasan el estado al evento. `segundo` se obtiene a partir de `primero`. El bloque permanece comentado, por lo que estas correcciones no se han validado ejecutando ese flujo.

Comprobación ejecutada: `mvn test`, **BUILD SUCCESS**, 51 pruebas, 0 fallos, 0 errores, 0 omitidas. Los informes están en `target/surefire-reports`. La suite compila el código principal y las pruebas. No valida una partida completa ni cubre específicamente el setter corregido o toda la contabilidad de equipo.

Quedan pendientes las demás correcciones de la auditoría. Antes de cambiar los efectos temporales o el combate se han solicitado decisiones sobre el +2 del turno, los destinatarios de eventos de inicio/final y la representación de modificadores temporales. No se han elegido reglas por defecto. También sigue pendiente decidir el estado inicial de `agregarHabilidad` y la relación entre los niveles de los constructores y EstadoHabilidad.

No se han añadido funcionalidades de equipo, catálogos, menús o combates, ni dependencias o patrones. No se ha realizado commit o push. La checklist se actualiza solo para los cambios y comprobaciones descritos.

## Tarea de atributos y equipamiento completada

Se ha trabajado sobre los cambios locales del primer bloque, sin recuperar ni sobrescribir con GitHub. Esta tarea no modifica `Combate`, su borrador ni la secuencia de sus eventos.

### Impacto y archivos modificados en esta tarea

| Archivo | Métodos afectados y resultado |
| --- | --- |
| `modelo/Combatiente.java` | `getAtributo`, `getAtributoBase`, `aumentarAtributo`, `disminuirAtributo`, `modificarAtributo`: validar atributos y rechazar VIDA sin tocar los campos de vida. Getters de modificadores/ranuras: solo lectura. `setEquipo`: delegar en la nueva operación `equipar(Equipo)`; nueva `desequipar(TipoEquipo)`. `agregarHabilidadEquipo` y `quitarHabilidadEquipo`: proteger contribuciones inválidas. |
| `arma/Equipo.java` | Constructor: copiar los mapas, validar sus entradas y hacerlos de solo lectura. `aplicarEfectos`: sumar atributos y habilidades. `retirarEfectos`: validar todas las habilidades primero y descontar solo sus contribuciones. El comparador conserva su implementación. |
| `habilidad/implementacion/Impetu.java` | `alHacerDanio` y `alFinalCombate` usan agilidad base, evitando duplicar o perder modificadores de equipo. |
| `habilidad/implementacion/InstintoSupervivencia.java` | `modificarFuerza` y `alFinalCombate` usan fuerza base. Sus eventos y umbral no cambian. |
| `habilidad/implementacion/PrimerImpulso.java` | Inicio/final suman y restan su nivel sobre iniciativa base. |
| `habilidad/implementacion/FuriaContenida.java` | Final resta exactamente su contribución de fuerza desde la base sin truncarla. |
| `habilidad/implementacion/Rabia.java` | Inicio usa base; nuevo efecto de final revierte fuerza +2, agilidad +1 y resistencia -1. Un indicador evita retirar efectos que no se activaron o retirarlos dos veces. |
| `src/test/java/org/viajedragos/modelo/EquipamientoTest.java` | Archivo nuevo: 34 escenarios de equipo/atributos/habilidades, incluidos casos parametrizados. |
| `src/test/java/org/viajedragos/habilidad/implementacion/HabilidadesConEquipoTest.java` | Archivo nuevo: 8 escenarios de efectos de habilidades con equipo positivo/negativo y cambio de pieza mientras el efecto está activo. |

Las búsquedas previas no encontraron llamadas existentes a `setEquipo` ni escritura en los getters de equipo/modificadores. Se conservan las firmas públicas existentes. `setEquipo` ahora aplica los efectos de la pieza aceptada y sigue mostrando un mensaje si la ranura está ocupada. Las nuevas operaciones booleanas permiten comprobar si se equipó/desequipó; no hay sustitución automática. Los getters de mapas mantienen su tipo, pero ya no permiten escritura, cambio deliberado para proteger las contribuciones. No se modifican `Habilidad`, `EstadoHabilidad`, `Arma`, `Equipable`, los enums ni el POM en esta tarea.

### Decisiones expresas y límites

- El autor ha pedido conservar el sistema que sube y baja los atributos desde las habilidades, sin un nuevo mapa temporal. Los eventos leen/escriben la base sin incluir equipo y revierten las diferencias con signo; provisionalmente, la base incluye el efecto mientras está activo. Los aumentos permanentes que se hagan durante ese efecto no se pierden al retirarlo.
- El autor ha confirmado que Rabia debe restaurarse al final. Se añade el evento, pero conectarlo al combate seguirá siendo tarea de `Combate`.
- VIDA sigue separada. No se inventa la relación entre vida actual/máxima y bonificaciones: los equipos con VIDA y los métodos genéricos con VIDA producen un error explícito. Si se quieren esos equipos, falta definir su regla.
- `aplicarEfectos` y `retirarEfectos` son operaciones de efectos que deben emparejarse. Para equipar objetos se usan las operaciones de Combatiente; no se llama manualmente a aplicar además de equipar la misma pieza. Las repeticiones de equipar/desequipar se rechazan sin duplicar efectos.
- Se conserva el funcionamiento existente de niveles internos de habilidades y el estado mutable de sus instancias; esta tarea verifica nivel/contador de EstadoHabilidad y contribuciones del equipo. Unificar cómo los eventos consumen ese nivel y evitar compartir instancias de habilidades con estado entre combatientes sigue siendo trabajo de integración de habilidades.
- `agregarHabilidad` y sus reglas de estado inicial permanecen pendientes; equipamiento usa los métodos existentes de aprender/agregar/quitar habilidades de equipo, que están probados.

### Comprobación final

`mvn clean test`: **BUILD SUCCESS**. Recompilación de 33 fuentes principales y 16 archivos de pruebas. **93 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**: 51 anteriores y 42 nuevas.

Se probaron todos los atributos ordinarios y todas las ranuras; rechazo de ranura ocupada; sustitución tras retirar la pieza; aplicación/retirada directas emparejadas; penalizaciones que superan la base; contribuciones de otras piezas; cambios permanentes durante el equipamiento; setters; habilidades comunes a varias piezas y a aprendizaje; aprendizaje mientras se equipa; operaciones repetidas; niveles inválidos; mapas externos mutados; retirada inválida sin descontar efectos parciales; vida conservada; comparador con ausencias y diferencias positivas/negativas; recuperación de atributos en las cinco habilidades corregidas.

La base de atributos/equipamiento permite pasar a completar Combate. Antes de integrarlo siguen pendientes sus reglas: +2 del turno, empate con iniciativa cero, destinatarios de eventos finales y resolución de muertes. No se ha ejecutado una partida ni implementado el bucle de combate. No se han añadido dependencias, persistencia, frameworks o patrones. No se ha hecho commit ni push.

## Comparación con fc7754c y ataque individual: integración parcial

Antes de modificar nada, HEAD local y `git ls-remote origin refs/heads/main` devolvieron `fc7754c438080b835358f9a64c871b2fbb7595db`; el árbol local estaba limpio y `git diff fc7754c` no mostró diferencias. Se ejecutó `mvn clean test`: 93 pruebas pasaron.

El ataque individual no estaba integrado en ese commit. `ataque` solo calculaba bruto, las habilidades no se invocaban y solo el contraataque restaba vida directamente. Las 42 pruebas incorporadas al commit eran de equipo/atributos, no de la integración del ataque.

Impacto confirmado antes del cambio: `Combate.ataque` mantiene su firma pública void y no tiene llamadas existentes en el código del juego. `danioBruto`, `resolverContraataque`, `ContextoAtaque`, los contratos de eventos y las implementaciones se inspeccionaron. Los cálculos existentes son la referencia para esta integración; no se han cambiado las reglas ni activado el borrador de `combatir`.

Cambios locales de esta tarea:

- `Combate.ataque`: copia bruto a final y aplica una sola vez ese daño al defensor si el ataque no es fallido. Los contraataques mantienen su ruta directa existente, sin volver a aplicar daño al defensor.
- Nuevo `src/test/java/org/viajedragos/combate/AtaqueTest.java`: 46 escenarios. Matriz de las 36 combinaciones 1D6/1D6, los seis valores del dado extra de crítico, daño mínimo cero sin curación, daño letal limitado a cero vida, enemigo como atacante y validación de dado. También se comprueba que solo el crítico 6/1 solicita la tercera tirada.
- Documentación actualizada para distinguir daño probado de eventos todavía pendientes.

Reglas existentes conservadas: ataque 1/defensa menor que 6 falla; 1/6 provoca contraataque de fuerza del defensor sin tirada extra; ataque 2–5/defensa 1 aplica todo el valor de ataque; 6/1 añade 1D6 crítico sin restar defensa; resto aplica ataque menos defensa con mínimo cero.

Resultado final de este bloque: `mvn clean test`, BUILD SUCCESS, 33 fuentes principales y 17 archivos de pruebas recompilados, **139 pruebas, 0 fallos, 0 errores, 0 omitidas**. No se han conectado los eventos de habilidades, así que esas pruebas no acreditan una integración completa ni el paso de EstadoHabilidad durante el ataque.

Se han solicitado tres decisiones antes de continuar esa parte: secuencia y destinatarios de eventos; orden entre modificadores del atacante y defensor y dentro de cada mapa; si el contraataque directo debe conservarse o pasar por eventos con identidades invertidas. Hasta recibirlas no se elige una regla por defecto. No se cambian niveles internos de habilidades, iniciativa, orden de turnos ni resolución de muertes. `combatir` sigue comentado. No hay commit ni push de este bloque.

## Ataque individual con eventos integrado tras confirmar las reglas

El autor confirmó las fases, atacante antes de defensor en eventos previos, paso del estado de cada entrada y contraataque directo sin nuevos contextos ni una segunda secuencia. No se descartaron los cambios locales ni las 139 pruebas anteriores.

### Revisión previa de habilidades

Se inspeccionaron todos los overrides de eventos del ataque: Caparazon, CastigoAlTorpe, DefensaPerfecta, GolpeCertero y GuardiaFerrea modifican final antes de aplicarlo; Impetu y SedDeBatalla actúan al hacer daño; FuriaContenida e InstintoSupervivencia al recibirlo. No hay implementaciones existentes de antesDeAtacar/antesDeCalcularDanio ni modificaciones existentes de bruto, dados o indicadores desde esas habilidades.

La secuencia confirmada permite ejecutar esos efectos sin cambiar sus fórmulas ni sustituir niveles internos. No se detectó un requisito de orden distinto que impidiese integrarlas. Se conservó una particularidad existente y se informó antes de editar: los modificadores de daño no comprueban el propietario. Por ejemplo, GuardiaFerrea en el atacante también reduciría el final, y GolpeCertero en el defensor podría aumentarlo. No se inventaron filtros de rol ni se modificaron habilidades; cualquier cambio de esa semántica requiere decisión del autor.

### Impacto y cambios de esta tarea

- `src/main/java/org/viajedragos/combate/Combate.java`: `ataque` conecta las fases confirmadas. `dispararHabilidades`, antes situado dentro del bloque comentado, pasa a código activo con su misma firma y recorrido de entradas. Se eliminan dos imports no usados. Se conservan la firma pública void, ContextoAtaque, contratos de eventos, implementaciones, mapas, niveles, fórmulas, `danioBruto` y `resolverContraataque`.
- Nuevo `src/test/java/org/viajedragos/combate/EventosAtaqueTest.java`: 19 escenarios de integración. La habilidad de registro existe únicamente en pruebas; no se añade un patrón ni abstracción productiva.
- Checklist y este informe actualizados. `AtaqueTest` conserva sus 46 escenarios anteriores sin modificación en esta tarea.

Los eventos previos ocurren para el ataque original incluso si su cálculo determina fallo/contraataque. Tras ellos, si el ataque está fallido, se deja final en cero y no se aplica daño normal ni eventos posteriores. En un ataque no fallido se aplica final una vez y se comprueba la pérdida real de vida antes de ejecutar alHacerDanio y alRecibirDanio. Un daño nominal positivo sobre vida ya cero no dispara esos eventos; el daño letal con pérdida real sí los dispara una vez, sin resolver muertes.

El contraataque permanece en su ruta original: fuerza del defensor directamente contra el atacante durante el cálculo, sin contexto adicional, inversión, tiradas nuevas ni efectos posteriores por ese daño. Los modificadores de final del ataque original no alteran ese daño directo.

Atacante y defensor se procesan por separado y por fases; cada callback recibe exactamente el estado de su propia entrada. El orden interno entre habilidades de un mismo mapa sigue siendo el existente, no se convierte HashMap a otra colección ni se establece un criterio nuevo. La secuencia usa un único ContextoAtaque y conserva sus límites de daño mínimo cero.

### Resultado y límites

`mvn clean test`: **BUILD SUCCESS**. Recompilados 33 archivos principales y 18 de pruebas. **158 pruebas, 0 fallos, 0 errores, 0 omitidas**: las 139 anteriores y 19 nuevas.

Las pruebas verifican orden de fases/dados, orden atacante-defensor, contexto único, estados diferentes en varios participantes/habilidades, atributos previos al cálculo, modificación de bruto/final, GolpeCertero/DefensaPerfecta, GuardiaFerrea, Caparazon, CastigoAlTorpe, crítico con modificadores, fallos, final cero, daño creado desde bruto cero, curación y cambios de atributos posteriores, letal, defensor sin vida y contraataque sin duplicación o efectos indirectos.

Esta tarea termina la integración del ataque individual. No activa `combatir`, no decide orden de turnos o iniciativa, no ejecuta eventos de inicio/final del combate ni alMorir y no resuelve muertes. Sigue pendiente definir el +2 del turno, empate con iniciativa cero, destinatarios iniciales/finales y reglas de resolución de muertes antes de activar el flujo completo. Tampoco se modifica la semántica de niveles internos ni el estado mutable compartido de instancias de habilidades. No se añadieron dependencias, persistencia, frameworks o patrones, ni se hizo commit o push.

## Preparación de combatir: reglas aclaradas e invariantes de habilidades

Se inspeccionó `79d15c7` con árbol local limpio y se comprobó `mvn clean test`: 158 pruebas. Se revisaron combatir, primerTurno, atacante, ordenHabilidades, resolverMuertes, personajeMuere y finalDelCombate, sus llamadas y overrides. El autor aclaró participación de ambos en eventos, prioridad por iniciativa actual/poseedor/tirada, transición de muerte al inicio y exigencia de niveles positivos. El +2 del poseedor se confirma exclusivamente para el cálculo del atacante.

La etapa A sigue detenida en un punto concreto: iniciativa × 1D6 da siempre empate si ambas iniciativas son cero. Se ha preguntado si se confirma la comparación directa de dados como excepción. No se aplica una respuesta por defecto y el flujo comentado sigue sin activarse.

Se avanzó únicamente en el bloque independiente de habilidades:

- `EstadoHabilidad`: constructor con parámetros y setters rechazan nivel <= 0 y contador de equipo negativo. Constructor vacío conserva su firma y crea nivel 1, inactivo. `isActiva()` refleja aprendizaje o equipo.
- `Combatiente.aprenderHabilidad`, `agregarHabilidadEquipo` y `quitarHabilidadEquipo`: aportaciones positivas; retirada final elimina la entrada sin asignar nivel cero; se rechaza dejar una habilidad que permanezca activa sin nivel positivo. `curarse` solo dispara habilidades activas.
- `Equipo`: otorgar niveles exige valores positivos y la retirada valida que las habilidades restantes conserven nivel positivo antes de descontar efectos.
- `Combate.dispararHabilidades`: filtra entradas inactivas, conservando estado, fases, fórmulas y firmas. No se modifica ataque ni se activa el bucle.
- Nuevo `EstadoHabilidadTest`: 14 escenarios de construcción/setters, actividad, aportaciones inválidas, retirada final, preservación de habilidad aprendida y filtrado en ataque/curación.

Resultado: `mvn clean test`, BUILD SUCCESS; 33 fuentes principales y 19 archivos de pruebas recompilados; **172 pruebas, 0 fallos, 0 errores, 0 omitidas**. Las 158 pruebas previas se conservan y pasan. Los niveles internos de las implementaciones no se cambian.

Pendientes de integración: inicio y final para ambos en prioridad actual, seguimiento de los efectos iniciales realmente aplicados (PrimerImpulso no debe restarse si no se ejecutó), alternancia, eventos de turno, muerte sin duplicados y restauración. No se añade huida ahora ni un límite/resultado arbitrario para combates sin progreso; esa decisión queda fuera de las reglas confirmadas. No se han hecho commit ni push.

## Etapa A de combatir y auxiliares activados

El autor confirmó la comparación directa de dados cuando ambas iniciativas efectivas son cero. Se avanzó de forma incremental sin activar todavía el bucle de la etapa B: está pendiente confirmar si una muerte definitiva tras un ataque omite alFinalTurno o lo ejecuta antes de cerrar.

Cambios:

- `Combate.primerTurno`: repetición iterativa; mantiene iniciativa × 1D6, con comparación directa de dados solo cuando ambas iniciativas son cero.
- `Combate.combatir`: método activo. Ordena inicio por iniciativa actual, ejecuta el inicio del primer participante vivo, resuelve las muertes y ejecuta el del segundo si no murió antes de su momento. Si una habilidad inicial produce un resultado definitivo, procesa final para ambos; de momento devuelve CONTINUA si ambos sobreviven, sin simular una victoria ni ejecutar ataques.
- `ordenHabilidades`: mayor iniciativa efectiva; empate usa poseedor de turno si lo hay, o tiradas directas si no. El poseedor se prepara para la etapa B; por ahora se reinicia a null al comienzo.
- `resolverMuertes` y `personajeMuere`: se conservan sus firmas privadas y se simplifican las ramas asimétricas. El primer muerto ejecuta sus efectos; si después el otro está muerto, ejecuta los suyos una vez. Se devuelve CONTINUA solo si ambos sobreviven, o el resultado de muerte correspondiente. Como máximo se invoca alMorir una vez por participante en esa resolución.
- `finalDelCombate`: prioridad actual y eventos finales de ambos, incluido el muerto, para restaurar efectos.
- `PrimerImpulso`: indicador de aplicación para no restar su aumento si nunca ejecutó el inicio, ni hacerlo dos veces. No cambia magnitud, fórmula ni nivel interno.
- `InicioCombateTest`: 8 escenarios de inicio, finales iniciales, prioridad actual, muerte antes del inicio, protección de PrimerImpulso, muerte simultánea, cadena de resurrección/muerte sin duplicados, iniciativas cero y 10.000 empates iterativos.

La suite pasa con **180 pruebas**, 0 fallos y 0 errores. Las 158 pruebas del ataque/equipo y los 14 invariantes de habilidades se conservan. Aún no se han probado victoria/muerte mediante turnos completos, alternancia ni resurrección con reanudación de ataques; esos escenarios pertenecen a la etapa B pendiente.

No se añade un límite de turnos, interfaz, huida, objetos o menús. La ausencia de recursión evita agotar la pila por empates; no garantiza que dados que siempre empaten, o un combate matemáticamente sin progreso, puedan terminar. El autor mantiene esas reglas y deja huida/rediseño para su fase correspondiente. No se ha hecho commit ni push.

## Combate automático completo: etapas A/B/C finalizadas

El autor aclaró que una muerte definitiva cierra inmediatamente el enfrentamiento: el muerto no ejecuta más eventos ordinarios y no hay final de turno posterior. Confirmó expresamente una excepción: **el jugador siempre ejecuta alFinalCombate**, incluso muerto, para restaurar atributos y contingencias. El enemigo no ejecuta finales de combate. Se corrigieron las expectativas de cuatro pruebas de inicio que antes suponían finales de ambos; las 158 pruebas previas a esta tarea se conservan sin cambiar.

### Archivos productivos modificados desde 79d15c7

- `Combate.java`: método público combatir activo y completo, campo de poseedor del turno, primerTurno iterativo con excepción confirmada de cero, prioridad actual/fallback, auxiliar privado eventosTurno, resolución de muertes sin duplicados y final solo del jugador. La firma pública, atacante, ataque y sus fórmulas/reglas especiales se conservan. No se añaden valores a ResultadoCombate ni clases productivas.
- `PrimerImpulso.java`: indicador de aplicación para que la limpieza no reste un efecto que no comenzó ni lo retire dos veces. La magnitud y el nivel interno no cambian.
- `EstadoHabilidad.java`, `Combatiente.java` y `Equipo.java`: validaciones de nivel positivo y actividad de habilidades, documentadas en el bloque anterior. Se mantienen las firmas existentes y no se cambian fórmulas de habilidades.

### Flujo implementado

1. Inicio por prioridad de iniciativa actual, desempate por dados cuando no hay poseedor. Muertes comprobadas entre los participantes. Quien muere antes de su inicio pasa a muerte, no a ese inicio.
2. Primer poseedor mediante iniciativa × 1D6; ambas iniciativas cero comparan dados directamente; repetición iterativa de empates.
3. Inicio de turno de ambos vivos por prioridad, con resolución de muertes entre participantes.
4. Selección con atacante existente y ataque existente. Resolver muertes antes de decidir el final. Una resurrección que deje ambos vivos permite continuar.
5. Si continúa, final de turno de ambos vivos por prioridad actual, comprobando muertes; después alternar poseedor, aunque el atacante efectivo fuese el oponente.
6. Al morir definitivamente alguien, cierre sin eventos ordinarios posteriores y limpieza alFinalCombate del jugador exclusivamente. Resultado coherente con la resolución, conservando todos los valores existentes del enum.

Cada resolución de muerte puede ejecutar hasta dos participantes, una vez cada uno. No se reejecuta el primero si el segundo lo mata otra vez. Una muerte posterior a una resurrección, en otro momento del combate, tiene una resolución nueva. Todos los eventos reciben el EstadoHabilidad de su entrada activa.

### Pruebas y resultados

- `EstadoHabilidadTest.java`: 14 escenarios de invariantes, aportaciones, retirada y actividad.
- `InicioCombateTest.java`: 9 escenarios de inicio y muertes iniciales, prioridad, ambos, protección de PrimerImpulso no iniciado y desempates iterativos.
- `CombateCompletoTest.java`: 21 escenarios con secuencias exactas de dados y protección de tiempo en pruebas: victoria, derrota, alternancia, atacante distinto del poseedor, muerte simultánea, resurrección del jugador/enemigo, cadena sin duplicados, prioridad empatada y cambiante, muertes en inicio/final del turno, SegundoAliento de ambos, restauración de Rabia/PrimerImpulso/Impetu/InstintoSupervivencia/FuriaContenida, equipo conservado, limpieza del jugador muerto, contraataque letal, estados inactivos, iniciativas cero y reutilización del jugador en otro combate.

Comprobación final ejecutada: **mvn clean test, BUILD SUCCESS, 202 pruebas, 0 fallos, 0 errores y 0 omitidas**. Se recompilaron 33 fuentes principales y 21 archivos de pruebas. Son las 158 anteriores y 44 nuevas. Los dados de pruebas fallan ante cualquier tirada adicional inesperada, por lo que detectan continuaciones indebidas y resultados no reproducibles.

La operación combatir permite ejecutar un enfrentamiento automático completo desde Java y está probada con habilidades reales y controladas. No se crea una interfaz ni se modifica app. No se introducen patrones, frameworks, dependencias o persistencia. No se han hecho commit ni push; HEAD permanece 79d15c7.

Pendientes fuera de este alcance: decisión interactiva atacar/huir/objeto, huida, inventario, encuentros, comercio y catálogos. Las reglas no garantizan terminar un combate matemáticamente sin progreso o un dado que nunca desempate; no se inventó un límite o resultado para esos casos, tal como decidió el autor. Los casos límite reproducibles de iniciativa cero, cadenas de muerte y muchos empates sí terminan en las pruebas. La semántica de niveles internos y el uso de instancias mutables compartidas entre combatientes siguen sin reformularse.

## Fase 3: combate interactivo por consola (15 de octubre de 2026)

Base de esta fase: `e393563` de main. El estado anterior de 202 pruebas se comprobó antes del cambio. Durante el análisis se detectó una discrepancia: el motor resolvía un solo ataque por turno, mientras que el autor quería una acción de cada participante en el orden de iniciativa. Tras explicar el impacto, el autor confirmó expresamente el turno de dos acciones y la adaptación del flujo automático y de las pruebas afectadas.

### Cambios e impacto

- `Combate.java`: conserva combatir() sin argumentos y añade combatir(Scanner, PrintStream); ambos delegan al mismo bucle. Se conservan primerTurno, la fórmula de atacante (iniciativa + dado, +2 del poseedor y desempate), alternancia del poseedor, eventos de inicio/final de turno, ataque individual y resolución/finalización. El resultado de atacante determina ahora quién actúa primero; el oponente actúa después si el combate continúa. Se resuelven muertes después de cada acción, antes de pedir otra decisión. Una resurrección que deje ambos vivos permite continuar.
- La sobrecarga interactiva muestra nombres, vida actual/máxima y orden. Pregunta Atacar únicamente al llegar la acción del jugador; el enemigo ataca automáticamente. Los errores de entrada repiten la pregunta sin volver a ejecutar eventos ni tirar dados. La narración opcional del ataque informa de fallos, críticos, contraataques y pérdida real de vida, limitada a la vida disponible. El contraataque especial sigue dentro del ataque original y no consume la acción ordinaria pendiente del defensor.
- `app.java`: sustituye la demostración de comparación de armas por presentación, inicio de combate con Aventurero/Lobo definidos en Java, resultado y nuevo combate/salida. Cada combate construye personajes nuevos. main usa System.in/System.out y Dado; ejecutar admite flujos y dados para comprobar el flujo real sin ficheros. No se usa System.exit ni se cierran los flujos del llamador.
- Cerrar la entrada en una decisión interrumpe la sesión sin simular una huida ni devolver un resultado ficticio. No se considera un combate terminado y no ejecuta su limpieza final. El personaje de esta demo se descarta al cerrar la sesión. Las finalizaciones ordinarias conservan siempre alFinalCombate del jugador, incluso muerto.
- Jugador, Enemigo, Dado, ResultadoCombate y las implementaciones de habilidades no necesitan cambios. No se alteran fórmulas ni niveles internos. El comparador de armas permanece disponible en su clase; app deja de ejecutarlo al arrancar.
- `README.md`: instrucciones de IntelliJ/JDK 23, terminal y Maven. Checklist de contexto actualizada.

### Pruebas ejecutadas

- Se conservan los 202 escenarios anteriores. Se adaptan seis de CombateCompletoTest: dos acciones/alternancia, resurrección del enemigo, resurrección del jugador, prioridad cambiante, muerte en final de turno y restauración de efectos/equipo. Cambian los dados y las expectativas temporales; se conservan las comprobaciones de reglas, estado final, niveles, restauración y consumo exacto de dados.
- `AppTest`: 10 casos de inicio/ataque/victoria, entrada inválida, repetición con vida restaurada por personajes nuevos, derrota sin decisión del muerto, salida, cierre de entrada en los menús y durante el combate, mensaje de muerte simultánea y entrada/salida de main.
- `CombateConsolaTest`: 12 casos de orden de ambas acciones, enemigo primero y decisión posterior, muerte previa al menú, orden nuevo cada turno, entrada inválida sin repetir eventos, fallo, contraataque letal y no letal, daño cero, resurrección antes de decidir, muerte simultánea y equivalencia de ambos modos.
- La clase vacía src/test/java/org/viajedragos/app.java se elimina: tenía el mismo nombre completo que la aplicación y la ocultaba durante las pruebas. No contenía pruebas.

Comprobación final: **mvn clean test, BUILD SUCCESS; 224 pruebas, 0 fallos, 0 errores, 0 omitidas**, recompilando 33 fuentes principales y 22 archivos de pruebas. La compilación limpia detectó un BOM UTF-8 introducido al escribir app.java desde PowerShell; se corrigió a UTF-8 sin BOM antes de esta ejecución satisfactoria.

También se ejecutó `java -cp target/classes org.viajedragos.app` en una terminal interactiva con Dado real: iniciar con 1, elegir Atacar durante cinco turnos, victoria con Aventurero a 18/24 y Lobo a 0/18, y salir con 0. El proceso terminó con código 0. Se observaron golpes, daño cero, fallos, actualización de vidas y menú posterior al resultado. `git diff --check` terminó sin errores de formato.

No se han añadido dependencias, patrones, capas, persistencia, inventario, comercio, exploración, huida ni objetos. No hay commit ni push de esta fase. Siguen pendientes las reglas de esas funcionalidades futuras y el equilibrio de combates sin progreso; no se introdujo un límite artificial de turnos. La ejecución desde la interfaz de IntelliJ queda para comprobación del autor; el flujo se ha ejecutado tanto desde terminal como desde las pruebas con entrada/salida controladas.
