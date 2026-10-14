# Auditoría de la fase 1 — Viaje a Dragos

Fecha: 14 de octubre de 2026.

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
