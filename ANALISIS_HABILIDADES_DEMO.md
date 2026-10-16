# Habilidades existentes: análisis y decisión sobre la demo

Fecha: 15 de octubre de 2026. Base local y main publicado: `072758f`.
Árbol de trabajo limpio al comenzar. Verificación: `mvn clean test`, 224 pruebas,
0 fallos, 0 errores, 0 omitidas. No se han modificado habilidades ni asignaciones.

Actualización del 16 de octubre: el autor confirmó los roles de los cinco
modificadores y autorizó su corrección. Se aplican comprobaciones de propiedad
en esas cinco habilidades sin cambiar fórmulas, eventos ni firmas. La suite final
ejecutada pasa con 286 pruebas. La primera partida sigue sin habilidades.

En esta tabla, «observable» se refiere a lo que ocurriría al asignar la habilidad:
actualmente Aventurero y Lobo no tienen ninguna. No existe narración identificada
por habilidad; solo se ven cambios de vida y el daño agregado de los ataques.

| Habilidad | Evento | Condición | Efecto implementado | Observable hoy si se asigna | Corrección o precaución |
|---|---|---|---|---|---|
| Caparazón | antesDeAplicarDanio | Propietario defensor; daño final > 0 | Resta el porcentaje indicado por EstadoHabilidad.nivel; convierte a int y limita a cero | Indirectamente, daño menor | Rol corregido sin cambiar fórmula |
| Castigo al torpe | antesDeAplicarDanio | Propietario atacante; defensa = 1 y ataque no fallido | +4 al daño final | Indirectamente | Rol corregido sin cambiar fórmula |
| Debilidad enemiga | alInicioCombate | Media efectiva de fuerza, resistencia, velocidad, agilidad e iniciativa del portador >= doble de la del oponente | Quita toda la vida actual del oponente; el motor resuelve después alMorir | Final inmediato si no resucita; imprime además medias de depuración | Retirar sus dos System.out.println para separar presentación; no hace falta cambiar su fórmula |
| Defensa perfecta | antesDeAplicarDanio | Propietario defensor; dado de defensa = 6 | Divide daño final entre 2, división entera | Indirectamente | Rol corregido sin cambiar fórmula |
| Furia contenida | alRecibirDanio; alFinalCombate | Daño efectivo; daño final > 0; aún no aplicada | +2 fuerza una vez; resta 2 al finalizar si la aplicó | Daño posterior diferente; no muestra fuerza | Funcional; final solo del jugador por regla confirmada |
| FuriaMaximaBerserker | Ninguno | Ninguna implementada | Solo declara nivelMinimo = 10; no hereda Habilidad | No | Incompleta; no utilizable como habilidad; fuera de esta tarea |
| Golpe certero | antesDeAplicarDanio | Propietario atacante; ataque no fallido | +2 al daño final, incluso desde cero | Indirectamente | Rol corregido sin cambiar fórmula |
| Guardia férrea | antesDeAplicarDanio | Propietario defensor; daño final > 0 | Resta 2 al daño final, mínimo cero | Indirectamente | Rol corregido sin cambiar fórmula |
| Ímpetu | alHacerDanio; alFinalCombate | Daño efectivo y final > 0; menos de tres incrementos | +1 agilidad por activación, máximo tres; retira lo acumulado al finalizar | Daño y defensa posteriores distintos; no muestra agilidad | Funcional; limpiar enemigo no forma parte de las reglas actuales |
| Instinto de supervivencia | alInicioCombate; alRecibirDanio; alCurarse; alFinalCombate | Vida <= 30% y sin aplicar; vida > 30% y aplicado | +3 fuerza al bajar al umbral; -3 al recuperarse por encima; retira efecto pendiente al finalizar | Daño posterior distinto; no muestra fuerza | Funcional con vida máxima positiva; alCurarse se ejecuta dentro de Combatiente.curarse y requiere atención para atribuir la narración |
| Primer impulso | alInicioCombate; alFinalCombate | No aplicado aún | +nivel del constructor a iniciativa; revierte si lo aplicó | Puede cambiar orden, sin mostrar el aumento | Funcional |
| Rabia | alInicioCombate; alFinalCombate | No aplicada aún | +2 fuerza, +1 agilidad, -1 resistencia base; revierte los tres cambios al finalizar | Ataque/defensa diferentes; no muestra atributos | Funcional; atributos efectivos siguen limitados a cero |
| Sed de batalla | alHacerDanio | Daño efectivo, ataque no fallido, daño final > 0 | Cura 2 al atacante, limitada a su vida máxima | Sí, en la vida mostrada después; sin identificar la habilidad | Funcional; con vida completa no debe narrarse curación inexistente |
| Segundo aliento | alInicioTurno | Portador vivo y vida estrictamente inferior a la mitad | Cura nivel del constructor, limitado a vida máxima | Sí, en la vida del turno; sin identificar la habilidad | Funcional; exactamente la mitad no activa el efecto |

El análisis inicial detectó que los cinco modificadores actuaban en ambos roles.
El evento sigue sin recibir un parámetro nuevo: ahora cada implementación usa
ContextoAtaque y comprueba que el estado recibido sea el asociado a su habilidad
en el participante del rol permitido. Así se conserva el despachador y se evita
confundir habilidades del mismo tipo en ambos combatientes. El defecto anterior
y la corrección autorizada se detallan en ANALISIS_ROLES_MODIFICADORES_DANIO.md.

Solo Caparazón lee el nivel de EstadoHabilidad. Las demás magnitudes anteriores
proceden de campos internos o del constructor. Se conserva esa semántica. Cada
personaje debe recibir instancias propias: varias habilidades contienen estado
mutable que no debe compartirse entre combatientes.

Ninguna habilidad real implementa alMorir, alFinalTurno, antesDeAtacar,
antesDeCalcularDanio, alHuir o alPerseguir. Las resurrecciones existentes en las
pruebas proceden de habilidades controladas de prueba, no de una habilidad de
producción que pueda asignarse a la demo.

## Propuesta descartada por el autor

El autor rechazó la asignación siguiente: Aventurero empieza sin habilidades y
el primer Lobo tampoco las necesita. La partida de aprendizaje se conserva sin
habilidades. Su adquisición futura por progresión, recompensas o equipo queda
pendiente de definir; no se inventan esas mecánicas. La lista siguiente queda
únicamente como registro de la propuesta anterior, no como trabajo pendiente.

- Aventurero: Rabia, aprendida a nivel 1, y SegundoAliento(2), aprendida a nivel 2.
- Lobo: FuriaContenida, aprendida a nivel 2, y SedDeBatalla, aprendida a nivel 2.

Son cuatro habilidades con cambios iniciales de tres atributos, un aumento de
fuerza condicionado a recibir daño, curación por golpear y recuperación al inicio
de turno por vida baja. No requieren cambiar eventos, fórmulas ni roles de daño.
La fuerza/agilidad cambian el daño mediante la fórmula normal; no hay un
modificador directo de daño final en esta selección. Las curaciones y Furia
contenida dependen del desarrollo real: no se garantizan ni se forzarán.
Rabia se restaura al finalizar para el jugador. El enemigo no ejecuta finales,
según la regla confirmada, y cada demo construye un Lobo nuevo.

Una alternativa razonable es sustituir Rabia por Ímpetu para ver una progresión
de agilidad tras cada golpe, en lugar de cambios al inicio. No se selecciona
ninguna combinación para la partida de aprendizaje.

## Alternativas de narración estudiadas, fuera de la tarea actual

El autor ha indicado que no se añada narración de habilidades ni se cambie la
interfaz. Estas alternativas quedan solo como análisis previo, sin autorización
de implementación.

1. Comparar antes/después de cada llamada de habilidad en Combate: vida de ambos,
   atributos base/efectivos y, para los eventos de ataque, daño bruto/final e
   indicadores del contexto. Es la opción mínima para las cuatro habilidades
   propuestas: métodos privados de captura/comparación, salida opcional y el
   nombre del portador/evento. No añade clases ni cambia firmas públicas. Los
   cambios de daño se describen como cambios del daño calculado, no como vida ya
   perdida; el ataque conserva su informe de daño realmente aplicado. No se
   imprime nada si no hay cambios relevantes, ni se narra aplicar como restaurar.
2. Hacer que los eventos devuelvan datos de sus efectos: exigiría modificar
   EventosHabilidad, implementaciones, llamadas y habilidades de prueba. Aporta
   atribución explícita, pero tiene mucho más impacto y no se recomienda para
   esta demo.

La primera alternativa tiene un límite importante para una futura selección que
incluya Instinto de supervivencia junto con curaciones: curarse dispara alCurarse
internamente. Una comparación exterior ve el cambio conjunto, pero no puede
atribuir el cambio de fuerza a esa habilidad anidada. No debe atribuirse a Sed de
batalla o Segundo aliento. Para narrar también cada evento anidado individual
habría que extender la captura a Combatiente.curarse; ese alcance debe acordarse
antes de añadir mecanismos o cambiar firmas. La selección propuesta no contiene
una habilidad con efecto en alCurarse, por lo que evita esa limitación en la demo
sin alterar el funcionamiento de las otras habilidades.

Archivos que habría afectado la propuesta descartada: Combate.java para narración opcional y eventos; app.java para
las cuatro asignaciones; pruebas de demo/eventos/compatibilidad; README y contexto.
DebilidadEnemiga.java solo necesitaría retirar las impresiones de depuración, si
se incluye esa limpieza de presentación. No se prevén cambios de Equipo,
EstadoHabilidad, fórmulas, niveles, tiradas, muertes ni resultados.

La tarea vigente valida el sistema únicamente mediante pruebas con personajes de
prueba. HabilidadesRealesIntegracionTest conserva sus 21 casos de efectos,
condiciones, curación, restauración, niveles, equipo y equivalencia de modos.
RolesModificadoresDanioTest comprueba ahora los roles corregidos en 41 casos.
El análisis separado está en ANALISIS_ROLES_MODIFICADORES_DANIO.md.

Estado: propuesta de demo descartada; partida básica conservada. Solo se corrigen
los cinco modificadores autorizados. Sin narración, commit ni push.
