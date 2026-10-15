# Viaje a Dragos — contexto de producto, reglas y plan de implementación para Codex

> **Naturaleza:** documento vivo de referencia para un agente de programación.
> **Fecha:** 14 de octubre de 2026.
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

El juego tendrá acciones de combate como atacar, intentar huir y utilizar objetos. El comercio y la obtención de objetos forman parte del propósito de la aplicación, pero sus reglas operativas concretas todavía no están suficientemente confirmadas en este contexto como para que Codex las invente.

La primera versión utilizará **únicamente datos preparados dentro del propio programa**. Por ejemplo, una clase podrá ofrecer una lista de enemigos y otra una lista de armas. Esto permite probar las mecánicas sin consultar ni escribir ninguna fuente externa. En una fase posterior, el colaborador podrá sustituir gradualmente esos catálogos por acceso a bases de datos, JPA/Hibernate, Spring o formatos de archivo como TXT, CSV, binarios y JSON. **Esa fase futura no debe implementarse ahora.**

## 3. Reglas conocidas del combate

### 3.1 Participantes y atributos

- En el alcance actual, cada combate enfrenta a **un jugador y un enemigo**. No diseñar por defecto combates de grupos.
- Los atributos conocidos son `FUERZA`, `RESISTENCIA`, `AGILIDAD`, `INICIATIVA`, `VELOCIDAD` y `VIDA`.
- `VIDA` es una entrada del enum `Atributo`, pero la vida actual y la vida máxima se gestionan mediante campos separados (`vida`, `vidaMaxima`), no como entradas ordinarias de los mapas de atributos.
- Los atributos base y los modificadores de equipo son conceptos diferentes. El atributo efectivo es `max(0, base + modificador)`.
- Una modificación permanente debe afectar al valor base. Una bonificación o penalización de equipo debe afectar a los modificadores y poder revertirse sin destruir el valor base.
- Decisión expresa del autor durante la tarea de equipo: conservar el sistema de habilidades que suma cambios a la base y los resta en otro momento, sin añadir un mapa temporal. Esos eventos usan el valor base sin incluir equipo, conservan las diferencias con signo y las revierten. Rabia también debe revertirse en `alFinalCombate`. Los cambios permanentes y los de equipo siguen separados; durante una habilidad activa la base incluye provisionalmente su efecto.
- Los modificadores pueden ser **negativos**. No se deben truncar al guardarlos: el límite de cero se aplica al calcular el atributo efectivo.

### 3.2 Dados, iniciativa y ataques físicos

Estas reglas proceden del diseño de combate previamente discutido; **Codex debe comprobar cómo están implementadas antes de asumir que están operativas**:

- La iniciativa inicial se decide comparando `INICIATIVA × tirada de 1D6` de cada combatiente. Si empatan, se repite la tirada hasta obtener un orden. Excepción confirmada: si ambas iniciativas efectivas son cero, se comparan directamente los dados y se repiten solo los empates. La repetición es iterativa, sin recursión.
- Regla confirmada para el turno: `atacante()` compara iniciativa + 1D6 y añade +2 únicamente al cálculo del poseedor del turno, con desempate a su favor. No aumenta agilidad ni modifica permanentemente iniciativa.
- Para un ataque físico se discutió: `daño = (FUERZA + AGILIDAD + 1D6 del atacante) - (RESISTENCIA + AGILIDAD + 1D6 del defensor)`, con daño mínimo de cero.
- También se discutieron casos especiales asociados a resultados naturales del dado: una tirada de agilidad de `1` provoca fallo; una tirada defensiva de `1` permite recibir el daño completo; la combinación de ataque `1` y defensa `6` puede producir contraataque. **La interacción exacta de estas excepciones con la fórmula debe contrastarse con el código y las decisiones posteriores** antes de completar la mecánica.
- Hay una abstracción de dado (`TiradorDado`) para que la tirada pueda controlarse en pruebas.

**Secuencia del ataque individual confirmada por el autor e integrada:** crear ContextoAtaque; antesDeAtacar solo del atacante; tirar ataque/defensa; antesDeCalcularDanio del atacante y después del defensor; calcular bruto con las reglas existentes; copiar bruto a final; antesDeAplicarDanio del atacante y después del defensor; aplicar el final una vez si el ataque no falló; alHacerDanio del atacante y alRecibirDanio del defensor solo si hubo pérdida real de vida. Cada evento recibe el EstadoHabilidad de su entrada. Las fases se completan en orden; dentro de cada combatiente se conserva la iteración del mapa actual, sin imponer un orden nuevo.

**Contraataque confirmado:** 1 de ataque y 6 de defensa mantienen el ataque original fallido. El defensor resta directamente su fuerza a la vida del atacante. No se crea otro contexto ni se invierten participantes; no se dispara otra secuencia ni eventos posteriores por ese daño. Los eventos previos del ataque original sí conservan sus fases.

**Importante:** no mezclar estas fórmulas con reglas de otros reglamentos de rol discutidos en conversaciones distintas. Este documento describe el videojuego Java, no otro sistema de mesa.

### 3.3 Secuencia de combate conocida

1. Se determina el orden de activación de habilidades de inicio (`ordenHabilidades()`), según la lógica ya presente.
2. Se ejecutan los eventos `alInicioCombate` de las habilidades correspondientes, pasando a cada una su propio `EstadoHabilidad`.
3. Se comprueba si alguien ha muerto como consecuencia de esos efectos.
4. Si hay muertos, se ejecuta la resolución de muertes, que puede activar habilidades `alMorir` y cambiar el desenlace.
5. Si ambos siguen vivos, se determina el primer turno (`primerTurno()`).
6. El bucle automático ejecuta inicio de turno para ambos vivos por prioridad, resuelve muertes, selecciona atacante con `atacante()` y ejecuta `ataque()`, resuelve muertes y, si el combate continúa, ejecuta final de turno para ambos y alterna el poseedor. Las futuras elecciones de atacar/huir/usar objeto no se implementan en esta tarea.
7. Regla final confirmada: una muerte definitiva cierra el combate sin eventos normales posteriores ni alFinalTurno. Solo el jugador ejecuta alFinalCombate, siempre, incluso muerto, como excepción de limpieza y contingencias. El enemigo no ejecuta alFinalCombate.

Decisiones expresas del autor para completar combatir: ambos pueden ejecutar habilidades en las fases correspondientes; la prioridad se determina por la iniciativa en ese momento, en ausencia de prioridad se usa el poseedor del turno, y si no lo hay se tira para determinar quién empieza. Si alguien muere antes de ejecutar su inicio, ejecuta alMorir en vez de ese inicio. El límite de una activación de muerte por participante y resolución se conserva. PrimerTurno mantiene iniciativa × 1D6 y el autor confirmó la excepción de comparación directa de dados si ambas iniciativas son cero. La huida queda para otra tarea y no se inventa un resultado o límite para combates sin progreso.

Una resurrección que deje a ambos vivos permite continuar. Si alguien vuelve a morir en otro momento, comienza otra resolución y puede ejecutar de nuevo alMorir una vez en esa resolución. Las fases de turno comprueban muertes después de cada participante; el muerto definitivo no ejecuta el siguiente evento ordinario. La operación ataque conserva toda su secuencia ya validada antes de resolver sus muertes.

### 3.4 Muerte y resultados

- Resultados conocidos: `CONTINUA`, `JUGADOR_MUERE`, `ENEMIGO_MUERE`, `AMBOS_MUEREN`.
- Un combatiente con cero vida no implica necesariamente un resultado definitivo: su habilidad `alMorir` podría resucitarlo o provocar la muerte del adversario.
- Tras ejecutar un efecto de muerte, se comprueba de nuevo quién sigue vivo.
- En una misma resolución de muertes, **cada personaje debe ejecutar sus efectos `alMorir` como máximo una vez**; evitar cadenas recursivas infinitas o activaciones duplicadas.
- Cuando ambos mueren, existe una noción de prioridad para decidir el orden de resolución. El método `resolverMuertes(Combatiente prioridad)` y su auxiliar `personajeMuere(...)` ya contienen lógica para varios casos, pero deben verificarse las ramas y el cumplimiento de la regla de una sola activación.
- `resolverMuertes` está pensado para invocarse cuando **al menos uno está muerto**.

## 4. Habilidades

- `Habilidad` es una clase abstracta que implementa una interfaz de eventos (`EventosHabilidad`).
- Las habilidades se identifican en el mapa por su tipo concreto: se ha trabajado en `equals()`/`hashCode()` para que habilidades de la misma clase se traten como la misma clave. Confirmar implementación exacta.
- Cada `Combatiente` posee un `Map<Habilidad, EstadoHabilidad>`.
- `EstadoHabilidad` incluye al menos `aprendida`, `vecesEquipada` y `nivel`.
- Una habilidad puede ser aprendida o concedida por el equipo. El nivel se ha concebido como un valor combinado, no como dos niveles independientes.
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
- **Inventario:** debe permitir conservar y utilizar objetos, pero la estructura definitiva y las reglas de capacidad, apilamiento y consumo deben verificarse. No convertir ejercicios aislados de `Map` en reglas confirmadas del juego.
- **Comercio:** previsto como mecánica sencilla de compra/venta o interacción con una tienda; precios, moneda, descuentos y restricciones **no están confirmados para esta aplicación**. No importar reglas de ejercicios independientes sobre Strategy y tipos de cliente.
- **Progresión:** no hay aquí una especificación confirmada de experiencia, niveles, clases de personaje o distribución de atributos para el MVP. No inventarla.
- **Huida y uso de objetos en combate:** son acciones previstas, pero sus probabilidades, consecuencias y límites necesitan decisión o comprobación del código.
- **Fin de partida, curación, recompensas y persistencia del estado entre encuentros:** pendiente de especificación.

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

> **Estado:** 14 de octubre de 2026. Base publicada `79d15c7`. En local se han completado las etapas A/B/C de combatir y los invariantes de actividad/nivel de habilidades. El enfrentamiento automático completo termina con el ResultadoCombate correspondiente; no hay retorno ficticio o provisional. Sin commit ni push.
> **Leyenda:** `[x]` acredita únicamente la acción o existencia descrita en esa fila; no implica que el juego completo funcione. `[ ]` indica trabajo pendiente. Las revisiones realizadas se distinguen de sus correcciones pendientes.
> **Evidencia:** `mvn clean test`: BUILD SUCCESS; 33 fuentes principales y 21 archivos de pruebas recompilados; 202 pruebas, 0 fallos, 0 errores, 0 omitidas. Son las 158 anteriores, 14 de EstadoHabilidadTest, 9 de InicioCombateTest y 21 de CombateCompletoTest. El método combatir se ejecuta en enfrentamientos completos mediante pruebas; `app` sigue comparando armas, pues no se añade interfaz.

### 8.1 Bases presentes o desarrolladas parcialmente

- [x] Proyecto Java de consola y modelo `Combatiente` / `Jugador` / `Enemigo`, compilables con Java 23.
- [x] Enum `Atributo`, mapas separados de base/equipo y getters/setters; VIDA se gestiona por campos y no por los mapas.
- [x] Métodos `aumentarAtributo`, `disminuirAtributo` y `modificarAtributo`; las habilidades corregidas leen la base, no valores efectivos. Los métodos genéricos rechazan VIDA explícitamente.
- [x] `TiradorDado`, `Dado`, `ContextoAtaque` y lógica inicial de iniciativa.
- [x] `Combate` y `ResultadoCombate`, incluyendo resultados de huida sin mecánica implementada.
- [x] Resolución de muertes y final integrados en inicio/turnos/ataques; una invocación de muerte por participante y resolución, continuación tras resurrección y limpieza final solo del jugador.
- [x] `Habilidad` / `EstadoHabilidad` y mapa por combatiente; las 13 habilidades con efectos heredan ahora de Habilidad y su igualdad por clase. FuriaMaximaBerserker sigue siendo un borrador sin efectos.
- [x] Métodos para aprender y conceder/retirar habilidades de equipo; niveles y contadores probados con varias piezas y aprendizaje permanente. La retirada inválida produce un error antes de descontar efectos.
- [x] Contratos y clases de equipo, bonificaciones y habilidades otorgadas.
- [x] Comparador de equipo ejecutado con atributos distintos y una bonificación negativa.
- [x] Suite JUnit: 202 pruebas ejecutadas sin fallos ni errores; 158 previas, 14 de invariantes de habilidades, 9 de inicio y 21 de combate completo.

### 8.2 Auditoría completada y correcciones/integración pendientes

- [x] Auditar clases, paquetes, firmas, dependencias y referencias del repositorio.
- [x] Comprobar compilación del código principal: `mvn compile` termina correctamente.
- [x] Ejecutar las pruebas existentes con `mvn test`.
- [x] Conseguir compilación de las pruebas y suite pasando: `mvn clean test`, 202 pruebas pasan. Se conservan las adaptaciones de firmas del primer bloque.
- [x] Revisar `EventosHabilidad`: todas sus firmas actuales y overrides implementados reciben `EstadoHabilidad` y compilan en el código principal.
- [x] Revisar y corregir las seis llamadas del borrador a `dispararHabilidades`: las lambdas reciben habilidad y estado y pasan ese estado al evento.
- [x] Activar el auxiliar existente `dispararHabilidades` para usarlo desde ataque, recorriendo habilidad y estado juntos.
- [x] Completar etapas A/B/C: combatir, ordenHabilidades, primerTurno iterativo, eventosTurno, resolverMuertes, personajeMuere y finalDelCombate. El ataque individual conserva sus fórmulas y eventos.
- [x] Probar enfrentamiento completo: victoria, derrota, muerte simultánea, resurrección, alternancia, prioridad y restauración.
- [x] Corregir la selección del segundo participante en el borrador: `determinarOponente(primero)`.
- [x] Completar el bucle de turnos y eliminar todos los retornos ficticios/provisionales. CONTINUA es un estado interno para continuar, no el resultado público de un enfrentamiento terminado.
- [x] Aplicar el daño de ataque sin habilidades: copiar bruto a final y restar vida una vez si no falla. Conservar las reglas existentes de fallo, defensa fallida, crítico 6/1 y contraataque 1/6. Probadas las 36 combinaciones de dados y seis valores del dado crítico extra.
- [x] Integrar eventos del ataque según la secuencia confirmada, atacante antes que defensor, con el EstadoHabilidad propio de cada entrada. Probar daño final aplicado una vez, efectos posteriores solo con daño efectivo y contraataque directo sin segunda secuencia.
- [x] Integrar atacante y ataque existentes con el bucle, sin alterar sus fórmulas. Alternar poseedor independientemente de quién atacó efectivamente.
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
- [ ] Crear interfaz de consola cuando se autorice; combatir ya ejecuta el enfrentamiento, pero app sigue comparando armas.
- [ ] Crear catálogos Java de enemigos, armas, objetos y habilidades en memoria.
- [ ] Integrar encuentros desde esos catálogos, sin Factory ni persistencia.
- [ ] Definir e implementar atacar desde un menú, huir y usar objeto.
- [ ] Definir e implementar inventario y adquisición/consumo de objetos.
- [ ] Definir e implementar comercio mínimo si lo requiere el MVP.
- [ ] Definir el final del encuentro, recompensas y transición al siguiente.
- [x] Añadir y ejecutar 34 escenarios de EquipamientoTest y 8 de HabilidadesConEquipoTest por autorización expresa del autor.
- [x] Añadir y ejecutar 46 escenarios de AtaqueTest: combinaciones, críticos, daño cero/letal, atacante enemigo y dado inválido. Estos escenarios no añaden habilidades; su integración se prueba en EventosAtaqueTest.
- [x] Conservar los 46 escenarios de AtaqueTest y añadir 19 de EventosAtaqueTest: fases, destinatarios, contexto único, estados diferentes, bruto/final, habilidades reales, cero/fallo/letal y contraataques sin duplicación.
- [x] Validar nivel positivo, actividad por aprendizaje/equipo y retirada sin nivel cero; ejecutar 14 escenarios de EstadoHabilidadTest junto con los anteriores.
- [x] Añadir 9 escenarios de InicioCombateTest: prioridad, participación, muertes, limpieza del jugador y desempates.
- [x] Añadir 21 escenarios de CombateCompletoTest: resultados, turnos, prioridad cambiante, ambas resurrecciones, contraataque letal, habilidades reales, limpieza viva/muerta, actividad y reutilización del jugador.
- [ ] Preparar README con instrucciones de ejecución local.
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

- La secuencia del ataque individual y el contraataque ya están confirmados y probados, según el apartado 3.2; falta conectar esa operación al futuro bucle de combate.
- ¿Qué duración exacta tiene el modificador de agilidad del turno?
- ¿Qué reglas concretas rigen huida, uso de objetos, inventario y comercio?
- Confirmado: alFinalCombate solo del jugador, siempre, como limpieza incluso muerto; el enemigo no participa.
- ¿Qué ocurre al terminar cada encuentro y qué recompensas se conceden?
- ¿Qué catálogo mínimo de enemigos y objetos quiere el autor para la primera demo?

Si alguna de estas respuestas ya está expresada de forma inequívoca en código o pruebas del repositorio, documentarla con referencia a los archivos correspondientes. Si no, pedir una decisión concreta antes de implementarla.

---

**Regla final:** Codex debe ayudar a *terminar una aplicación sencilla que funcione*, no a convertirla en una arquitectura compleja. La ampliación didáctica posterior —patrones de diseño y múltiples métodos de acceso a datos— es deliberadamente un trabajo para otra fase.

