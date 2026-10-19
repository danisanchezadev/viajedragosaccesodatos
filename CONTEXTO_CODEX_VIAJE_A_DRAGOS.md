# Viaje a Dragos — contexto de producto, reglas y plan de implementación para Codex

> **Naturaleza:** documento vivo de referencia para un agente de programación.
> **Fecha:** 18 de octubre de 2026.
> **Fuente de verdad del estado del código:** el repositorio actual. Las descripciones de implementación aquí proceden de conversaciones y fragmentos compartidos; **no sustituyen una inspección del código**.
> **Objetivo inmediato:** MVP Java de consola, sin ningún acceso a datos, que permita jugar y sirva como banco de pruebas para que otra persona aprenda posteriormente diferentes técnicas de persistencia.

## 1. Instrucciones obligatorias para Codex

1. **Lee este documento y examina el repositorio antes de cambiar nada.** No presupongas que las clases o firmas descritas siguen idénticas.
2. **No implementes acceso a datos de ningún tipo.** No conectes bases de datos; no añadas JPA, Hibernate, Spring Data, JDBC, DAO, repositorios de persistencia, lectura/escritura de TXT, CSV, JSON o binarios; no uses servicios remotos ni almacenamiento persistente. No hagas que el juego guarde o recupere partidas. Las colecciones Java en memoria sí están permitidas.
3. **Crea datos de prueba directamente en clases Java**, por ejemplo listas de enemigos, objetos, armas o encuentros, con instancias construidas en código. Esos catálogos serán reemplazados paulatinamente en una fase futura por orígenes externos. No diseñes ahora esa futura capa de acceso.
4. **No añadas patrones de diseño por iniciativa propia:** Builder, Factory, Abstract Factory, Strategy, Observer, etc. No refactorices el proyecto para exhibir patrones. Si alguno ya existe en el código, consérvalo cuando corresponda; no lo elimines sin autorización. El autor quiere incorporar y practicar esos patrones personalmente **después** de tener el MVP funcionando.
5. **No reescribas la aplicación ni hagas refactorizaciones masivas** salvo petición expresa. Prefiere cambios mínimos y verificables que reutilicen las clases y firmas existentes.
6. **No inventes reglas de juego, fórmulas, excepciones, progresión, recompensas ni comportamientos.** Cuando falte una decisión, márcala como pendiente y pregunta; no rellenes el hueco con una regla habitual de RPG.
7. **Una funcionalidad por petición.** No implementes automáticamente toda la lista de pendientes. Explica el alcance de lo solicitado y respeta los límites.
8. **Antes de cualquier cambio, entrega un análisis de impacto:** (a) archivos y métodos a modificar, (b) referencias y llamadas afectadas, (c) cambios de firmas o comportamiento, (d) posibles errores de compilación, (e) alternativa menos invasiva si existe. Distingue impactos comprobados en el repositorio de impactos posibles. Usa búsquedas de referencias; no obligues al autor a descubrir dependencias después.
9. **Tras el cambio, verifica** compilación y pruebas disponibles, indica qué se ha probado realmente y qué queda sin comprobar. No afirmes que algo funciona si no lo has ejecutado.
10. **Preserva el propósito educativo.** El juego no pretende ser un producto comercial complejo: su objetivo es una mecánica sencilla de combates, objetos, comercio y encuentros, con entidades que más adelante puedan obtenerse mediante distintos métodos de acceso a datos.
11. Mantén el código legible y cercano al nivel actual del proyecto. No añadas frameworks, servicios, capas, abstracciones o dependencias por comodidad del agente.
12. Si detectas discrepancias entre este documento y el código, **informa antes de decidir cuál prevalece**. Para el estado real de implementación, prevalece el repositorio; para la intención funcional, prevalecen las decisiones expresas del autor.

## 2. Visión narrativa del juego

*Viaje a Dragos* es una aventura de rol sencilla en Java. Un jugador controla a un personaje que puede encontrarse con enemigos, combatir, obtener o utilizar objetos, equiparse y comerciar. La aventura debe permitir probar progresivamente distintas entidades del mundo del juego: combatientes, armas, otros equipos, habilidades, objetos y, cuando se definan, encuentros y mercancías.

En el combate se enfrentan un jugador y un enemigo. Los personajes poseen atributos que influyen en su capacidad para actuar, atacar y defenderse. El enfrentamiento incorpora tiradas de dados, una decisión sobre quién actúa primero y habilidades que pueden activarse en distintos momentos. Algunas habilidades pueden modificar los resultados habituales: por ejemplo, al morir un combatiente podrían devolverlo a la vida, matar al oponente o alterar quién sobrevive. Por eso, el juego no debe decidir automáticamente que el combate ha terminado en cuanto la vida de alguien llega a cero: debe resolver los efectos pertinentes y volver a comprobar el estado de los participantes.

El equipo también importa. Un arma u otro objeto equipable puede otorgar bonificaciones o penalizaciones a varios atributos y conceder habilidades. Cambiar de equipo exige distinguir los atributos propios del personaje de los modificadores aportados por el equipo, para que una penalización o bonificación desaparezca correctamente al retirarlo. Antes de sustituir una pieza, el jugador puede comparar sus modificadores con los de la nueva pieza.

El juego permite atacar, intentar huir y utilizar pociones. El comercio sencillo está confirmado según 3.17. Las ampliaciones de estas mecánicas requieren reglas expresas; no inventarlas.

La primera versión utilizará **únicamente datos preparados dentro del propio programa**. Por ejemplo, una clase podrá ofrecer una lista de enemigos y otra una lista de armas. Esto permite probar las mecánicas sin consultar ni escribir ninguna fuente externa. En una fase posterior, el colaborador podrá sustituir gradualmente esos catálogos por acceso a bases de datos, JPA/Hibernate, Spring o formatos de archivo como TXT, CSV, binarios y JSON. **Esa fase futura no debe implementarse ahora.**

## 3. Reglas conocidas del combate

### 3.1 Participantes y atributos

- En el alcance actual, cada combate enfrenta a **un jugador y un enemigo**. No diseñar por defecto combates de grupos.
- Los atributos conocidos son `FUERZA`, `RESISTENCIA`, `AGILIDAD`, `INICIATIVA`, `VELOCIDAD` y `VIDA`.
- `VIDA` es una entrada del enum `Atributo`, pero la vida actual y la base de vida máxima se gestionan por separado. Desde 3.24, VIDA en equipo suma exclusivamente al máximo; no se acepta en los métodos genéricos de atributos ni modifica la vida actual al equipar.
- Los atributos base y los modificadores de equipo son conceptos diferentes. El atributo efectivo es `max(0, base + modificador)`.
- Una modificación permanente debe afectar al valor base. Una bonificación o penalización de equipo debe afectar a los modificadores y poder revertirse sin destruir el valor base.
- Decisión expresa del autor durante la tarea de equipo: conservar el sistema de habilidades que suma cambios a la base y los resta en otro momento, sin añadir un mapa temporal. Esos eventos usan el valor base sin incluir equipo, conservan las diferencias con signo y las revierten. Rabia también debe revertirse en `alFinalCombate`. Los cambios permanentes y los de equipo siguen separados; durante una habilidad activa la base incluye provisionalmente su efecto.
- Los modificadores pueden ser **negativos**. No se deben truncar al guardarlos: el límite de cero se aplica al calcular el atributo efectivo.

### 3.2 Dados, iniciativa y ataques físicos

Estas reglas proceden del diseño de combate previamente discutido; **Codex debe comprobar cómo están implementadas antes de asumir que están operativas**:

- La iniciativa inicial se decide comparando `INICIATIVA × tirada de 1D6` de cada combatiente. Si empatan, se repite la tirada hasta obtener un orden. Excepción confirmada: si ambas iniciativas efectivas son cero, se comparan directamente los dados y se repiten solo los empates. La repetición es iterativa, sin recursión.
- Regla confirmada para el turno: `atacante()` compara iniciativa + 1D6 y añade +2 únicamente al cálculo del poseedor del turno, con desempate a su favor. No aumenta agilidad ni modifica permanentemente iniciativa.
- Aclaración confirmada en la fase de consola: ese resultado determina el **primero en actuar**. Cada turno permite una acción de cada combatiente, primero el ganador de iniciativa y después el otro si el combate continúa. El jugador decide únicamente al llegar su acción, nunca antes de resolver la acción previa del enemigo. El poseedor sigue alternando al terminar cada turno completo; no se vuelve a tirar entre las dos acciones.
- Para un ataque físico se discutió: `daño = (FUERZA + AGILIDAD + 1D6 del atacante) - (RESISTENCIA + AGILIDAD + 1D6 del defensor)`, con daño mínimo de cero.
- También se discutieron casos especiales asociados a resultados naturales del dado: una tirada de agilidad de `1` provoca fallo; una tirada defensiva de `1` permite recibir el daño completo; la combinación de ataque `1` y defensa `6` puede producir contraataque. **La interacción exacta de estas excepciones con la fórmula debe contrastarse con el código y las decisiones posteriores** antes de completar la mecánica.
- Hay una abstracción de dado (`TiradorDado`) para que la tirada pueda controlarse en pruebas.

**Secuencia del ataque individual confirmada por el autor e integrada:** crear ContextoAtaque; antesDeAtacar solo del atacante; tirar ataque/defensa; antesDeCalcularDanio del atacante y después del defensor; calcular bruto con las reglas existentes; copiar bruto a final; antesDeAplicarDanio del atacante y después del defensor; aplicar el final una vez si el ataque no falló; alHacerDanio del atacante y alRecibirDanio del defensor solo si hubo pérdida real de vida. Cada evento recibe el EstadoHabilidad de su entrada. Las fases se completan en orden; dentro de cada combatiente se conserva la iteración del mapa actual, sin imponer un orden nuevo.

**Contraataque confirmado:** 1 de ataque y 6 de defensa mantienen el ataque original fallido. El defensor resta directamente su fuerza a la vida del atacante. No se crea otro contexto ni se invierten participantes; no se dispara otra secuencia ni eventos posteriores por ese daño. Los eventos previos del ataque original sí conservan sus fases.

**Roles confirmados y corregidos el 16 de octubre de 2026:** Caparazón, Defensa perfecta y Guardia férrea solo modifican el daño si pertenecen al defensor; Castigo al torpe y Golpe certero, solo si pertenecen al atacante. Cada implementación compara por identidad el estado recibido con el asociado a su habilidad en el combatiente correspondiente de ContextoAtaque. No se modifican firmas de eventos ni el despachador. Se mantienen sus condiciones, magnitudes, niveles y orden.

**Importante:** no mezclar estas fórmulas con reglas de otros reglamentos de rol discutidos en conversaciones distintas. Este documento describe el videojuego Java, no otro sistema de mesa.

### 3.3 Secuencia de combate conocida

1. Se determina el orden de activación de habilidades de inicio (`ordenHabilidades()`), según la lógica ya presente.
2. Se ejecutan los eventos `alInicioCombate` de las habilidades correspondientes, pasando a cada una su propio `EstadoHabilidad`.
3. Se comprueba si alguien ha muerto como consecuencia de esos efectos.
4. Si hay muertos, se ejecuta la resolución de muertes, que puede activar habilidades `alMorir` y cambiar el desenlace.
5. Si ambos siguen vivos, se determina el primer turno (`primerTurno()`).
6. El bucle compartido ejecuta inicio de turno para ambos vivos por prioridad y resuelve muertes. `atacante()` determina el orden de las dos acciones. Ejecuta la primera y resuelve muertes; si ambos siguen vivos, ejecuta la segunda y vuelve a resolver muertes. Si continúa, ejecuta final de turno para ambos y alterna el poseedor. En consola el enemigo ataca automáticamente y el jugador elige Atacar, usar una poción o Huir solo al llegar su acción, según 3.9/3.10. La huida exitosa cierra inmediatamente mediante limpieza final, sin acción pendiente ni fin de turno.
7. Regla final confirmada: una muerte definitiva cierra el combate sin eventos normales posteriores ni alFinalTurno. Solo el jugador ejecuta alFinalCombate, siempre, incluso muerto, como excepción de limpieza y contingencias. El enemigo no ejecuta alFinalCombate.

Decisiones expresas del autor para completar combatir: ambos pueden ejecutar habilidades en las fases correspondientes; la prioridad se determina por la iniciativa en ese momento, en ausencia de prioridad se usa el poseedor del turno, y si no lo hay se tira para determinar quién empieza. Si alguien muere antes de ejecutar su inicio, ejecuta alMorir en vez de ese inicio. El límite de una activación de muerte por participante y resolución se conserva. PrimerTurno mantiene iniciativa × 1D6 y el autor confirmó la excepción de comparación directa de dados si ambas iniciativas son cero. La huida se integra según 3.10; no se inventa un resultado o límite para combates sin progreso.

Una resurrección que deje a ambos vivos permite continuar. Si alguien vuelve a morir en otro momento, comienza otra resolución y puede ejecutar de nuevo alMorir una vez en esa resolución. Las fases de turno comprueban muertes después de cada participante; el muerto definitivo no ejecuta el siguiente evento ordinario. La operación ataque conserva toda su secuencia ya validada antes de resolver sus muertes.

### 3.4 Muerte y resultados

- Resultados conocidos: `CONTINUA`, `JUGADOR_MUERE`, `ENEMIGO_MUERE`, `AMBOS_MUEREN`.
- Un combatiente con cero vida no implica necesariamente un resultado definitivo: su habilidad `alMorir` podría resucitarlo o provocar la muerte del adversario.
- Tras ejecutar un efecto de muerte, se comprueba de nuevo quién sigue vivo.
- En una misma resolución de muertes, **cada personaje debe ejecutar sus efectos `alMorir` como máximo una vez**; evitar cadenas recursivas infinitas o activaciones duplicadas.
- Cuando ambos mueren, existe una noción de prioridad para decidir el orden de resolución. El método `resolverMuertes(Combatiente prioridad)` y su auxiliar `personajeMuere(...)` ya contienen lógica para varios casos, pero deben verificarse las ramas y el cumplimiento de la regla de una sola activación.
- `resolverMuertes` está pensado para invocarse cuando **al menos uno está muerto**.

### 3.5 Continuidad entre encuentros: fase 4 confirmada

- Cada aventura crea un único Aventurero con fuerza 6, resistencia 2, iniciativa 3, agilidad 2, velocidad 2 y 48/48 de vida, sin habilidades ni equipo inicial. La vida anterior era 24/24; ajuste autorizado según 3.11. Se conserva la misma instancia durante toda la aventura.
- Cada encuentro crea un enemigo y un Combate nuevos. Selección uniforme de los elegibles por nivel desde el primero según 3.23, sin Lobo obligatorio, escalado de atributos ni límite de encuentros.
- El jugador conserva la vida restante y su vida máxima, sin curación automática entre encuentros. Puede utilizar voluntariamente pociones según 3.8. No se reinician atributos ni se eliminan equipo o habilidades; la limpieza temporal sigue en alFinalCombate.
- Tras victoria, botín y oferta de espada si corresponde: menú de exploración con `1. Revisar inventario`, `2. Usar objeto`, `3. Continuar al siguiente encuentro`, `4. Terminar aventura y volver al menú inicial`, `0. Salir del juego`. Continuar conserva al jugador; terminar lo descarta; salir termina la sesión. Sustituye la numeración inicial de la fase 4.
- Menú inicial: `1. Nueva aventura`, `0. Salir`. Una nueva aventura crea un personaje nuevo.
- Muerte del jugador o de ambos: mostrar resultado, terminar aventura y volver al menú inicial. El motor resuelve previamente sus muertes y contingencias según las reglas existentes.
- Experiencia y progresión añadidas según 3.22. Las recompensas conservan las espadas, el botín de pociones y cinco monedas por victoria según 3.17. Consumo fuera de combate según 3.8.
- Si se interrumpe la entrada durante el combate, termina la sesión sin resultado ficticio ni reutilización del personaje interrumpido.
- Implementación limitada a app.java: ejecutar conserva el menú inicial y crea el jugador; jugarAventura, de acceso de paquete, contiene la repetición de encuentros y devuelve si corresponde volver al inicio o salir. No cambia el motor ni sus contratos.

### 3.6 Primera recompensa de equipamiento: fase 5 confirmada

- Solo después de la primera victoria efectiva de cada aventura: mostrar el resultado y ofrecer una Espada desgastada, TipoEquipo.ARMA, FUERZA +1, sin habilidades otorgadas. Huir no cuenta como victoria; si se huye del primer Lobo y se vence al segundo, se ofrece entonces. El primer combate sigue sin equipo ni habilidades iniciales.
- Pregunta independiente: `1. Equipar Espada desgastada`, `2. Rechazar Espada desgastada`. Después aparece el menú habitual entre encuentros. Entradas inválidas repiten la pregunta sin crear otra pieza, aplicar efectos o consumir dados.
- Aceptar llama una sola vez a jugador.equipar y comprueba su booleano antes de anunciar éxito. No se modifica fuerza directamente ni se llama a aplicarEfectos manualmente. Si la ranura está ocupada, se informa del rechazo sin sustituirla.
- Rechazar descarta definitivamente la pieza; el inventario de pociones no almacena armas y no permite recuperarla. No se vuelve a ofrecer en encuentros posteriores, se acepte o se rechace.
- La espada y su FUERZA +1 permanecen con la misma instancia del jugador en los encuentros posteriores. Base de fuerza 6, efectiva 7 en el Aventurero sin otros efectos. No se modifica vida actual/máxima.
- Morir o terminar aventura descarta personaje y equipo. Una aventura nueva empieza sin equipo y puede recibir su propia espada tras su primera victoria.
- Si se cierra la entrada durante la oferta, termina la sesión sin equipar ni iniciar otro encuentro. La victoria ya obtenida conserva su resultado real.
- Implementación en app.ofrecerEspada, llamado desde jugarAventura con ENEMIGO_MUERE y espadaOfrecida == false. El booleano local se marca antes de la oferta, tanto para aceptar como para rechazar; sustituye el antiguo encuentro == 1. La pieza procede ahora del catálogo de objetos de 3.12; no hay motor de recompensas ni sustitución automática de equipo.

### 3.7 Botín de pociones publicado en c1d20d

- Todos los enemigos derrotados definitivamente pueden dejar pociones: dos comprobaciones independientes por victoria, pequeña 50 % y grande 20 %. Cada éxito concede una unidad; son posibles ninguna, pequeña, grande o ambas.
- TipoPocion contiene PEQUENA (Poción pequeña, recuperación máxima 8 PS) y GRANDE (Poción grande, 16 PS). El enum mantiene los datos; Jugador gestiona su consumo según 3.8.
- Jugador mantiene un EnumMap privado final de cantidades. agregarPocion suma una unidad, getCantidadPociones devuelve cero si no existe y getInventarioPociones ofrece una vista de solo lectura. Ambos constructores comienzan vacíos.
- El mismo personaje conserva el inventario entre encuentros. Al morir/terminar se descarta el personaje con sus posesiones, como ya ocurre con el equipo; una aventura nueva crea otro jugador vacío. No se reinicia ni vacía el inventario entre encuentros.
- app.obtenerBotin se ejecuta una vez cuando combatir devuelve ENEMIGO_MUERE, después de su limpieza final y antes de la espada/menús. Nunca por derrota, muerte simultánea o combate interrumpido; no basta que un enemigo llegue provisionalmente a vida cero si puede resucitar.
- Aleatoriedad: Random exclusivo del botín, creado una vez al ejecutar la sesión y pasado a todos los encuentros/aventuras. Dos nextInt(100) independientes, pequeña <50 y grande <20. No se usa Dado/TiradorDado ni se reinicializa el generador en el bucle.
- Se conservan ejecutar(Scanner, PrintStream, TiradorDado) y jugarAventura(Jugador, Scanner, PrintStream, TiradorDado). Sobrecargas de paquete aceptan Random para pruebas controladas; la llamada directa a jugarAventura sin Random crea uno para esa aventura y lo conserva durante todos sus encuentros.
- La consola solo informa de pociones realmente obtenidas. Obtener una poción no cambia vida, atributos, habilidades ni equipo. La espada mantiene sus reglas y su oferta única. Consulta/consumo según 3.8; sin cofres, mazmorras, botín genérico, comercio ni persistencia.

### 3.8 Exploración, consulta y consumo de pociones entre combates

- Publicado en e363c95 sobre c1d20d. El menú de exploración aparece después del botín y de resolver la espada cuando corresponda; muestra nombre y vida actual/máxima. Continuar (3) inicia directamente otro combate, sin tiradas de mazmorra ni curación automática.
- Consultar inventario (1) muestra solo pociones con cantidad positiva, sus cantidades y recuperación máxima; si no hay, «No tienes consumibles». Muestra las piezas realmente equipadas, bonificaciones con signo y habilidades otorgadas, sin reaplicar efectos ni permitir operaciones de equipo. 0 vuelve a exploración; consultar no cambia el estado.
- Usar objeto (2) ofrece siempre 1 pequeña, 2 grande y 0 cancelar, incluso con cantidad cero. No se consume si falta la poción o la vida está completa. Entrada inválida/cancelación no cambia el estado. Un intento rechazado permite elegir de nuevo; un éxito vuelve a exploración.
- Jugador.usarPocion(TipoPocion) devuelve los PS realmente recuperados o cero si no consume. Comprueba existencias y vida, reutiliza curarse y descuenta una unidad solo si hay recuperación positiva; elimina del mapa la última unidad. No expone escritura externa ni modifica firmas anteriores.
- La consola calcula el resultado mediante la recuperación devuelta y muestra vida y cantidad restante. Con 22/24, una pequeña cura 2 PS. Nunca se supera la vida máxima mediante la recuperación de la poción.
- Se conservan los eventos alCurarse activos y sus estados/niveles. Instinto de supervivencia mantiene sus efectos según vida, aun fuera de combate; el siguiente combate conserva su restauración final. No se añaden habilidades a la demo.
- app conserva el bucle entre encuentros y contiene métodos sencillos de presentación, consulta, uso y lectura de opciones. Las cabeceras ASCII de esta fase inicial se sustituyen por PanelConsola en 3.18. El cierre de entrada en los submenús termina la sesión sin otra acción o encuentro.
- El consumo durante el combate está implementado según 3.9 y la huida según 3.10. Siguen pendientes cofres, mazmorra y otros consumibles.

### 3.9 Pociones durante la acción del jugador en combate

- Publicado en f97e7fc sobre e363c95. En modo interactivo, Combate muestra TU TURNO solo al llegar la acción del jugador, después de resolver cualquier acción previa del enemigo. Opciones 1 Atacar y 2 Usar objeto, ampliadas con 3 Huir según 3.10.
- El submenú mantiene 1 pequeña, 2 grande y 0 cancelar, aunque la cantidad sea cero. Reutiliza exclusivamente Jugador.usarPocion para curación/descuento; muestra recuperación real, vida y unidades restantes.
- Un resultado de recuperación positiva consume la acción y sustituye al ataque: no se crea ContextoAtaque ni se ejecutan eventos/tiradas de ataque por usar la poción. Después se conserva la comprobación de muertes, la acción pendiente del enemigo, los eventos de fin de turno y la alternancia actuales.
- Cancelar vuelve al menú del turno; inválidos, poción inexistente, vida completa o consumo rechazado permiten seguir eligiendo sin consumir acción, dados ni eventos de turno adicionales.
- ejecutarAccionJugador sustituye al antiguo booleano privado elegirAtaque: ejecuta la acción y devuelve CONTINUA o JUGADOR_HUYE según 3.10. usarPocionEnCombate conserva true tras éxito y false al cancelar; no son contratos públicos ni un sistema genérico de acciones.
- El constructor conserva Combatiente. Si el participante en el lugar del jugador no es Jugador, solo ofrece Atacar y rechaza 2 o 3 sin perder la acción, sin conversión inválida ni acceso a inventario. El modo automático no muestra menús y siempre ataca, aunque un Jugador tenga pociones.
- Se conservan alCurarse, actividad/estados/niveles y las fórmulas de habilidades. Instinto puede retirar su fuerza al curarse y reactivarla tras daño según su umbral; la limpieza conserva el equipo. Segundo aliento puede completar vida al inicio del turno y hacer innecesaria una poción.
- Scanner.nextLine mantiene NoSuchElementException al cerrar la entrada en cualquiera de los menús. app ya captura esa excepción: interrumpe sin resultado ficticio, botín ni finalización de un combate incompleto.
- En la fase publicada de pociones solo cambió Combate.java, sus pruebas y documentación. No se cambiaron firmas públicas, Jugador, app, habilidades, dados, equipo ni recompensas. La fase de huida amplía Combate y app según 3.10. Presentación ASCII sencilla en Combate sin dependencia hacia app ni nuevas clases productivas; las reglas de consumo siguen centralizadas en Jugador.

### 3.10 Huida del jugador — implementación autorizada

- Publicado en main 40463bd sobre f97e7fc. Seleccionar 3 Huir realiza inmediatamente dos tiradas de TiradorDado, una del jugador y otra del enemigo, sin confirmación. Éxito si velocidad efectiva del jugador + dado > velocidad efectiva del enemigo + dado. Incluye equipo y efectos activos; empate falla sin repetición ni límites adicionales de probabilidad.
- ejecutarAccionJugador ejecuta ataque, consumo o intento de huida. Devuelve CONTINUA tras ataque/poción/huida fallida y JUGADOR_HUYE tras éxito. No añade clases, estados ni contratos públicos. Solo Jugador dispone de opciones 2 y 3; otros Combatiente conservan Atacar. El modo automático siempre ataca.
- En combatirInterno, JUGADOR_HUYE termina inmediatamente mediante finalDelCombate: sin ataque del jugador, acción enemiga pendiente ni alFinalTurno. Sí ejecuta alFinalCombate del jugador para restaurar temporales y conserva vida, equipo, inventario y habilidades. alHuir y alPerseguir siguen sin conectar por decisión expresa.
- Fallar consume una acción sin ataque, eventos de ataque, daño ni penalización adicionales. El enemigo pendiente actúa; si ya actuó, no repite. Se conserva resolución de muertes/resurrecciones después de acciones y los eventos finales/alternancia si el combate continúa. Navegar o cancelar pociones no repite iniciativa ni inicio de turno.
- app trata explícitamente JUGADOR_HUYE: mensaje de escape y regreso a exploración, sin victoria/derrota, botín ni espada. Continuar usa el mismo Jugador con vida restante y crea un enemigo/Combate nuevos; el abandonado no se conserva. Terminar o morir mantiene el descarte habitual del personaje.
- La espada se ofrece una vez tras la primera victoria real, usando espadaOfrecida local según 3.6. Huir no marca ese booleano; aceptar o rechazar la primera oferta impide repetirla.
- El cierre de entrada conserva NoSuchElementException e interrupción sin resultado ficticio ni recompensas. No hay huida enemiga, persecución, nuevas habilidades/consumibles ni cambios de reglas de ataque o iniciativa.

### 3.11 Catálogo sustituible y equilibrio inicial autorizados

- Fase publicada en a630e26, desarrollada sobre 40463bd. El autor autorizó expresamente DefinicionEnemigo (record), RepositorioEnemigos (interfaz) y RepositorioEnemigosMemoria. Esta interfaz específica prepara la sustitución didáctica del origen de datos sin implementar persistencia, frameworks, servicios ni fábricas.
- Contrato List<DefinicionEnemigo> obtenerTodos(), sin aleatoriedad. El record almacena id técnico, nombre, descripción, fuerza, resistencia, iniciativa, agilidad, velocidad y vida máxima. crearEnemigo construye un Enemigo nuevo con vida completa y sus mapas propios, sin habilidades/equipo. No se almacenan combatientes mutables en el catálogo.
- Catálogo en memoria, inmutable y ordenado: lobo/Lobo (5,2,2,1,2,18), goblin/Goblin (3,1,2,2,3,12), bandido/Bandido (4,1,2,1,2,14), esqueleto/Esqueleto (3,2,1,1,1,14). Orden numérico: fuerza, resistencia, iniciativa, agilidad, velocidad, vida máxima. Descripciones inicialmente vacías. Id y nombre obligatorios no vacíos, descripción no nula; atributos no negativos y vida máxima positiva.
- app carga List.copyOf del catálogo una vez al comenzar cada aventura. Catálogo vacío/nulo, entradas nulas, ids duplicados o definición inválida impiden iniciar la aventura con mensaje y regreso al menú inicial. No hay respaldo inventado, reintentos ni infraestructura de errores externos.
- Selección privada en app: nextInt(catalogo.size()), uniforme (25 % cada tipo), desde el primer encuentro y con repeticiones permitidas. Crear siempre una instancia nueva, incluso si repite definición. La aventura conserva el jugador, vida, inventario, equipo, huida y recompensas actuales.
- Random de encuentros independiente de TiradorDado y del Random de botín. Se crea una vez por sesión y se pasa entre aventuras/encuentros. Las firmas anteriores se conservan; nueva sobrecarga pública ejecutar(Scanner, PrintStream, TiradorDado, Random azarBotin, RepositorioEnemigos, Random azarEncuentros), y sobrecarga de paquete de jugarAventura para pruebas.
- Para cambiar el origen, proporcionar otra implementación del contrato en el arranque mediante esa sobrecarga. No modificar selección, exploración, Enemigo ni Combate. Archivos, JDBC, JPA y otras tecnologías siguen pendientes y no están implementados.
- Equilibrio: solo la vida inicial de consola cambia 24/24 a 48/48, manteniendo 6/2/3/2/2 en los demás atributos. Sin cambios en Jugador o fórmulas. El jugador elige equipar espada; no se equipa automáticamente. Se justifica con simulación del motor real de cuatro encuentros, sin consumibles/huida y con fuerza +1 desde el segundo.
- EquilibrioAventuraTest: 10.000 sesiones por candidato con generadores de semillas 20261018 (dados) y 20261019 (encuentros), nextLong independiente por sesión. Resultados de cuatro victorias: vida 24: 53,34 %; 28: 63,87 %; 32: 72,63 %; 36: 80,46 %; 40: 86,18 %; 48: 94,02 % (9.402/10.000). A 48 PS: 2,991 turnos/combate y 25,66 PS restantes medios entre supervivientes. La prueba exige >=90 % con semillas fijas; es objetivo de diseño, no una regla o garantía del motor.
- Se mantienen los 422 escenarios anteriores, inyectando solo-Lobo en las pruebas de aplicación. Los escenarios que construyen el Aventurero desde ejecutar adaptan expectativas de vida y secuencias letales; los combatientes de prueba propios mantienen sus datos originales. Nuevos casos: 15 catálogo/validación/aislamiento, 14 selección/integración/repositorio sustituible y 1 simulación reproducible.

### 3.12 Catálogo de objetos autorizado

- Publicado en main d796ffc, desarrollado sobre a630e26. Cuatro tipos nuevos: DefinicionArma y DefinicionPocion (records en modelo), RepositorioObjetos y RepositorioObjetosMemoria (catalogo). Sin persistencia ni nuevas mecánicas.
- Contrato exclusivo List<DefinicionArma> obtenerArmas() / List<DefinicionPocion> obtenerPociones(). Listas de memoria inmutables. Datos: espada_desgastada/Espada desgastada/FUERZA +1, pocion_pequena/PEQUENA/8 PS y pocion_grande/GRANDE/16 PS. Los nombres y curación iniciales de pociones proceden de TipoPocion, sin duplicar sus valores.
- DefinicionArma guarda id, nombre y copia inmutable del mapa de bonificaciones. CrearArma genera una Arma nueva, ranura ARMA y habilidades vacías. Se admiten penalizaciones, se rechaza VIDA igual que en Equipo. DefinicionPocion guarda id, tipo, nombre y recuperación positiva. No guardan cantidades, personajes ni habilidades mutables.
- Jugador conserva ambos constructores anteriores y admite uno adicional con List<DefinicionPocion>. configurarPociones valida/copia el conjunto completo antes de reemplazarlo, sin modificar inventario; getDefinicionPocion devuelve su record inmutable. Se necesitan ambos tipos, ids únicos y una definición por tipo. La configuración pertenece al jugador, sin global mutable ni repositorios en el motor.
- TipoPocion mantiene identidad, nombres, 8/16 PS y métodos originales como predeterminados. Para un jugador configurado, únicamente sus definiciones determinan nombre y recuperación: una pequeña de 10 PS se muestra y usa como 10 PS. Inventario, botín, exploración y menú de combate consultan esa misma fuente efectiva. usarPocion conserva curarse, alCurarse, límite de vida, recuperación real y descuento solo tras éxito.
- Nueva sobrecarga pública app.ejecutar(Scanner, PrintStream, TiradorDado, Random azarBotin, RepositorioEnemigos, Random azarEncuentros, RepositorioObjetos), sin eliminar las anteriores. Sobrecarga correspondiente de jugarAventura de paquete. Una consulta a cada lista por aventura; instantáneas antes del primer encuentro. No se consulta en cada menú/uso/encuentro.
- Validación antes de iniciar: colecciones/entradas nulas, ids repetidos incluso entre categorías, espada ausente (id técnico espada_desgastada, no nombre visible), pociones incompletas, tipos repetidos o datos inválidos impiden comenzar y devuelven al inicio con mensaje. Sin respaldos silenciosos. Se conserva el rechazo previo de enemigos inválidos.
- Oferta única tras primera victoria real, aceptación/rechazo, ranuras, botín 50/20 independiente, continuidad, huida y reglas del combate intactas. Solo cambian consultas de datos en el menú de Combate, sin dependencia de RepositorioObjetos. Equipar pasa por Combatiente.equipar; no se duplican efectos.
- Repositorios de archivos/JDBC/JPA podrán devolver estos mismos records mediante el contrato; no existen aún. Sin nuevos objetos, inventario genérico ni catálogo de habilidades. El equipo existente que concede habilidades sigue funcionando; externalizarlas requiere otra fase.
- Pruebas: 18 de catálogo y 31 de integración añadidas, con 452 anteriores intactas; mvn clean test confirma 501, sin fallos/errores/omitidas, 41 fuentes principales y 35 de pruebas. Datos alternativos de espada y pociones, validación, instantáneas, consumo dentro/fuera, estados/eventos y equipo probados.

### 3.13 Inventario de equipamiento y comparación autorizados

- Cambios locales sobre d796ffc. Jugador añade List<Equipo> privada, agregarEquipo y getInventarioEquipo (vista no modificable). Sin límite de capacidad/peso/espacios; admite piezas repetidas y mantiene identidad y orden de inserción. Rechaza null. No modifica pociones ni ranuras, no aplica efectos y no realiza traslados automáticos entre inventario y equipo equipado.
- consultarInventario conserva firma anterior y añade sobrecarga con boolean coloresAnsi. Muestra consumibles, equipo equipado y equipamiento guardado con sus bonificaciones/habilidades; opciones 1 comparar y 0 volver. Comparación: seleccionar equipada por orden de TipoEquipo, seleccionar guardada filtrando la misma ranura. 0 en guardada vuelve a equipada, 0 en equipada vuelve al inventario. Tras cada comparación sigue la selección de guardadas. Vacíos/incompatibles se informan sin error; inválidos repiten y EOF interrumpe como antes.
- Se reutiliza Equipo.compararCon. Firma anterior conservada y delegada a compararCon(Equipo, PrintStream, boolean) sin colores. Único cálculo existente: nueva menos actual, unión de claves con EnumSet para ordenar por Atributo. Comparar es informativo; no cambia atributos, equipo, habilidades, pociones ni lista guardada.
- ANSI solo en valores DIFERENCIA: verde positivo, rojo negativo, cero normal, reset inmediato por valor y alineación equivalente. Sin colores en otras columnas/menús. Desactivado por defecto; activar con VM -Dviajedragos.ansi=true para el flujo hacia System.out. Un PrintStream arbitrario no se considera compatible automáticamente; pruebas/llamadores de la sobrecarga eligen el booleano explícito. Sin librerías ni sistema general de estilos.
- Al cerrar 3.13 no se concedía equipo guardado ni había intercambio; esa limitación queda ampliada por 3.14. La Espada desgastada conserva su oferta original y rechazo sin almacenarla. Combate y las operaciones originales equipar/desequipar permanecen intactos.
- Suite real: 538 pruebas, 0 fallos/errores/omitidas, BUILD SUCCESS; 501 anteriores intactas y 37 nuevas (8 inventario, 9 comparador, 20 consola/integración). Recompiladas 41 fuentes principales y 38 archivos de pruebas. Sin commit ni push.

### 3.14 Obtención e intercambio autorizados

- Se continúa sobre los cambios locales de 3.13, comprobados antes de editar, sin reconstruirlos ni sobrescribirlos. Base publicada d796ffc; no se realiza commit ni push.
- Nueva definición espada_hierro/Espada de hierro, ARMA y FUERZA +2, sin habilidades. Memoria contiene dos armas y dos pociones. El catálogo de aventura requiere ambas espadas por id; repositorios alternativos deben proporcionarlas y siguen rechazándose datos incompletos sin respaldo.
- Un contador local de victorias definitivas ofrece hierro únicamente cuando alcanza dos, después del botín. Huir/perder/muerte simultánea no suman. Oferta única: 1 guardar mediante agregarEquipo, 2 dejar atrás; inválidos repiten, EOF interrumpe sin obtener la pieza. Guardar no equipa ni modifica atributos. La espada desgastada conserva su primera oferta original.
- Inventario: 1 comparar, 2 cambiar, 0 volver. Cambio: elegir una de todas las ranuras (incluidas vacías), elegir solo una pieza compatible guardada y confirmar con 1 o cancelar con 0. Comparación existente antes de confirmar si hay pieza actual. Tras éxito se vuelve al inventario; vacíos/incompatibles e inválidos se informan. EOF no confirma.
- Jugador.equiparDesdeInventario(TipoEquipo, int) usa índice global de lista desde cero; rechaza índices inválidos/incompatibles sin cambios. Tras equipar con éxito elimina solo esa entrada y añade la pieza anterior al final, preservando identidades y duplicados. En vacío no hay pieza anterior. Sin límite de inventario ni descarte automático.
- Combatiente añade cambiarEquipoSeguro protegido, sin modificar equipar/desequipar ni sus efectos. Conserva instantáneas de ranuras, modificadores, base, vida y mapa/valores de EstadoHabilidad. Ante false/RuntimeException restaura estado e identidades, sin ejecutar otra retirada/aplicación potencialmente fallida. Solo tras éxito cambia la lista de Jugador. No se modifican eventos, combate, habilidades ni algoritmo de compararCon.
- Suite real: 572 casos, 0 fallos/errores/omitidas, BUILD SUCCESS; 538 anteriores y 34 nuevos. Ajustes previos limitados a responder la nueva oferta, cambiar un inválido de 2 a 3 y ampliar la expectativa de catálogo de una a dos armas. Pruebas de restauración antes/después de efectos, habilidad retirada/restaurada, slots vacíos, identidad, continuidad y nueva aventura. Compiladas 41 fuentes principales y 40 archivos de pruebas.
- Siguen fuera de alcance tiendas, cofres, monedas, aleatoriedad de equipo, otras piezas o habilidades y persistencia. No se habilitan objetos ni cambios de equipo durante el combate.

### 3.15 Corrección de retirada de efectos temporales

- La auditoría de 3.14 reprodujo Instinto activado al curarse fuera de combate: quitar su única pieza eliminaba la habilidad y dejaba FUERZA +3 sin limpieza futura. La corrección está autorizada y no altera fórmulas ni pociones.
- Habilidad añade retirarEfectosTemporales(Combatiente, EstadoHabilidad), por defecto vacía, y guardarEstadoTemporal(), que devuelve un Runnable para restaurar únicamente sus indicadores internos. No se añaden eventos ni se simula alFinalCombate al desequipar.
- InstintoSupervivencia, Impetu, PrimerImpulso, FuriaContenida y Rabia extraen su limpieza anterior a la nueva operación; alFinalCombate delega en ella. Conservan magnitudes y niveles internos. La limpieza inactiva es inocua. En la corrección posterior de 3.16 sus booleanos y contador se trasladan a EstadoHabilidad.
- Combatiente.quitarHabilidadEquipo limpia exclusivamente antes de eliminar la última fuente no aprendida. Usa la instancia realmente almacenada, ya que la igualdad por clase permite recibir otra instancia como argumento. Si sigue aprendida o equipada por otra pieza, mantiene efecto/estado y solo descuenta la contribución retirada.
- cambiarEquipoSeguro amplía sus instantáneas con restauraciones internas por identidad de las habilidades existentes y las del equipo nuevo. Ante fallo restaura también esos indicadores después de atributos y EstadoHabilidad; de otro modo el siguiente cierre o curación no podría limpiar el efecto recuperado.
- Caso exacto corregido: fuerza base vuelve a 6, efectiva 8 con arma +2, sin habilidad residual y sin restos tras otro combate. 29 regresiones nuevas (26 retirada, 3 cancelación); 572 pruebas anteriores intactas. mvn clean test: 601, 0 fallos/errores/omitidas, BUILD SUCCESS. Sin cambios de menús, catálogos, dados, botín, huida ni reglas de combate.
- Cualquier futura habilidad temporal deberá definir su limpieza y mantener sus datos por combatiente; las habilidades sin ese estado conservan operaciones vacías. El aislamiento posterior se describe en 3.16.

### 3.16 Aislamiento del estado temporal entre combatientes

- Defecto reproducido: compartir una instancia de Instinto entre dos personajes compartía también activo; uno no recibía la mejora y podía restar fuerza inexistente al desequipar.
- Se aísla el estado, sin clonar habilidades: EstadoHabilidad incorpora efectoTemporalActivo y acumulacionesTemporales. Las cinco implementaciones temporales usan exclusivamente el estado recibido, que los métodos de aprendizaje/equipamiento crean por combatiente. Se conservan niveles internos, condiciones, magnitudes y fórmulas.
- La identidad de Habilidad puede compartirse sin interferencias temporales. equals/hashCode por clase y las claves/referencias del equipo se mantienen: varias piezas siguen contribuyendo a una única entrada dentro del personaje.
- cambiarEquipoSeguro copia y restaura también los datos temporales de los mismos EstadoHabilidad. La copia de seguridad no repite efectos y conserva las identidades originales; guardarEstadoTemporal sigue disponible por compatibilidad para extensiones, pero las cinco habilidades actuales ya no almacenan indicadores en su instancia.
- No se modifican dispararHabilidades, EventosHabilidad, Combate, Equipo, Jugador, turnos, menús, catálogos ni recompensas. agregarHabilidad conserva su comportamiento previo (no incorpora entradas nuevas); aprenderHabilidad y agregarHabilidadEquipo son las operaciones de alta utilizadas.
- Las llamadas directas a eventos deben reutilizar el EstadoHabilidad de su entrada. El mapa y setHabilidades conservan su API previo de bajo nivel; insertar manualmente el mismo EstadoHabilidad en dos personajes no forma parte del aislamiento de referencias Habilidad y debe evitarse.
- 23 casos nuevos en AislamientoHabilidadesTest: reproducción exacta, cinco habilidades, activa/inactiva, aprendizaje/equipo, varias piezas, intercambio/restauración, contador por atacante y ciclos. Los 601 casos anteriores permanecen; ImpetuTest y PrimerImpulsoTest adaptan únicamente su preparación para reutilizar el estado entre eventos, con todas sus aserciones conservadas.
- Verificación real: mvn clean test, BUILD SUCCESS; 624 pruebas, 0 fallos/errores/omitidas; 41 fuentes principales y 43 archivos de pruebas. Sin commit ni push.

### 3.17 Oro y comercio sencillo

- Base de esta fase: 0ab4dff; código incorporado al commit 88934c5 durante la sesión por una operación externa a Codex. Codex no ejecutó commit ni push.
- Jugador empieza con 15 monedas, conserva saldo entre encuentros y recibe +5 solo por victoria definitiva. No se premia huida, derrota ni muerte simultánea. Nueva aventura: jugador nuevo, sin pertenencias y saldo 15.
- getOro/agregarOro/gastarOro rechazan cantidades no positivas y evitan saldo negativo o desbordamiento al sumar. No hay clase monedero ni economía genérica.
- ProductoTienda asocia exactamente una DefinicionArma o DefinicionPocion con precio positivo; id y nombre proceden de la definición. RepositorioTienda permite obtenerTodos/buscarPorId. Su implementación en memoria reutiliza las definiciones ya cargadas de RepositorioObjetos, con cuatro productos: pequeña 5, grande 10, desgastada 12, hierro 20.
- Jugador.comprar(id, RepositorioTienda) consulta precio, valida definición y saldo, incorpora una unidad y cobra al final. Las pociones se acumulan; cada arma es nueva y se guarda sin equipar. Ante fallo se recuperan saldo e inventarios, incluso si la incorporación fue parcial. No se permite comprar una definición de poción distinta de la configurada para ese tipo en el jugador.
- Exploración añade 5. Tienda sin renumerar 0–4. Se muestra oro en exploración y tienda; 0 en tienda vuelve, inválidos repiten y EOF interrumpe la sesión. Las firmas previas de app siguen disponibles; una sobrecarga permite proporcionar RepositorioTienda alternativo. No hay venta, descuentos, stock limitado ni persistencia.
- 691 casos reales superados: 624 anteriores más 67 de oro/catálogo/compra/integración. AventuraTest y BotinPocionesTest sustituyen el inválido 5 por 9 sin cambiar aserciones.

### 3.18 Interfaz de consola unificada

- Auditoría de todas las salidas de src/main/java: app, Combate, Equipo, el aviso breve de Combatiente.setEquipo y dos mensajes de depuración de DebilidadEnemiga. Los catálogos y demás modelos no imprimen pantallas.
- Ya existían paneles manuales para inicio, encuentro, victoria/recompensas, turno, acciones y ataques. Usaban interiores de 44/48/49 y no ajustaban textos largos; los demás menús aún eran planos o ASCII. Se conserva esa estética y se sustituye su construcción repetida por PanelConsola.
- PanelConsola: interior 48, márgenes 2 por lado y 44 de texto; bordes Unicode, títulos centrados, opciones [n], separadores, columnas ajustables, barras proporcionales de 10 segmentos. Divide textos largos sin cortar puntos de código; los SGR ANSI no cuentan como anchura y se cierran antes del borde. Referencia: IntelliJ con fuente monoespaciada y los caracteres latinos/bordes/barras del juego; no hay implementación universal de anchura para emojis o escrituras de doble columna.
- Se unifican exploración, tienda, inventario y atributos existentes, comparación, cambio/confirmación, pociones en ambos contextos y ofertas de armas. Avisos breves, validaciones, confirmaciones, botín de pociones y resultado textual pueden seguir fuera de paneles. Se eliminan solo los dos println de depuración de medias de DebilidadEnemiga, sin cambiar su efecto.
- Se conserva el bucle original de Combate: el borrador visual local había dejado final de turno, alternancia e incremento dentro de un bloque comentado. Se recupera el flujo publicado y se reemplaza únicamente su presentación. Dados, fórmulas, eventos, muerte/resurrección, acción de poción y huida no cambian.
- AGENTS.md establece ESTÁNDAR DE INTERFAZ DE CONSOLA — VIAJE A DRAGOS y conserva la preferencia de asesoramiento sin elogios automáticos. Sin nuevas dependencias, cambios en contratos públicos existentes, repositorios ni modelos de negocio.
- Pruebas previas adaptadas solo en lectura de contenido/literales/filas: TextoConsola retira bordes y reúne líneas ajustadas para las regresiones funcionales; las 32 pruebas nuevas examinan salida original (23 del componente y 9 de flujos). No se eliminan casos ni comprobaciones de atributos, eventos, resultados o dados. Ahora la vida inicial aparece tanto en encuentro como en estado de acción, por lo que se ajustan los recuentos de esas apariciones de 2 a 4 en pruebas de dos aventuras.
- Verificación real: mvn clean test, BUILD SUCCESS; 723 pruebas, 0 fallos/errores/omitidas; 46 fuentes principales y 49 archivos de pruebas. main configura UTF-8 de forma explícita; prueba con salida original ASCII y ejecución real del menú en terminal correctas. No se ha abierto IntelliJ durante esta verificación. Sin commit ni push de la interfaz.

### 3.19 Pausas narrativas configurables

- RitmoConsola centraliza una espera secuencial y cuatro velocidades: instantánea 0, rápida 250, normal 700 y lenta 1200 ms. No hay animación por caracteres, hilos, temporizadores ni un sistema de eventos nuevo.
- main inicia Normal. El menú principal añade 2. Velocidad de narración: 1 instantánea, 2 rápida, 3 normal, 4 lenta, 0 vuelve sin cambiar. La elección se conserva en un único RitmoConsola durante toda la sesión, incluidas aventuras nuevas; sin persistencia.
- Las entradas existentes de app con flujos/dados proporcionados y el constructor anterior de Combate conservan ritmo instantáneo. Las nuevas sobrecargas permiten proporcionar el ritmo. El motor automático y ataque sin salida no esperan, incluso si se proporciona Normal. No se detecta un entorno de pruebas ni se depende de ajustes del desarrollador.
- Pausas después del panel de encuentro y anuncio de turno/orden; antes del resultado de un ataque enemigo; después de cada ataque (también fallo/contraataque); tras anunciar victoria, derrota o muerte simultánea; tras presentar oro, cada poción realmente encontrada y cada oferta de arma. Las pausas se sitúan fuera del dibujo de paneles y no modifican el orden de los eventos o la resolución del combate.
- No se esperan menús, entradas, cancelaciones, validaciones, inventario, comparación, tienda o cambios de velocidad. Las repeticiones de opciones inválidas no repiten pausas de los hitos ya mostrados.
- El componente vacía la salida antes de esperar. InterruptedException restaura la bandera y muestra un aviso; mientras siga interrumpido no vuelve a esperar. La Espera sustituible permite comprobar llamadas sin dormir.
- AGENTS.md establece el estándar de ritmo futuro. Se conservan los cambios visuales y las 723 pruebas anteriores; el único inválido principal 2 de AventuraTest se sustituye por 9 sin cambiar aserciones. 28 casos nuevos (9 de componente y 19 de integración) comprueban configuración, sesión, puntos de pausa, paneles, instantáneo/automático e interrupción sin esperas reales.
- Verificación: mvn clean test, BUILD SUCCESS; 751 pruebas, 0 fallos/errores/omitidas; 47 fuentes principales y 51 archivos de pruebas. Sin commit ni push.

### 3.20 Venta sencilla de objetos

- Tienda: 1 comprar, 2 vender, 0 exploración. Las pantallas de compra/venta vuelven a tienda con 0; cancelar la confirmación también vuelve sin cambios. EOF conserva el comportamiento de salida. Saldo visible en todas las pantallas y confirmaciones, leído de Jugador, sin copia independiente ni pausas.
- ProductoTienda.precioVenta centraliza precio / 2 entero. Precios de compra intactos; venta de pequeña 2, grande 5, desgastada 6 y hierro 10. Un repositorio alternativo determina sus propios precios mediante la misma fórmula.
- Jugador.productoVenta busca una correspondencia única y valida listado/búsqueda e IDs. Pociones: definición completa configurada. Armas: instancia de Arma, ranura ARMA, nombre/bonificaciones de la definición y sin habilidades adicionales. No hay IDs en las armas actuales: objetos desconocidos o correspondencias ambiguas no se valoran ni venden.
- Jugador.venderPocion retira una unidad sin curar. venderEquipo comprueba identidad y ubicación: guardada o equipada, nunca ambas. Las equipadas requieren confirmación explícita en consola y se retiran mediante desequiparSeguro; no hay arma obligatoria ni prohibición normal de desequipar. Las referencias duplicadas manualmente al mismo objeto se rechazan para evitar monetizar dos veces un objeto físico; las piezas independientes repetidas siguen admitidas.
- El saldo final se calcula con Math.addExact antes de retirar; se acredita solo tras éxito. Combatiente reutiliza la recuperación de cambiarEquipoSeguro para desequipar sin reemplazo, sin cambiar firmas previas. Fallos parciales recuperan atributos, vida, ranuras, mapa/identidad de habilidades y estados temporales. Restricciones previas de retirada de habilidades se conservan.
- 42 pruebas nuevas: VentaObjetosTest 28 y VentaConsolaTest 14. Se adaptan únicamente entradas de dos pruebas de consola para entrar/salir de comprar, sin eliminar aserciones ni casos. Se conserva combate, exploración, catálogos/repositorios, botín, ofertas y precios de compra.
- Verificación real: mvn clean test, BUILD SUCCESS; 793 pruebas, 0 fallos/errores/omitidas; 47 fuentes principales y 53 archivos de pruebas. Sin commit ni push.

### 3.21 Puntos de integración de repositorios externos

- ConfiguracionRepositorios centraliza enemigos(), objetos() y tienda(armas, pociones). Todas las entradas predeterminadas de app usan ese punto; las sobrecargas con repositorios proporcionados se conservan. Memoria sigue siendo el valor predeterminado, sin acceso a datos externo, dependencias, contenedor ni persistencia. La preparación de estos puntos está autorizada; implementar tecnologías externas sigue fuera de alcance.
- CatalogoTiendaMemoria separa las cuatro ofertas/precios de la construcción del repositorio. RepositorioTiendaMemoria conserva constructores y añade uno con productos/armas/pociones, sin exigir cuatro productos. La fórmula de venta no cambia.
- RepositorioTienda.validarProductos es una utilidad estática compartida; no añade métodos abstractos. Rechaza nulos, duplicados, referencias inexistentes o definiciones distintas para el mismo ID. app la aplica al iniciar también con repositorios externos y comprueba listado/búsqueda. Una definición equivalente reconstruida es válida, sin exigir identidad Java.
- Enemigos/objetos se cargan una vez por aventura. RuntimeException de lectura rechaza el inicio con el aviso existente, sin consumir entrada ni dados. La configuración de pociones del jugador se aplica después de validar también la tienda. Una tienda vacía es válida; enemigos vacíos u objetos incompletos impiden iniciar. Repositorios comerciales deben mantener listado/búsqueda coherentes durante la aventura.
- Nuevos enemigos y armas compatibles no requieren tocar combate/inventario/tienda. Permanecen los límites: dos tipos de poción y una definición por tipo; armas solo ARMA, sin habilidades ni VIDA en las definiciones; dos espadas de recompensa obligatorias por ID; venta de armas por correspondencia única de contenido. No se amplían mecánicas ni recompensas.
- GUIA_ACCESO_DATOS.md documenta métodos, modelos, punto de selección, ejemplo mínimo, validaciones y limitaciones. No resuelve lectura de archivos, JDBC, JPA ni otras tecnologías.
- Verificación: mvn clean test, BUILD SUCCESS; 818 pruebas, 0 fallos/errores/omitidas; 49 fuentes principales y 55 archivos de pruebas. Se conservan las 793 anteriores sin cambios y se añaden 25 (12 de repositorios alternativos y 13 de integración/validación de catálogos). Sin cambios de interfaz, pausas o reglas. Sin commit ni push.

### 3.22 Experiencia y niveles

- Jugador comienza en nivel 1, con 0 EXP y 0 puntos pendientes. agregarExperiencia(int) acepta cantidades positivas, descuenta 20/30/40/... EXP para alcanzar niveles 2/3/4/..., conserva sobrantes y permite varias subidas en una recompensa. getExperiencia muestra sobrante hacia el siguiente nivel, no total histórico.
- Cada nivel añade +1 permanente a FUERZA, RESISTENCIA, AGILIDAD, INICIATIVA y VELOCIDAD mediante los atributos base existentes. La vida se gestiona aparte: máxima +6 y actual +6, limitada al nuevo máximo. No usa curarse ni dispara alCurarse. Equipo y limpieza temporal conservan sus aportaciones sin perder las mejoras permanentes.
- Cada subida añade 2 puntos pendientes. asignarPunto(Atributo) consume exactamente uno y aumenta base +1; permite repetir atributo, rechaza VIDA/null/sin puntos/atributo saturado sin cambios. Las operaciones de EXP prevalidan magnitudes para evitar cambios parciales ante desbordamiento.
- DefinicionEnemigo incorpora int experiencia positiva y crearEnemigo la transmite a Enemigo. Los constructores anteriores siguen disponibles con 5 EXP por compatibilidad. Catálogo inicial: Goblin 5, Esqueleto 6, Bandido 7, Lobo 10; ninguna estadística cambia. Repositorios externos pueden definir otras recompensas, sin modificar combate.
- app concede EXP junto al oro solo con ENEMIGO_MUERE definitivo, después de la limpieza del motor. No concede por derrota, muerte simultánea, huida ni interrupción. Combate automático permanece ajeno a todas las recompensas. Se mantienen oro, botín y ofertas existentes.
- PanelConsola anuncia cada nivel alcanzado, mejoras, vida y puntos disponibles; una pausa solo tras cerrar el anuncio. Se reparten todos los puntos acumulados antes de continuar, sin pausas ni opción 0 para escapar. Entradas inválidas no gastan; EOF interrumpe conservando pendientes en Jugador. Si se reutiliza un personaje con pendientes, se reparten antes de otro combate. Tras el reparto se muestra el resultado; inventario y exploración muestran nivel/EXP.
- Pruebas: ProgresionJugadorTest 34 y ProgresionAventuraTest 15. Se conservan los 818 casos previos; solo se adapta la expectativa de EXP del catálogo, conservando todas las estadísticas/aserciones anteriores. Verificación: mvn clean test, BUILD SUCCESS; 867 pruebas, 0 fallos/errores/omitidas; 49 fuentes principales y 57 archivos de pruebas. Sin commit ni push.

### 3.23 Enemigos por nivel y encuentros progresivos

- DefinicionEnemigo incorpora int nivel como último campo, después de experiencia, y valida >= 1. crearEnemigo transmite ambos datos a Enemigo, que expone getNivel. Los constructores anteriores siguen disponibles con nivel 1; no se generan atributos a partir del nivel.
- Los cuatro iniciales conservan todas sus estadísticas y EXP y son nivel 1. Nuevos: goblin_guerrero/Goblin guerrero, nivel 3, fuerza 5, resistencia 2, iniciativa 3, agilidad 2, velocidad 3, vida 20, EXP 15; esqueleto_veterano/Esqueleto veterano, nivel 4, fuerza 6, resistencia 3, iniciativa 2, agilidad 2, velocidad 2, vida 24, EXP 20; bandido_elite/Bandido de élite, nivel 5, fuerza 7, resistencia 3, iniciativa 3, agilidad 2, velocidad 3, vida 28, EXP 25.
- app filtra el catálogo cargado una vez por aventura en cada encuentro: enemigo.nivel <= jugador.getNivel. Selección uniforme entre elegibles, manteniendo los inferiores y el orden del repositorio; sin condiciones por ID, pesos, zonas o mapas. Con memoria: niveles 1–2 cuatro elegibles, 3 cinco, 4 seis, 5+ siete.
- Catálogo vacío conserva el rechazo anterior. Sin elegibles se informa y vuelve al inicio, sin combate, tiradas, consumo de acciones ni recompensas. Un repositorio externo solo necesita devolver sus definiciones con nivel; ConfiguracionRepositorios e interfaces siguen intactos. El panel de encuentro añade nombre y nivel, sin cambiar pausas.
- Se conservan fórmulas, recompensas anteriores, objetos y progresión del jugador. GUIA_ACCESO_DATOS documenta campo, constructor, validación y filtro por nivel.
- Nuevas pruebas: EncuentrosPorNivelTest 16, NivelesEnemigosTest 12 y EquilibrioVariantesTest 3. Las 867 anteriores se conservan adaptando catálogo ampliado, filtro inicial y jugador al nivel de cada enemigo en la prueba automática. Simulación histórica inicial intacta en sus resultados. Variantes: 18.000 combates con semillas fijas, nivel mínimo, vida completa, repartos fuerza/resistencia/velocidad y arma +0/+2, sin pociones: 100 % victorias en la muestra, medias 2,128–3,222 turnos. No representa empezar herido.
- Verificación: mvn clean test, BUILD SUCCESS; 898 pruebas, 0 fallos/errores/omitidas; 49 fuentes principales y 60 archivos de pruebas. Sin commit ni push.

### 3.24 Equipamiento general, primera fase

- Equipo pasa a ser la clase común concreta y conserva Arma como subclase compatible con sus habilidades. TipoEquipo añade AMULETO al final sin cambiar el orden anterior. Siete ranuras, una pieza por cada una, incluido un solo anillo; inventario ilimitado, sin restricciones de nivel/clase/atributos.
- DefinicionEquipo(id, nombre, tipo, bonificaciones) inmutable crea ejemplares independientes. Cada pieza de catálogo conserva getIdDefinicion; identidad del ejemplar por referencia y selección por índice, sin equals basado en nombre/estadísticas. DefinicionArma conserva constructor/crearArma y añade comoEquipo; las armas creadas guardan ahora ID. Los constructores manuales antiguos sin ID se conservan.
- RepositorioObjetos añade obtenerEquipamiento default vacío para definiciones adicionales sin repetir armas. Las implementaciones anteriores siguen siendo válidas. La aventura lee las tres listas una vez. ConfiguracionRepositorios.tienda añade sobrecarga con las adicionales; ProductoTienda añade equipo() general y conserva sus constructores anteriores y arma() para DefinicionArma. Validación cruza IDs y contenido entre armas, piezas y pociones.
- VIDA se admite en cualquier pieza como bonificación al máximo. Combatiente conserva vidaMaxima permanente y getVidaMaximaBase; getVidaMaxima suma el modificador VIDA del equipo, con mínimo cero. Equipar no cura ni dispara alCurarse; retirar ajusta vida actual al máximo final si es necesario. Curarse consulta el máximo efectivo. La progresión mantiene +6 a la base y +6 a actual, sin absorber bonos de equipo; no cambian umbrales ni puntos. Un intercambio conserva la vida hasta el máximo final, evitando recortes por el máximo transitorio.
- Aplicación/retirada de los otros atributos y habilidades mantiene el sistema anterior. Los intercambios, desequipado y ventas reutilizan snapshots que restauran ranuras, inventario, atributos/base, modificadores VIDA, vida y estados ante fallos.
- Nuevas definiciones en memoria: armadura_cuero/Armadura de cuero (ARMADURA, RESISTENCIA +1, 8 monedas); casco_cuero/Casco de cuero (CASCO, RESISTENCIA +1, 8); guantes_cuero/Guantes de cuero (GUANTES, FUERZA +1, 10); botas_ligeras/Botas ligeras (BOTAS, AGILIDAD +1, 8); anillo_sencillo/Anillo sencillo (ANILLO, INICIATIVA +1, 10); amuleto_sencillo/Amuleto sencillo (AMULETO, VIDA máxima +2, 10). Diez productos con los cuatro anteriores intactos. Venta precio / 2 entero, desde guardado o equipado. Sin nuevos botines/recompensas.
- Comercio identifica por ID más contenido; compatibilidad estructural única solo para piezas antiguas sin ID. Dos modelos con mismo nombre/bonos y diferentes IDs ya no son ambiguos. Las referencias duplicadas manualmente conservan la protección de venta anterior; los ejemplares independientes se seleccionan y venden individualmente.
- Inventario muestra ranuras vacías/ocupadas y bonificaciones de Vida máxima. Opciones 1 comparar, 2 cambiar, 3 desequipar, 0 volver. Desequipar pide confirmar, conserva la referencia y la devuelve al final del inventario. Cancelación/EOF no confirman. Sin pausas dentro de menús.
- Pruebas añadidas: EquipoGeneralTest 25, CatalogoEquipoGeneralTest 17 y EquipoGeneralConsolaTest 21. Base local comprobada: 899 casos (la documentación anterior registraba 898); se conservan, adaptando solo las reglas estructurales de VIDA, tienda de diez productos, ranuras vacías, el inválido 3 a 4 y una pieza antigua sin ID en la regresión de venta ambigua.
- Verificación: mvn clean test, BUILD SUCCESS; 962 pruebas, 0 fallos/errores/omitidas; 50 fuentes principales y 63 archivos de pruebas. README y GUIA_ACCESO_DATOS actualizados. Sin cambios en fórmulas, enemigos, recompensas ni sistema de habilidades/pausas; sin persistencia. Pendientes pergaminos, mejoras individuales/aleatorias, rarezas y habilidades desde datos. Sin commit ni push.

### 3.25 Pergaminos y mejoras individuales

- Todos los ejemplares nuevos de Equipo/Arma generan una única nextInt(4), uniformemente 0–3 puntos disponibles. Generador predeterminado central en Equipo, sin reinicializarlo por pieza; constructores anteriores conservados. DefinicionEquipo/DefinicionArma añaden sobrecargas de creación con int o Random; Jugador.comprar añade Random para controlar pruebas. Las definiciones permanecen inmutables y sin puntos. Consultar, equipar, desequipar, comparar o vender no vuelve a generar.
- Equipo conserva bonos originales, adicionales y totales con vistas externas protegidas, puntos restantes y contador de consumidos. La mejora controlada añade +1 y resta un punto; máximo tres en cada ejemplar según su tirada inicial. No comparte estado con otra pieza de la misma definición.
- Pergamino es un record interno con atributo y cantidad obligatoriamente 1; nombre calculado (vida máxima para VIDA). Los seis atributos son elegibles. Jugador almacena cantidades ilimitadas por atributo, separadas de pociones/equipo; no hay catálogo, compra ni venta de pergaminos. Nueva aventura crea inventario vacío.
- Una comprobación del 10 % por victoria definitiva y, solo si tiene éxito, nextInt(6) uniforme para un atributo; máximo una unidad por victoria. Generador independiente de pociones, encuentros y dados. La sobrecarga completa de ejecutar acepta Random azarPergaminos y lo conserva entre encuentros y aventuras; las firmas anteriores delegan con un generador real. No se consulta por derrota, muerte simultánea, huida o combate incompleto.
- La comprobación de pergamino se hace después del panel de oro/EXP y antes de cualquier reparto de nivel, para no omitirla si se cierra la entrada tras vencer. Mensaje y pausa solo cuando se obtiene. Se mantienen las dos tiradas de pociones y sus resultados, el oro, experiencia y ofertas de espadas.
- Jugador.usarPergamino valida propiedad por identidad/ubicación, cantidad y puntos. Combatiente reutiliza sus snapshots para incrementar el modificador activo sin retirar/reaplicar habilidades. Equipo valida magnitudes, actualiza y comprueba el portador antes de confirmar sus mapas/puntos; después solo se descuenta el pergamino en la colección privada. Fallos o rechazos conservan el estado y no consumen. No hay hilos ni transacciones nuevas.
- VIDA añade solo +1 al máximo efectivo, sin curar. Retirar equipo descuenta su total mejorado y ajusta actual si supera el máximo. Cambios, ciclos y +6 PS permanentes por nivel conservan las reglas de 3.24. Las habilidades de armas mantienen contribuciones, niveles y estados.
- Comercio reconoce ID, nombre, ranura y bonos originales; los adicionales no aumentan precio ni impiden vender. Mitad entera del precio original, guardada o equipada; en equipada se retira el total. Otros ejemplares del mismo modelo no se modifican.
- Inventario muestra pergaminos/cantidades y mejoras disponibles de cada pieza. Añade 4 utilizar pergaminos sin renumerar 0–3. Elegir pergamino, pieza guardada/equipada, vista previa de bono y puntos antes/después, confirmar o cancelar. 0 retrocede un nivel; éxito muestra resultado y vuelve a acciones de inventario; inválidos/EOF no consumen. Sin pausas dentro de menús.
- Pruebas nuevas: MejorasPergaminosTest 49, BotinPergaminosTest 17 y PergaminosConsolaTest 18. Los 962 casos previos se mantienen; cuatro archivos de consola se adaptan a generador de pergaminos controlado, inválido 4 a 5 y normalización exclusiva del nuevo contador aleatorio en la comparación de texto instantáneo/normal. No se eliminan comprobaciones anteriores de reglas/resultados.
- Verificación: mvn clean test, BUILD SUCCESS; 1046 pruebas, 0 fallos/errores/omitidas; 51 fuentes principales y 66 archivos de pruebas. README y GUIA_ACCESO_DATOS actualizados. Sin contratos nuevos de repositorios, persistencia, habilidades, categorías o rarezas. Sin cambios de enemigos, combate, experiencia o precios. Sin commit ni push.

### 3.26 Pergaminos de habilidades y maestría

- PergaminoHabilidad fija una HabilidadDisponible al obtenerse; Jugador acumula cantidades por contenido y un contador independiente de maestría. Sin inventario inicial, compra/venta, caducidad ni repositorios nuevos.
- Catálogo estable de 13 habilidades jugables en HabilidadDisponible, sin duplicados de clase y sin depender del portador. Excluye FuriaMaximaBerserker porque es un borrador sin eventos; conserva PrimerImpulso(3) y SegundoAliento(2), sin modificar fórmulas ni magnitudes internas.
- Por victoria: 10 % atributo (intacto), 5 % habilidad con selección uniforme y 2 % maestría, independientes y máximo una unidad por tipo. Ocho combinaciones posibles; no hay comprobaciones por derrota, muerte simultánea, huida o entrada finalizada antes de victoria. Se entregan antes del reparto de puntos de nivel, que puede terminar por EOF.
- Las nuevas tiradas usan Random azarHabilidades, separado de atributos/pociones/encuentros/dados. Sobrecargas completas de app lo reciben después de azarPergaminos; las anteriores se conservan. Un mismo generador durante encuentros y aventuras. Tiradas: nextInt(100) < 5, nextInt(13) solo en éxito y otro nextInt(100) < 2.
- EstadoHabilidad añade nivelPermanente; getNivel sigue siendo el total y getNivelEquipo su aportación de equipo. Aprendizaje y contribuciones suman con comprobaciones de desbordamiento. Constructor anterior conservado (todo permanente si aprendida), nuevo constructor explícito para reparto mixto y copia/restauración completa, incluidos indicadores temporales. Incorporar habilidades normalmente mediante Combatiente, no manipulando su mapa.
- Casos: desconocida -> permanente 1; aprendida 2 -> 3; solo equipo 2 -> permanente 1/efectivo 3; aprendida 2 + equipo 2 -> permanente 3/efectivo 5. Retirar equipo conserva el nivel permanente. No duplica entradas ni reinicia efectos temporales activos.
- Consumo encapsulado en Jugador, tras aprenderHabilidadSeguro de Combatiente. Reutiliza la recuperación del intercambio; fallos/desbordamientos preservan cantidades, niveles, instancias y datos temporales. No modifica atributos, vida, EXP, equipo ni eventos de combate. Las habilidades con magnitudes internas fijas mantienen su comportamiento.
- Inventario conserva opciones 0–4 y añade 5 habilidad y 6 maestría. Consulta de cantidades, elección de contenido fijo o habilidad libre, confirmación con niveles permanente/equipo/efectivo y resultado. Cancelar, inválidos y EOF no consumen; 0 retrocede un nivel. PanelConsola, sin pausas en menús.
- Verificación final: mvn clean test, BUILD SUCCESS; 1146 pruebas, 0 fallos/errores/omitidas; 53 fuentes principales y 69 archivos de pruebas. Se conservan los 1046 casos anteriores y se añaden 100: PergaminosHabilidadesTest 59, BotinHabilidadesTest 21 y PergaminosHabilidadesConsolaTest 20. Cinco archivos anteriores adaptan solo la inyección del nuevo azar o la opción inválida 5 a 7; no se debilitan aserciones. Sin cambios de combate, repositorios, precios, oro/EXP o fórmulas de habilidades. Sin commit ni push.

### 3.27 Nombre y consulta de estado

- app.solicitarNombre solicita 1–30 puntos de código Unicode, recorta espacios exteriores y rechaza vacíos, controles y caracteres de formato/separación que alteren la consola. 0 cancela al menú inicial; EOF sale sin crear jugador ni consultar catálogos/dados. Se utiliza el nombre en el constructor existente de Jugador y se conserva entre encuentros.
- Exploración añade 6 Estado del personaje conservando 0–5 e inventario independiente. app.consultarEstado muestra identidad/descripción si existe, nivel/EXP/umbral, puntos pendientes, vida actual/máxima, oro, los seis atributos y dos secciones de habilidades. 0 vuelve; inválidos repiten el prompt y EOF finaliza sin modificar estado.
- Fuentes de atributos: getAtributoBase y getAtributo para los cinco atributos; getVidaMaximaBase y getVidaMaxima para VIDA. getModificadoresEquipo proporciona el bono neto, incluidas mejoras y penalizaciones. Total mínimo cero explícito cuando base + equipo es negativo. No se reconstruye la base restando bonos.
- Habilidades aprendidas: nivelPermanente, nivelEquipo y total desde EstadoHabilidad; las exclusivas de equipo aparecen aparte con piezas otorgantes obtenidas de las ranuras. Orden estable por nombre, una entrada por habilidad, sin disparar eventos ni modificar mapas, instancias, inventario o niveles.
- Limitación conservada: los efectos temporales actuales modifican atributosBase. La pantalla avisa si hay indicadores/contadores activos y presenta la base actual; no inventa un desglose permanente que el modelo no registra por separado ni limpia efectos para consultar.
- Sin cambios de firmas existentes, modelos, combate, dados, recompensas, repositorios o pausas. Las pruebas previas de ejecutar incorporan el nombre explícitamente sin eliminar sus aserciones. README actualizado. Sin commit ni push.
- Verificación: mvn clean test, BUILD SUCCESS; 1180 pruebas, 0 fallos/errores/omitidas; 53 fuentes principales y 71 archivos de pruebas. Se conservan las 1146 anteriores y se añaden 34: NombrePersonajeTest 22 y EstadoPersonajeTest 12. Doce archivos existentes adaptan solo las entradas de nombre. Sin esperas reales.

## 4. Habilidades

- `Habilidad` es una clase abstracta que implementa una interfaz de eventos (`EventosHabilidad`).
- Las habilidades se identifican en el mapa por su tipo concreto: `equals()`/`hashCode()` son finales y comparan la clase, de modo que varias piezas y aprendizaje comparten una sola entrada por portador.
- Cada `Combatiente` posee un `Map<Habilidad, EstadoHabilidad>`.
- `EstadoHabilidad` incluye `aprendida`, `vecesEquipada`, `nivel` efectivo, `nivelPermanente` e indicadores temporales independientes por portador.
- Una habilidad puede ser aprendida o concedida por el equipo. El nivel efectivo suma el permanente y las contribuciones de equipo; desde 3.26 se distingue expresamente el nivel permanente para mostrarlo y mejorarlo mediante pergaminos.
- Regla confirmada por el autor: una habilidad aprendida o equipada debe tener nivel estrictamente positivo. EstadoHabilidad valida nivel > 0 y contadores no negativos; `isActiva()` devuelve aprendida o vecesEquipada > 0. Los eventos de ataque y curación ignoran entradas inactivas. Aprender/otorgar/retirar niveles exige aportaciones positivas; la retirada de la última pieza elimina la entrada antes de asignar cero. El constructor vacío conserva su firma y crea un estado inactivo de nivel 1.
- Hay métodos para aprender habilidades y para sumar o retirar habilidades otorgadas por equipo (`aprenderHabilidad`, `agregarHabilidadEquipo`, `quitarHabilidadEquipo`). La retirada debe respetar que una habilidad aprendida pueda seguir existiendo aunque se retire el equipo que también la otorgaba.
- Los eventos de habilidad reciben el **estado de esa habilidad concreta**, además de los participantes pertinentes. Las firmas de todos los eventos y todas sus llamadas deben ser consistentes.
- Para recorrer las habilidades, se ha planteado `dispararHabilidades(Combatiente, BiConsumer<Habilidad, EstadoHabilidad>)`, iterando por `entrySet()` y pasando `entrada.getKey()` y `entrada.getValue()`.
- Los eventos conocidos incluyen `alInicioCombate`, `alMorir` y `alFinalCombate`; el repositorio puede contener otros. No inventar eventos ni su semántica.

## 5. Equipo, objetos y comparación

- Existe una clase abstracta `Equipo`, derivada de `Objeto`, que implementa `Equipable`.
- Cada equipo tiene un `TipoEquipo` y puede proporcionar `Map<Atributo,Integer> bonificaciones` y `Map<Habilidad,Integer> habilidadesOtorgadas`.
- `Equipable` define `aplicarEfectos(Combatiente)` y `retirarEfectos(Combatiente)`.
- Al equipar, deben aplicarse los modificadores de atributos y las habilidades del objeto. Al desequipar, deben retirarse **exactamente sus contribuciones**, sin borrar aumentos permanentes ni efectos de otras fuentes.
- Se ha previsto un `Map<TipoEquipo, Equipo> equipoEquipado`: **una pieza por ranura/tipo**. Para reemplazar una pieza, primero hay que gestionar la retirada de la anterior.
- El método de modificación de atributos existente se denomina `modificarAtributo(Atributo, int)`; una referencia anterior a `modificarAtributoEquipo` provocó un error de compilación. Inspeccionar nombres reales antes de llamar métodos.
- La comparación de dos equipos debe mostrar la **unión** de los atributos modificados por cualquiera de ellos, con columnas `ATRIBUTO | ACTUAL | NUEVO | DIFERENCIA`.
- `DIFERENCIA = bonificación nueva - bonificación actual`; valor positivo es una mejora relativa, negativo es empeoramiento.
- Cuando un atributo no figure en el mapa de una pieza, mostrar `--`; para calcular la diferencia, tratar su ausencia como `0`. Se acordó que los mapas no incluyen bonificaciones explícitas de valor cero.
- Comparar equipos **no modifica** los atributos ni equipa automáticamente el objeto nuevo.
- Las bonificaciones pueden ser positivas o negativas. Ejemplo: pasar de `RESISTENCIA -4` a ausencia de bonificación equivale a una diferencia de `+4`.
- La interfaz de comparación puede ser simple texto en consola; no introducir interfaz gráfica.

## 6. Exploración, objetos, comercio y progreso

Estas áreas forman parte del objetivo del producto, pero **no se han establecido aquí suficientes reglas detalladas para implementarlas completas**:

- **Encuentros y exploración:** el personaje podrá encontrar enemigos y objetos. Quedan pendientes el procedimiento de selección de encuentros, desplazamiento, zonas, eventos y recompensas concretas.
- **Inventario actual:** cantidades de las dos pociones según 3.7, encapsuladas en Jugador, consultables y consumibles entre combates según 3.8 y utilizables durante su acción según 3.9. Cualquier inventario general sigue pendiente. No se inventan límites de capacidad ni otros objetos.
- **Comercio:** compras según 3.17 y ventas según 3.20 implementadas. Descuentos y otras ampliaciones quedan fuera de alcance. No importar reglas de ejercicios independientes sobre Strategy y tipos de cliente.
- **Progresión:** experiencia, niveles y reparto confirmados e implementados según 3.22. No se añaden clases de personaje, aprendizaje automático de habilidades ni otras reglas de progresión.
- **Huida:** implementada para el jugador según 3.10, sin persecución ni huida enemiga. **Objetos en combate:** únicamente las dos pociones, según 3.9; otros objetos siguen pendientes.
- **Continuidad actual:** mismo jugador, oro y pertenencias; vida restante sin curación automática, enemigos nuevos del catálogo 3.11 y menús según 3.5/3.17. Espadas, botín y oro mantienen sus reglas. Muerte termina aventura; sin persistencia.

## 7. Estructura de código conocida (orientativa; verificar)

- `org.viajedragos.modelo.Combatiente`: clase abstracta; `Jugador` y `Enemigo` son subclases.
- `org.viajedragos.arma.Atributo`: enum de atributos.
- `org.viajedragos.arma.Equipo`: clase abstracta de equipo; `TipoEquipo` representa ranuras; `Equipable` declara aplicar/retirar efectos.
- `Objeto`: clase base de objetos; ubicación y API exactas por comprobar.
- `Habilidad`, `EstadoHabilidad`, `EventosHabilidad`: sistema de habilidades; paquetes exactos por comprobar.
- `Combate`: lógica de enfrentamiento; `ResultadoCombate`: resultados.
- `TiradorDado`: abstracción para tiradas.
- Puede existir `ContextoAtaque` y otras clases auxiliares. **Inventariarlas en el repositorio** antes de intervenir.

En `Combatiente` se ha trabajado con:

```java
private final Map<Atributo, Integer> atributosBase = new EnumMap<>(Atributo.class);
private final Map<Atributo, Integer> modificadoresEquipo = new EnumMap<>(Atributo.class);

public int getAtributo(Atributo atributo) {
    int base = atributosBase.get(atributo);
    int modificador = modificadoresEquipo.get(atributo);
    return Math.max(0, base + modificador);
}

public void modificarAtributo(Atributo atributo, int cantidad) {
    modificadoresEquipo.merge(atributo, cantidad, Integer::sum);
}
```

También se han trabajado `aumentarAtributo(Atributo,int)` y `disminuirAtributo(Atributo,int)` para modificar permanentemente el atributo base. **Buscar referencias antiguas** como `aumentarFuerza()` antes de modificar o eliminar métodos: hubo roturas de llamadas al refactorizar.

## 8. Estado de implementación: checklist auditada

> **Estado:** 19 de octubre de 2026. Base local 1846613; pergaminos de habilidades/maestría y nombre/estado en cambios locales según 3.26–3.27. Codex no ha realizado commit ni push.
> **Leyenda:** `[x]` acredita únicamente la acción o existencia descrita en esa fila; no implica que el juego completo funcione. `[ ]` indica trabajo pendiente. Las revisiones realizadas se distinguen de sus correcciones pendientes.
> **Evidencia actual (19 de octubre de 2026):** `mvn clean test`: BUILD SUCCESS; 53 fuentes principales y 71 archivos de pruebas; 1180 pruebas, 0 fallos, 0 errores, 0 omitidas. Se conservan los 1146 casos anteriores y se añaden 34 según 3.27. Sin cambios de fórmulas, enemigos o recompensas anteriores ni acceso a datos externo.

### 8.1 Bases presentes o desarrolladas parcialmente

- [x] Proyecto Java de consola y modelo `Combatiente` / `Jugador` / `Enemigo`, compilables con Java 23.
- [x] Enum `Atributo`, mapas separados de base/equipo y getters/setters; VIDA se gestiona por campos y no por los mapas.
- [x] Métodos `aumentarAtributo`, `disminuirAtributo` y `modificarAtributo`; las habilidades corregidas leen la base, no valores efectivos. Los métodos genéricos rechazan VIDA explícitamente.
- [x] `TiradorDado`, `Dado`, `ContextoAtaque` y lógica inicial de iniciativa.
- [x] `Combate` y `ResultadoCombate`, con JUGADOR_HUYE integrado según 3.10. ENEMIGO_HUYE conserva su declaración sin mecánica implementada.
- [x] Resolución de muertes y final integrados en inicio/turnos/ataques; una invocación de muerte por participante y resolución, continuación tras resurrección y limpieza final solo del jugador.
- [x] `Habilidad` / `EstadoHabilidad` y mapa por combatiente; las 13 habilidades con efectos heredan ahora de Habilidad y su igualdad por clase. FuriaMaximaBerserker sigue siendo un borrador sin efectos.
- [x] Métodos para aprender y conceder/retirar habilidades de equipo; niveles y contadores probados con varias piezas y aprendizaje permanente. La retirada inválida produce un error antes de descontar efectos.
- [x] Contratos y clases de equipo, bonificaciones y habilidades otorgadas.
- [x] Comparador de equipo ejecutado con atributos distintos y una bonificación negativa.
- [x] Suite JUnit: 1180 pruebas ejecutadas sin fallos ni errores; 1146 casos anteriores y 34 de nombre/estado, sin pruebas eliminadas ni deshabilitadas ni esperas reales.

### 8.2 Auditoría completada y correcciones/integración pendientes

- [x] Auditar clases, paquetes, firmas, dependencias y referencias del repositorio.
- [x] Comprobar compilación del código principal: `mvn compile` termina correctamente.
- [x] Ejecutar las pruebas existentes con `mvn test`.
- [x] Conseguir compilación de las pruebas y suite pasando: `mvn clean test`, 751 pruebas pasan. Se conservan las adaptaciones de firmas del primer bloque.
- [x] Revisar `EventosHabilidad`: todas sus firmas actuales y overrides implementados reciben `EstadoHabilidad` y compilan en el código principal.
- [x] Revisar y corregir las seis llamadas del borrador a `dispararHabilidades`: las lambdas reciben habilidad y estado y pasan ese estado al evento.
- [x] Activar el auxiliar existente `dispararHabilidades` para usarlo desde ataque, recorriendo habilidad y estado juntos.
- [x] Completar etapas A/B/C: combatir, ordenHabilidades, primerTurno iterativo, eventosTurno, resolverMuertes, personajeMuere y finalDelCombate. El ataque individual conserva sus fórmulas y eventos.
- [x] Probar enfrentamiento completo: victoria, derrota, muerte simultánea, resurrección, alternancia, prioridad y restauración.
- [x] Corregir la selección del segundo participante en el borrador: `determinarOponente(primero)`.
- [x] Completar el bucle de turnos y eliminar todos los retornos ficticios/provisionales. CONTINUA es un estado interno para continuar, no el resultado público de un enfrentamiento terminado.
- [x] Aplicar el daño de ataque sin habilidades: copiar bruto a final y restar vida una vez si no falla. Conservar las reglas existentes de fallo, defensa fallida, crítico 6/1 y contraataque 1/6. Probadas las 36 combinaciones de dados y seis valores del dado crítico extra.
- [x] Integrar eventos del ataque según la secuencia confirmada, atacante antes que defensor, con el EstadoHabilidad propio de cada entrada. Probar daño final aplicado una vez, efectos posteriores solo con daño efectivo y contraataque directo sin segunda secuencia.
- [x] Integrar atacante y ataque existentes con el bucle, sin alterar sus fórmulas. Tras la aclaración de consola, atacante establece el orden de dos acciones; comprobar muertes entre ellas y alternar poseedor al terminar el turno.
- [x] Revisar por lectura `resolverMuertes` / `personajeMuere`: algunas ramas omiten los efectos del segundo muerto; no hay validación ejecutada de esa lógica comentada.
- [x] Corregir resolución de muertes y probarla en inicio y ataques: estado final reevaluado, una activación por participante/resolución y nuevas resoluciones para muertes posteriores a una resurrección.
- [x] Revisar `finalDelCombate`: el borrador solo dispara eventos para el jugador.
- [x] Confirmar participación de ambos en las fases de habilidades, prioridad por iniciativa actual con poseedor del turno como respaldo, y transición a alMorir si se muere antes de ejecutar el inicio.
- [x] Confirmar y aplicar cierre inmediato tras muerte definitiva: no hay alFinalTurno posterior; alFinalCombate solo del jugador, vivo o muerto, como limpieza. PrimerImpulso solo resta su aumento si lo aplicó.
- [x] Conectar y probar eventos de turnos y finalización. Ambos ejecutan eventos de turno mientras viven; las fases respetan prioridad y comprobaciones de muerte.
- [x] Integrar las 13 habilidades con efectos en el tipo `Habilidad`, conservando los constructores públicos y los efectos existentes.
- [ ] Unificar el uso del nivel donde corresponda: solo Caparazon lee `estado.getNivel()`; varias habilidades usan campos propios.
- [x] Corregir `Combatiente.setHabilidades` para asignar el parámetro recibido; no se ha añadido una prueba específica de este setter.
- [ ] Definir el estado inicial de `agregarHabilidad` y corregirlo: todavía no añade entradas ausentes.
- [x] Proteger la retirada de habilidades sin contribución o con niveles inválidos; comprobar contadores/niveles y rechazo explícito de VIDA en los métodos genéricos.
- [x] Corregir operaciones de atributos en Impetu, InstintoSupervivencia, PrimerImpulso, FuriaContenida y Rabia, manteniendo el sistema de sumar/restar autorizado por el autor. Rabia retira sus tres cambios al final del combate.
- [ ] Si se requieren bonificaciones de equipo a vida o vida máxima, definir su regla antes de admitir VIDA en equipos; por ahora se rechaza sin modificar la vida.
- [ ] Completar `FuriaMaximaBerserker`, que solo contiene un campo.
- [x] Completar y probar `Equipo.aplicarEfectos` y `retirarEfectos` para atributos y habilidades. Son operaciones de efectos emparejadas; las ranuras se gestionan desde Combatiente.
- [x] Completar `equipar(Equipo)` / `desequipar(TipoEquipo)`, con resultado booleano. Mantener `setEquipo(Equipo)` como entrada compatible. Una ranura ocupada se rechaza sin efectos; sustituir requiere desequipar antes.
- [x] Comprobar `equipoEquipado`: inicializado con EnumMap en la declaración.
- [x] Probar retirada exacta de contribuciones positivas/negativas, atributos efectivos limitados a cero, cambios permanentes con equipo activo y habilidades compartidas entre piezas o aprendidas.
- [x] Ejecutar el comparador: FUERZA +3, AGILIDAD +3, INICIATIVA -2 y RESISTENCIA -7 para las armas actuales.
- [x] El constructor de Equipo copia ambos mapas y sus getters devuelven mapas de solo lectura; proteger también los mapas de ranuras y modificadores de Combatiente. No se encontraron llamadas existentes que necesiten escribir mediante esos getters.
- [x] Confirmar el +2 del turno: solo suma al cálculo de iniciativa + 1D6 del poseedor; no modifica atributos ni agilidad.
- [x] Confirmar y aplicar excepción de iniciativas cero, repetición iterativa y prueba de 10.000 empates sin acumular pila.
- [x] Crear interfaz de consola: app.main presenta el juego, inicia aventura, muestra vidas y resultado, y permite continuar con el mismo personaje, terminar aventura o salir. Las entradas inválidas repiten la pregunta sin consumir dados ni acciones. Sin System.exit.
- [x] Integrar Atacar después de determinar el orden, únicamente cuando llega la acción del jugador. El ataque previo del enemigo se resuelve antes de preguntar; una muerte definitiva impide la acción pendiente y una resurrección permite continuar.
- [x] Conservar combatir() automático y añadir combatir(Scanner, PrintStream) con un bucle compartido. La narración muestra atacante, objetivo, fallo, crítico, contraataque y vida realmente perdida.
- [x] Probar consola con dados controlados y flujos en memoria: inicio, decisión, entradas inválidas, victoria/derrota, mensaje de muerte simultánea, resurrección, reinicio, salida, cierre de entrada y equivalencia de ambos modos.
- [x] Auditar las 14 clases de habilidades para la demo sobre main publicado 072758f: 13 con efectos y un borrador. Análisis en ANALISIS_HABILIDADES_DEMO.md; mvn clean test confirma 224 pruebas superadas, sin modificaciones de código.
- [x] Confirmar partida inicial de aprendizaje: Aventurero y primer Lobo sin habilidades ni equipo iniciales. La propuesta de asignar habilidades para demostración está descartada. La fase 5 solo añade la espada sin habilidades después de vencer al primer Lobo; sin progresión ni experiencia.
- [x] Validar habilidades existentes exclusivamente con combatientes de prueba: 21 casos nuevos de eventos, atributos usados en cálculos, curación limitada, restauración del jugador vivo/muerto, estado/nivel, equipo y equivalencia de modos.
- [x] Tras reproducir el defecto en 10 casos, convertir RolesModificadoresDanioTest en 41 regresiones de roles confirmados, condiciones originales, mismo tipo/instancia en ambos participantes, daño cero, crítico, fallo y contraataque; documentado en ANALISIS_ROLES_MODIFICADORES_DANIO.md.
- [ ] Narración de habilidades aplazada expresamente por el autor; no añadir mensajes ni cambiar la interfaz en esta tarea.
- [x] Confirmar y corregir los cinco roles: Caparazón/Defensa perfecta/Guardia férrea solo defensor; Castigo al torpe/Golpe certero solo atacante. Guarda por identidad del estado asociado al propietario; sin cambiar eventos, fórmulas, niveles, equipo ni otras habilidades.
- [x] Catálogo Java de cuatro enemigos en memoria mediante contrato sustituible, definiciones inmutables, validación e instancias independientes.
- [x] Integrar selección uniforme desde el primer encuentro y generador independiente, sin Factory ni persistencia. Mantener huida, botín, espada única e inventario.
- [x] Ajustar únicamente la vida inicial de consola 24 a 48 PS tras simulación del motor real: 94,02 % de cuatro victorias en 10.000 sesiones, sin consumibles y con espada desde el segundo.
- [x] Catálogo de objetos autorizado: espada y dos pociones, records inmutables y repositorio sustituible; valores efectivos por jugador y validación antes del primer encuentro según 3.12.
- [x] Verificar compatibilidad con espada única, equipo, botín, curación, ambos menús, eventos y continuidad sin modificar las 452 pruebas anteriores.
- [x] Inventario de equipamiento ilimitado y encapsulado, duplicados/instancias conservados y separado de pociones/ranuras según 3.13.
- [x] Comparar desde inventario por ranura, reutilizando compararCon y manteniendo el personaje intacto; ANSI opcional limitado a diferencias, navegación y EOF probados.
- [x] Obtener hierro tras segunda victoria, guardar/rechazar, equipar e intercambiar con recuperación segura, según autorización 3.14.
- [x] Corregir el efecto temporal persistente al perder la última fuente de una habilidad; conservar aprendidas/compartidas y recuperar indicadores internos tras fallos según 3.15.
- [x] Aislar el estado temporal de las cinco habilidades entre combatientes mediante EstadoHabilidad, conservando claves, referencias y disparo secuencial; 23 regresiones según 3.16.
- [ ] Crear catálogo de habilidades cuando se solicite; no se añade en esta fase.
- [x] Implementar Atacar desde el menú en su momento real de acción.
- [x] Implementar huida directa con dos dados y velocidad efectiva, empate fallido, sin ataque por intento. Éxito limpia y vuelve a exploración; fallo consume acción y conserva la acción enemiga pendiente.
- [x] Conservar personaje/posesiones tras escapar, sin botín ni espada por huida. Ofrecer espada una vez tras primera victoria real, aunque antes se haya huido.
- [x] Añadir 24 casos de huida/continuidad, con 36 parejas de dados, velocidad/equipo/efectos activos, turnos, limpieza, muerte/resurrección, otros Combatiente, inválidos y cierre de entrada.
- [x] Implementar las dos pociones en combate según 3.9, sustituyendo al ataque solo tras éxito. Sin perder acción por cancelar/inválidos/rechazo ni modificar iniciativa, eventos de turno o resolución de muertes.
- [x] Conservar Combatiente no Jugador con su selección de Atacar y modo automático sin consumo de pociones. Sin conversiones forzadas, nuevas clases ni contratos públicos.
- [x] Añadir 29 casos de consumo en combate, eventos/estados/habilidades reales, muerte/resurrección, recuperación real, dados exactos, interrupción y continuidad con exploración/botín/espada.
- [x] Implementar almacenamiento y adquisición de dos pociones en Jugador, 50 %/20 % independientes por victoria definitiva, una unidad por éxito y conservación durante la aventura.
- [x] Verificar límites 0/49/50/99 y 0/19/20/99, los cuatro resultados y enumerar 10.000 parejas: 40/40/10/10. Dos llamadas de botín por victoria y ninguna por derrota/muerte simultánea/interrupción; sin dados extra.
- [x] Verificar acumulación, inventario nuevo vacío, resurrección antes de la muerte definitiva, generador conservado entre encuentros/aventuras y espada intacta. Obtener pociones no recupera vida.
- [x] Implementar consumo encapsulado mediante Jugador.usarPocion, con curarse, recuperación real, una unidad solo tras éxito y retirada de la última entrada; sin modificar el mapa desde fuera.
- [x] Menú de exploración 1/2/3/4/5/0, consulta de pociones/equipo y uso con opciones fijas 1/2/0; paneles Unicode, cancelación, inválidos y cierre de entrada.
- [x] Añadir 31 casos de consumo/exploración; conservar vida/pociones/equipo entre encuentros sin curación automática, botín duplicado ni dados adicionales. Verificar alCurarse con actividad/estado/nivel e Instinto de supervivencia y su restauración posterior.
- [x] Oro y comercio mínimo de cuatro productos, repositorio en memoria sustituible, compras encapsuladas y recompensa fija según 3.17.
- [x] Venta unitaria de pociones y armas guardadas/equipadas, confirmación, precio de catálogo dividido entre dos, saldo siempre visible y restauración segura según 3.20.
- [x] Centralizar selección de repositorios, separar ofertas predeterminadas, validar correspondencias y documentar integración externa sin implementarla según 3.21.
- [x] Experiencia por victoria desde datos del enemigo, subidas múltiples, mejoras permanentes, vida independiente y reparto obligatorio con presentación/pruebas según 3.22.
- [x] Nivel en datos de enemigos, tres variantes, encuentros elegibles por nivel y validación/visualización/compatibilidad de repositorios según 3.23.
- [x] Generalizar equipo, ID de catálogo y ejemplares, siete ranuras, vida máxima, desequipado, tienda y repositorios compatibles según 3.24.
- [x] Generación 0–3 por ejemplar, botín de pergamino 10 %, mejora +1 segura, inventario y venta por definición original según 3.25.
- [x] Pergaminos de habilidad 5 % y maestría 2 % independientes, catálogo de 13 habilidades, aprendizaje permanente separado del equipo, consumo seguro y selección/confirmación de consola según 3.26.
- [x] Elección de nombre al crear una aventura y estado independiente en exploración, con desglose de atributos/habilidades y consulta sin efectos según 3.27; la limitación de base con efectos temporales se indica expresamente.
- [x] Unificar pantallas con PanelConsola y establecer normas futuras en AGENTS.md; alineación, ANSI, barras y flujos probados según 3.18.
- [x] Pausas narrativas mediante RitmoConsola, cuatro velocidades, selección persistente durante la sesión y pruebas sin esperas reales según 3.19.
- [x] Continuidad de fase 4 publicada en 6fc2a8d: misma instancia del jugador, vida restante sin recuperación, Lobos nuevos y sin límite de encuentros; muerte/terminación voluntaria devuelve al inicio y nueva aventura crea otro jugador. La fase 5 añade su primera recompensa fija.
- [x] Espada desgastada publicada en ec9dc46: ARMA, FUERZA +1, sin habilidades, aceptar/rechazar tras la primera victoria, oferta única y descarte definitivo. Se conserva su comportamiento al incorporar pociones.
- [x] Añadir 11 casos de RecompensaTest: oferta única/condicionada, no equipo inicial, +1 sin cambiar base ni vida, persistencia sin duplicar, rechazo, invalidación/EOF, aventura nueva sin espada, daño real 7→8, limpieza temporal y ranura ocupada sin sustitución.
- [x] Añadir 12 casos de AventuraTest: identidad jugador/enemigos, vida actual/máxima, menús, muerte y muerte simultánea, entrada inválida/interrumpida y limpieza temporal con equipo y habilidades de prueba.
- [x] Comprobar flujo en terminal con Dado real: primera victoria a 23/24, continuar a otro Lobo con 23/24, segunda victoria, terminar aventura, nueva aventura a 24/24, completar combate y salir con código 0. No se alteran tiradas ni atributos.
- [x] Añadir y ejecutar 34 escenarios de EquipamientoTest y 8 de HabilidadesConEquipoTest por autorización expresa del autor.
- [x] Añadir y ejecutar 46 escenarios de AtaqueTest: combinaciones, críticos, daño cero/letal, atacante enemigo y dado inválido. Estos escenarios no añaden habilidades; su integración se prueba en EventosAtaqueTest.
- [x] Conservar los 46 escenarios de AtaqueTest y añadir 19 de EventosAtaqueTest: fases, destinatarios, contexto único, estados diferentes, bruto/final, habilidades reales, cero/fallo/letal y contraataques sin duplicación.
- [x] Validar nivel positivo, actividad por aprendizaje/equipo y retirada sin nivel cero; ejecutar 14 escenarios de EstadoHabilidadTest junto con los anteriores.
- [x] Añadir 9 escenarios de InicioCombateTest: prioridad, participación, muertes, limpieza del jugador y desempates.
- [x] Añadir 21 escenarios de CombateCompletoTest: resultados, turnos, prioridad cambiante, ambas resurrecciones, contraataque letal, habilidades reales, limpieza viva/muerta, actividad y reutilización del jugador.
- [x] Preparar README con JDK 23, ejecución desde IntelliJ y terminal, opciones y comprobación con Maven.
- [x] Publicar el estado inicial en GitHub: commit `9a9f274`, rama main, repositorio `danisanchezadev/viajedragosaccesodatos`, por petición expresa del autor. Esa publicación no acredita una demo jugable ni una suite pasando.

### 8.3 Fuera del alcance del MVP actual

- [ ] Conexiones a bases de datos y JDBC.
- [ ] JPA / Hibernate / Spring Data.
- [ ] Lectura/escritura de TXT, CSV, JSON y archivos binarios para datos del juego.
- [ ] Guardado/carga de partidas o cualquier persistencia.
- [ ] Introducción deliberada de patrones Builder, Factory u otros como ejercicio de diseño.
- [ ] Infraestructura de servidor, API web, interfaz gráfica o multijugador, salvo cambio expreso de requisitos.

## 9. Orden recomendado para llegar al MVP

1. **Auditoría y compilación:** identificar fallos reales, dependencias y tests existentes.
2. **Combate mínimo completo:** inicio, iniciativa, ataques, muerte, final y retorno de un resultado coherente.
3. **Habilidades integradas:** asegurar estado correcto y activaciones de eventos sin duplicaciones.
4. **Equipo operativo:** equipar, retirar, aplicar y revertir efectos; comparador de armas.
5. **Catálogos en memoria:** clases Java con listas de instancias de prueba, sin fuentes externas.
6. **Flujo jugable en consola:** seleccionar encuentro, combatir y continuar o terminar.
7. **Objetos, inventario, huida y comercio:** implementar de uno en uno según reglas confirmadas y prioridad que marque el autor.
8. **Pruebas y documentación:** dejar el proyecto estable y comprensible para quien posteriormente practique acceso a datos.

El orden es una **propuesta**, no una autorización para ejecutar todos los pasos sin consultar.

## 10. Protocolo de trabajo por petición

Antes de programar, responde brevemente con:

**Objetivo de este cambio:** qué comportamiento concreto se va a conseguir.

**Análisis de impacto confirmado:** archivos, métodos, referencias y pruebas identificados en el repositorio.

**Impacto posible o decisiones pendientes:** dudas que requieren comprobar código o consultar al autor.

**Plan mínimo:** pasos y archivos que tocarás, evitando abstracciones nuevas.

Después de programar, informa de:

**Cambios realizados**, **comprobaciones ejecutadas y resultados**, **código afectado** y **pendientes**. Actualiza las casillas correspondientes de este documento solo cuando la evidencia lo justifique.

## 11. Preguntas que Codex no debe resolver por su cuenta

- La secuencia del ataque individual y el contraataque están confirmados, integrados y probados. El turno tiene dos acciones ordenadas y resuelve muertes entre ellas, según la aclaración expresa del autor durante la fase 3.
- No existe un modificador de agilidad del turno: el +2 se aplica exclusivamente al cálculo de iniciativa del poseedor.
- El comercio mínimo está confirmado en 3.17 y la venta en 3.20; otras ampliaciones requieren decisiones nuevas. La huida del jugador está confirmada en 3.10. alHuir/alPerseguir, persecución y huida enemiga permanecen fuera de alcance. Las pociones se confirman en 3.7/3.8/3.9; el consumo exitoso en combate sustituye al ataque y cancelar/inválidos/rechazos no consumen acción.
- Confirmado: alFinalCombate solo del jugador, siempre, como limpieza incluso muerto; el enemigo no participa.
- Confirmado: tras victoria se permite consultar inventario/usar poción/continuar/terminar aventura/salir, sin recuperación automática. Espada única tras primera victoria y botín de pociones tras cada victoria definitiva. Futuras ampliaciones necesitan reglas propias.
- Catálogo de enemigos confirmado en 3.11 y objetos en 3.12. Otros catálogos o fuentes externas requieren su propia autorización.

Si alguna de estas respuestas ya está expresada de forma inequívoca en código o pruebas del repositorio, documentarla con referencia a los archivos correspondientes. Si no, pedir una decisión concreta antes de implementarla.

---

**Regla final:** Codex debe ayudar a *terminar una aplicación sencilla que funcione*, no a convertirla en una arquitectura compleja. La ampliación didáctica posterior —patrones de diseño y múltiples métodos de acceso a datos— es deliberadamente un trabajo para otra fase.

