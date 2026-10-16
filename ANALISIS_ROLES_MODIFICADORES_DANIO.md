# Modificadores de daño: roles confirmados y corregidos

Actualizado: 16 de octubre de 2026. Base publicada: `072758f`, más la validación
local previa de 255 pruebas. Se conservan esos cambios locales. El autor confirmó
los cinco roles y autorizó su corrección. Sin commit ni push de esta fase.

## Análisis técnico y solución mínima

Se revisaron ContextoAtaque, Combate.dispararHabilidades, EventosHabilidad,
Habilidad.equals/hashCode, las cinco implementaciones y sus llamadas en pruebas.

Combate ejecuta antesDeAplicarDanio primero para las habilidades activas del
atacante y después para las del defensor. Cada llamada recibe el EstadoHabilidad
asociado a esa entrada. ContextoAtaque ya identifica a ambos combatientes y estos
permiten consultar el estado de cada habilidad. Aprender y equipar crean estados
propios por combatiente, incluso si se comparte la instancia de una habilidad.

No es necesario añadir un propietario mutable al contexto ni cambiar firmas:

- Caparazón, Defensa perfecta y Guardia férrea comprueban que
  contexto.getDefensor().getHabilidades().get(this) sea el mismo objeto que estado.
- Castigo al torpe y Golpe certero hacen esa comprobación sobre getAtacante().
- Si la identidad no coincide, retornan antes de modificar el daño. Después de
  esa guarda se conserva íntegro el cuerpo anterior.

La comparación usa identidad de EstadoHabilidad, no solo containsKey(this): la
igualdad de Habilidad es por clase y ambos pueden poseer el mismo tipo. Tampoco
basta comparar la identidad de la habilidad, porque el equipo puede compartirla.
Las pruebas cubren ambos supuestos con estados separados y niveles diferentes.
Como cualquier código que utiliza los estados por propietario, esta solución
presupone no inyectar manualmente el mismo EstadoHabilidad en mapas de personajes
distintos; aprender/equipar ya cumplen ese requisito. No se cambia el setter del
mapa ni se añade una regla al equipamiento.

Añadir portador al evento habría obligado a modificar firmas, llamadas y pruebas
sin aportar información que no esté disponible. Filtrar clases específicas en
Combate habría acoplado el motor a estas cinco habilidades. Se descartan ambas
alternativas para este cambio.

No se modifican ContextoAtaque, Combate, EventosHabilidad, EstadoHabilidad, Equipo,
Combatiente ni las ocho habilidades restantes con efectos.

## Comportamiento anterior y corrección aplicada

Todas conservan su evento antesDeAplicarDanio y sus magnitudes/niveles actuales.

| Habilidad | Antes, con dueño atacante | Antes, con dueño defensor | Comportamiento confirmado y aplicado | Método modificado |
|---|---|---|---|---|
| Caparazón | Reducía su propio daño causado | Reducía el daño recibido | Solo defensor; daño positivo, reducción porcentual con estado.nivel, truncamiento y mínimo cero | Caparazon.antesDeAplicarDanio |
| Castigo al torpe | Sumaba 4 si la defensa rival era 1 y el ataque no fallaba | Sumaba 4 al daño que él mismo recibía con defensa 1 | Solo atacante; mismas condiciones y +4 interno | CastigoAlTorpe.antesDeAplicarDanio |
| Defensa perfecta | Reducía su daño si el rival obtenía defensa 6 | Reducía el recibido con defensa 6 | Solo defensor; misma condición y división entera entre dos | DefensaPerfecta.antesDeAplicarDanio |
| Golpe certero | Sumaba 2 al daño causado en ataque no fallido | Sumaba 2 al daño recibido si el ataque no fallaba | Solo atacante; mantiene +2 interno, incluso desde daño cero | GolpeCertero.antesDeAplicarDanio |
| Guardia férrea | Restaba 2 a su daño causado | Restaba 2 al recibido | Solo defensor; daño positivo, resta 2 y mínimo cero | GuardiaFerrea.antesDeAplicarDanio |

## Ejemplos reproducibles después de corregir

Atacante: fuerza 10, agilidad 3. Defensor: resistencia 4, agilidad 2. Ambos con
100 de vida, sin equipo ni otras habilidades. Nivel de estado 25; solo Caparazón
lo usa como porcentaje. Las otras cuatro mantienen su magnitud interna.

| Habilidad | Dados ataque/defensa | Daño sin habilidad | Con dueño atacante | Con dueño defensor |
|---|---|---|---|---|
| Caparazón | 4/2 | 9 | 9 | 6 |
| Castigo al torpe | 4/1 | 17 | 21 | 17 |
| Defensa perfecta | 4/6 | 5 | 5 | 2 |
| Golpe certero | 4/2 | 9 | 11 | 9 |
| Guardia férrea | 4/2 | 9 | 9 | 7 |

## Pruebas y compatibilidad

RolesModificadoresDanioTest deja de caracterizar el defecto y pasa a comprobar
los roles correctos, con 41 casos:

- Diez de rol: cinco mantienen la expectativa anterior del rol correcto y cinco
  cambian la del rol contrario, que antes esperaban el comportamiento defectuoso.
- Diecisiete de condiciones y límites: defensa distinta de 1 o 6, ataque fallido,
  daño cero, Golpe certero desde cero, Guardia férrea limitada a cero y contraataque
  directo 1/6 sin nueva secuencia ni tiradas adicionales.
- Diez con ambos poseyendo el mismo tipo: instancias distintas o compartidas,
  estados distintos y niveles 25/50. Solo actúa la entrada del rol permitido.
- Cuatro de combinación ofensiva/defensiva con ataque normal, crítico, fallo y
  contraataque. El consumo de dados sigue siendo exacto.

CastigoAlTorpeTest, DefensaPerfectaTest, GolpeCerteroTest y GuardiaFerreaTest
mantienen sus 16 escenarios y resultados esperados. Se adapta solo la preparación:
registran la habilidad en el propietario adecuado y pasan su estado real, en vez
de construir un estado suelto sin propietario. Las firmas de las llamadas no cambian.

Los 21 casos de HabilidadesRealesIntegracionTest se conservan íntegros. También
se conservan las pruebas de eventos, equipamiento, atributos, muertes, restauración
y consola. La partida inicial sigue sin habilidades y no se añade narración.

La verificación final se registra en AUDITORIA_FASE_1.md y en la checklist de
CONTEXTO_CODEX_VIAJE_A_DRAGOS.md. No se han cambiado fórmulas, niveles, orden de
iteración, fases, daño bruto/final, crítico, fallo, contraataque ni finales.
