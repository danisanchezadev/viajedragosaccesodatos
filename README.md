# Viaje a Dragos

Demo Java de una aventura de combates consecutivos por consola, con datos en memoria.
Requiere JDK 23 y Maven. No utiliza persistencia ni servicios externos.

## Ejecutar desde IntelliJ

1. Abre el proyecto e importa `pom.xml` como proyecto Maven.
2. Selecciona JDK 23 como SDK del proyecto.
3. Abre `src/main/java/org/viajedragos/app.java` y ejecuta su método `main`.
4. En el menú inicial escribe `1` y pulsa Intro para comenzar una nueva aventura.
5. Cuando llegue la acción de Aventurero, escribe `1` para atacar o `2` para
   utilizar una poción que hayas obtenido en encuentros anteriores.
6. Tras la primera victoria, encontrarás la **Espada desgastada**: `1` la equipa
   y `2` la rechaza definitivamente. Aporta **Fuerza +1** y ninguna habilidad.
7. Después del botín y de la oferta de espada aparece el menú de exploración:
   `1` revisa el inventario, `2` permite usar una poción, `3` continúa al siguiente
   encuentro, `4` termina la aventura y vuelve al inicio, y `0` sale del juego.

El menú inicial ofrece `1. Nueva aventura` y `0. Salir`. Cada aventura crea un
Aventurero con 24/24 de vida, sin habilidades ni equipamiento. Cada encuentro crea
un Lobo nuevo con los valores actuales, sin dificultad automática ni límite de
encuentros. Al continuar se conserva exactamente la vida restante del Aventurero:
no hay curación automática entre encuentros ni progresión. La espada fija se ofrece
después del primer Lobo y no vuelve a ofrecerse durante esa aventura. Además, cada
enemigo derrotado definitivamente puede dejar pociones.

La espada permanece equipada y su bonificación no se acumula. La fuerza base
sigue siendo 6 y la efectiva pasa a 7. Equiparla no recupera vida. Las entradas
inválidas repiten la elección; cerrar la entrada durante la oferta termina la
sesión sin equiparla. No hay inventario de armas ni sustitución de equipo.

## Botín de pociones

Después de cada victoria se realizan dos comprobaciones independientes:

- Poción pequeña: 50 % de probabilidad, una unidad si tiene éxito.
- Poción grande: 20 % de probabilidad, una unidad si tiene éxito.

Puede obtenerse ninguna, una de las dos o ambas. La consola informa solo de las
unidades obtenidas. No hay botín por derrota, muerte simultánea o combate
interrumpido. Se espera al resultado definitivo después de posibles resurrecciones.

Las cantidades se acumulan en el inventario encapsulado del mismo Aventurero,
durante toda la aventura. Morir o terminar la aventura descarta el personaje y
sus pociones; una nueva aventura empieza con inventario vacío. Obtenerlas no cura.
La pequeña recupera hasta 8 PS y la grande hasta 16 PS al utilizarlas desde el
menú de exploración o durante la acción del jugador en combate.

El botín utiliza un Random propio, conservado durante la sesión, separado de los
dados de combate. Cada victoria solicita dos valores 0–99: éxito con <50 para la
pequeña y <20 para la grande. Los menús no repiten estas comprobaciones. En la
primera victoria se informa del botín antes de ofrecer la espada; sus opciones
y reglas no cambian.

## Exploración e inventario

El menú de exploración muestra nombre y vida actual/máxima con separadores ASCII.
Continuar inicia directamente otro combate; no hay tiradas de mazmorra.

Revisar inventario muestra únicamente las pociones disponibles, sus cantidades y
su recuperación máxima. Si no hay, indica «No tienes consumibles». También muestra
el equipo realmente equipado, sus bonificaciones y las habilidades que otorgue,
sin volver a aplicar efectos. `0` vuelve a exploración. Consultar no cambia el estado.

Usar objeto conserva las opciones fijas `1. Poción pequeña`, `2. Poción grande`
y `0. Cancelar`, incluso con cantidades cero. Un uso correcto recupera vida sin
superar el máximo, descuenta exactamente una unidad y vuelve a exploración.
La consola muestra los PS realmente recuperados y la cantidad restante: con
22/24, una pequeña recupera 2 PS. La última unidad se elimina del mapa.
Una poción inexistente, vida completa, cancelar o una entrada inválida no consume
nada. Tras un intento rechazado se puede elegir de nuevo o cancelar.

`Jugador.usarPocion(TipoPocion)` encapsula curación y descuento; devuelve la
recuperación real o cero si no hay consumo. Reutiliza `Combatiente.curarse(int)`
y sus eventos `alCurarse`, con el estado de cada habilidad activa. Instinto de
supervivencia mantiene su comportamiento según el porcentaje de vida, también
fuera del combate; el siguiente combate conserva su limpieza final existente.
No se añaden habilidades al Aventurero de la demo. No hay curación automática.

## Pociones durante el combate

Cuando llega la acción del jugador se muestra TU TURNO: `1. Atacar` y
`2. Usar objeto`. El submenú conserva `1. Poción pequeña`, `2. Poción grande` y
`0. Cancelar`, incluso cuando sus cantidades sean cero. Una poción utilizada
correctamente sustituye al ataque de esa acción: no hay ataque ni eventos de
ataque del jugador por el consumo. Se muestran recuperación real, vida resultante,
unidades restantes y «Tu acción ha terminado».

Cancelar vuelve al menú del turno. Entradas inválidas, falta de existencias, vida
completa o un consumo rechazado no terminan la acción ni realizan tiradas. El
enemigo conserva su acción en el orden determinado por iniciativa. No se permite
curarse antes de resolver un ataque previo del enemigo, ni después de morir
definitivamente. Inicio/final de turno, muertes, resurrecciones y limpieza final
mantienen su secuencia.

Se utiliza el mismo Jugador.usarPocion y sus eventos alCurarse que en exploración.
Instinto de supervivencia puede retirar su fuerza adicional al superar el umbral;
Segundo aliento puede completar la vida al inicio del turno antes de elegir.
No se alteran habilidades ni bonificaciones del equipo. El cierre de entrada
interrumpe la sesión como antes, sin resultado ni botín ficticio.

El constructor de Combate sigue aceptando Combatiente. Si el participante que
ocupa el lugar del jugador no es una instancia de Jugador, conserva únicamente
Atacar; elegir 2 es inválido y no consume su acción. No hay conversión forzada.
El modo automático siempre ataca y nunca consume pociones.

Si muere el Aventurero, incluso si mueren ambos, se muestra el resultado y se
regresa al menú inicial. Terminar voluntariamente la aventura también descarta
el personaje junto con su equipo e inventario. Una nueva aventura comienza con
otro Aventurero a 24/24, sin espada ni pociones, y puede conseguir sus propias recompensas.

Cada turno ordena las dos acciones mediante la iniciativa y los dados existentes.
El enemigo ataca automáticamente cuando le corresponde. Si actúa primero, su
ataque, las muertes y las posibles resurrecciones se resuelven antes de pedir la
decisión del jugador. Si hay muerte definitiva, no se ejecuta la acción pendiente.
El +2 sigue aplicándose solo al cálculo de iniciativa del poseedor del turno,
que alterna después de cada turno completo.

La consola muestra el orden, la vida actual/máxima, los fallos, los críticos,
los contraataques y la pérdida real de vida, limitada a la vida disponible.
Las entradas inválidas repiten la pregunta sin consumir acciones ni dados.
Cerrar la entrada interrumpe la sesión; no se interpreta como huida o victoria.

Los valores de Aventurero se definen en `app.ejecutar` y los del Lobo en
`app.jugarAventura`. Esta última conserva el jugador recibido, crea un nuevo
enemigo/Combate por encuentro y deja la limpieza de efectos a alFinalCombate.
`app.ofrecerEspada` crea una pieza por oferta y utiliza únicamente jugador.equipar;
no modifica fuerza directamente ni llama a aplicarEfectos manualmente.
El primer combate es de aprendizaje: ambos comienzan sin habilidades. Su
adquisición mediante progresión, recompensas o equipamiento se definirá más adelante.
Esta demo ofrece Atacar y usar las dos pociones en combate. Huida, otros
consumibles, cofres, tiradas de mazmorra, desplazamiento y comercio quedan pendientes.

## Comprobar el proyecto

```text
mvn clean test
```

También puedes compilar y ejecutar desde una terminal con JDK 23:

```text
mvn compile
java -cp target/classes org.viajedragos.app
```

`Combate.combatir()` sigue disponible para enfrentamientos automáticos, sin
entrada ni salida de consola. La sobrecarga `combatir(Scanner, PrintStream)`
comparte el mismo motor. Ambos modos aplican la regla confirmada de dos acciones
por turno; las pruebas anteriores de un ataque por turno se han adaptado a ella.

Las habilidades reales se comprueban con combatientes creados exclusivamente en
las pruebas de integración, sin asignarlas a la demo ni añadir narración.
`HabilidadesRealesIntegracionTest` verifica eventos, atributos, curaciones,
restauración, niveles y equipo. `RolesModificadoresDanioTest` comprueba los roles
confirmados: Caparazón, Defensa perfecta y Guardia férrea solo en defensa;
Castigo al torpe y Golpe certero solo en ataque. Incluye condiciones de activación,
instancias compartidas y reglas especiales. La suite completa pasa con 398 pruebas.
El análisis y la corrección están en `ANALISIS_ROLES_MODIFICADORES_DANIO.md`.

`AventuraTest` añade 12 casos de continuidad: identidad del jugador, enemigos
nuevos, vida sin recuperación, nueva aventura, muerte, muerte simultánea,
menús, entrada inválida/interrumpida y efectos temporales sin acumulación ni
pérdida de equipo o habilidades. Las habilidades observadoras y la contingencia
de muerte simultánea existen únicamente en las pruebas; no se asignan a la demo.

`RecompensaTest` añade 11 casos: primer combate sin equipo, oferta única, aceptación,
rechazo definitivo, vida/base intactas, conservación en tres encuentros, entradas
inválidas y cierre, derrota y muerte simultánea sin premio, nueva aventura sin
espada, daño real 7→8 con dados 4/2 y limpieza temporal que conserva la bonificación.
También comprueba que una ranura ocupada no se sustituye ni se anuncia como éxito.

La continuidad entre encuentros está publicada en `main`, commit `6fc2a8d`.
La Espada desgastada está publicada en `ec9dc46` y el botín de pociones en
`c1d20d`. El menú de exploración, consulta y consumo fuera de combate están
publicados en `e363c95`. El uso de pociones durante el combate es el cambio local
de esta fase; no se ha hecho commit ni push.

`InventarioPocionesTest` añade 8 casos de cantidades, datos de recuperación,
independencia entre jugadores, encapsulación y ausencia de curación automática.
`BotinPocionesTest` añade 21 casos de límites, cuatro resultados, acumulación,
continuidad, muertes/resurrección, interrupciones, compatibilidad con la espada y
consumo exacto de aleatoriedad. Enumera las 10.000 parejas posibles sin muestreo
aleatorio: 40 % ninguna, 40 % solo pequeña, 10 % solo grande, 10 % ambas.

Las firmas originales de ejecutar/jugarAventura siguen disponibles; las
sobrecargas de paquete permiten fijar un Random distinto del TiradorDado en
pruebas. Jugador expone cantidades y un mapa de solo lectura; el consumo se
gestiona mediante usarPocion. No se añaden clases genéricas de inventario; Combate
incorpora la elección de poción sin cambiar sus firmas públicas ni las reglas de ataque.

`ConsumoPocionesTest` añade 14 casos de recuperación, descuento, mapa de solo
lectura, vida completa, falta de existencias y eventos de curación. Comprueba
actividad/estado/nivel y el efecto real de Instinto con equipo y limpieza posterior.
`ExploracionTest` conserva sus 17 casos de consulta, equipo, opciones fijas, consumo,
recuperación real, cancelación, entradas inválidas/cierre y dos encuentros
conservando vida/pociones/equipo sin repetir botín ni consumir dados adicionales.
En la fase publicada de exploración se adaptaron Continuar de 1 a 3 y Terminar de
2 a 4. Esta fase conserva los 369 casos anteriores sin adaptarlos y añade 29:
25 en CombateConsolaTest (37 en total) y 4 en ExploracionTest (21 en total).
Verifican ambas pociones, sustitución de ataque, orden/eventos/estados, recuperación
real, cancelación/rechazo, muerte/resurrección, limpieza, participantes no Jugador,
modo automático, interrupciones y continuidad hacia exploración. Los dados de
combate y el generador de botín se controlan por separado.
