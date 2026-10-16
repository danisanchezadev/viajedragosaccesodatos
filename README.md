# Viaje a Dragos

Demo Java de una aventura de combates consecutivos por consola, con datos en memoria.
Requiere JDK 23 y Maven. No utiliza persistencia ni servicios externos.

## Ejecutar desde IntelliJ

1. Abre el proyecto e importa `pom.xml` como proyecto Maven.
2. Selecciona JDK 23 como SDK del proyecto.
3. Abre `src/main/java/org/viajedragos/app.java` y ejecuta su método `main`.
4. En el menú inicial escribe `1` y pulsa Intro para comenzar una nueva aventura.
5. Cuando llegue la acción de Aventurero, escribe `1` para atacar.
6. Tras la primera victoria, encontrarás la **Espada desgastada**: `1` la equipa
   y `2` la rechaza definitivamente. Aporta **Fuerza +1** y ninguna habilidad.
7. Después de resolver la recompensa, escribe `1` para continuar con el mismo Aventurero, `2` para
   terminar la aventura y volver al inicio, o `0` para salir del programa.

El menú inicial ofrece `1. Nueva aventura` y `0. Salir`. Cada aventura crea un
Aventurero con 24/24 de vida, sin habilidades ni equipamiento. Cada encuentro crea
un Lobo nuevo con los valores actuales, sin dificultad automática ni límite de
encuentros. Al continuar se conserva exactamente la vida restante del Aventurero:
no hay curación entre encuentros ni progresión. La única recompensa es la espada
fija después del primer Lobo; no vuelve a ofrecerse durante esa aventura.

La espada permanece equipada y su bonificación no se acumula. La fuerza base
sigue siendo 6 y la efectiva pasa a 7. Equiparla no recupera vida. Las entradas
inválidas repiten la elección; cerrar la entrada durante la oferta termina la
sesión sin equiparla. No existe inventario ni sustitución de armas.

Si muere el Aventurero, incluso si mueren ambos, se muestra el resultado y se
regresa al menú inicial. Terminar voluntariamente la aventura también descarta
el personaje junto con su equipo. Una nueva aventura comienza con otro Aventurero
a 24/24, sin espada, y puede conseguir su propia recompensa después de ganar.

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
Esta demo solo ofrece Atacar; huida, objetos, inventario, exploración y comercio
quedan pendientes.

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
instancias compartidas y reglas especiales. La suite completa pasa con 309 pruebas.
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
La recompensa de esta fase se ha implementado localmente; no se ha hecho commit ni push.
