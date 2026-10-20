# Equilibrio de combate, progresión y economía — fase 4

20 de octubre de 2026. Fuente de verdad: estado local, con las fases anteriores conservadas.
Esta sección es el estado vigente; el informe de fase 3 se conserva al final como histórico.

## A. Situación inicial y decisiones

La comparación con perfiles preparados cambia la conclusión sobre el Capitán: el nivel 2 con
hierro, cuatro piezas básicas y pociones ganaba 199/200 (99,5 %), aunque el nivel 1 sin equipo
solo ganaba 27/200 en fase 3. No se justifica rebajarlo: se refuerza y queda en 102/200 (51 %).
Conserva opción 6, exclusión del sorteo, victoria única y desbloqueo de planta 3.
Los cuatro normales de barracones y los cinco enemigos de planta 1 no cambian.

Antes, B ganaba 500/500 aislados y 100/100 expediciones en plantas 3–5; cuatro victorias
profundas podían dar dos o tres niveles. El oro fijo incentivaba repetir plantas fáciles.
Se ajustan ataque/defensa/iniciativa de los quince enemigos profundos y el Capitán, pero
**ningún PS, velocidad, nivel, ID o planta**. No hay multiplicadores ni escalado automático.

Se probaron tres candidatos antes de editar producción. El primero daba 40 % a B contra
Capitán y 56,4 %/40,2 % en plantas 4/5, con desgaste excesivo. Se redujeron sus cifras sin
aumentar PS. El último ajuste de abismo mejora a B de 55,8 % a 62,6 %. Se detuvo ahí:
no se persiguen porcentajes rígidos ni mejoras marginales. El Capitán final tiene F11/R7/I5/A5.
La tabla económica propuesta 5/8/12/17/23 se eleva a **5/8/15/24/35** por el gasto medido en
pociones. Se mantienen los 24 precios existentes. La EXP media profunda baja de 28/45,6/68
a 20,4/26,8/39,4, manteniendo intacta la curva y todos los incrementos del jugador.

## B. Metodología y perfiles

`CatalogoAntesBalance` congela literalmente las 25 definiciones de fase 3, solo en src/test.
`BalanceProgresionTest` compara ese catálogo con el actual usando `SimulacionBalance`.
Se ejecuta **Combate.combatir(Scanner, PrintStream) real**, sin reproducir sus fórmulas.
Scanner entrega una decisión en el momento de la acción del jugador: se toma una grande
si la vida está a mitad o menos, después pequeña si no quedan grandes; si no, se ataca.
Usar poción consume la acción real y respeta iniciativa, eventos, críticos y contraataques.
La salida se descarta y el ritmo es instantáneo. No se añaden estrategias a producción.

Por catálogo: tres perfiles por planta y 100 combates por enemigo (500 por perfil);
Capitán adicional, 200 por perfil. Total 16.200 aislados, con muestras de 500 que tienen
error binomial orientativo máximo ±4,4 puntos al 95 %; las de 200, ±6,9.
Expediciones: 100 secuencias por perfil y catálogo (3.000 secuencias, máximo 12.000 combates),
más 500 secuencias de un mismo veterano para comparar plantas. Con N=100, margen máximo
orientativo ±9,8 puntos; importan diferencias grandes. No son millones de simulaciones.

Semilla aislada: `20261021L + planta*100000L + índiceEnemigo*1000L + muestra`.
El Capitán adicional tiene índice 0 y no es la misma muestra que la mezcla de cinco.
Expediciones: selección/dados `20261022L + planta*10000L + muestra`; botín de pociones,
equipo y pergaminos usan bases separadas 20261023/24/25/26. Dados: `nextInt(6)+1`.
Una rama distinta consume dados distintos, igual que en el motor. Las regresiones fijan
semillas/cuentas concretas, sin condiciones aleatorias que hagan frágil la suite.

Base F6/R2/I3/A2/V2 y 48 PS. Se usa Jugador para subir de nivel y repartir un punto extra
a Fuerza y otro a Resistencia por nivel. EXP sobrante inicial 0 y saldo líquido inicial
15 después de la preparación modelada. Vida completa solo al iniciar cada muestra.
La tabla contiene valores efectivos F/R/I/A/V, PS, pequeñas/grandes, IDs y habilidades.
A: poco preparado; B: preparación razonable; C: muy preparado. C mejora únicamente arma,
armadura y botas con +F/+R/+A respectivamente: +1 en plantas 1/2/5, +2 en plantas 3/4.
No se mejoran las otras piezas ni se supera su capacidad real.

| Perfil | Nivel | F/R/I/A/V | PS | Peq./gr. | Equipo (IDs) | Mejora en tres piezas | Habilidades |
|---|---:|---|---:|---|---|---:|---|
| P1 A | 1 | 6/2/3/2/2 | 48 | 0/0 | ninguno | 0 | ninguna |
| P1 B | 1 | 7/4/3/2/2 | 48 | 1/0 | espada_desgastada, armadura_cuero, casco_cuero | 0 | ninguna |
| P1 C | 2 | 11/7/4/5/3 | 54 | 2/0 | espada_hierro, armadura_cuero, casco_cuero, botas_ligeras | 1 | ninguna |
| P2 A | 1 | 7/3/3/2/2 | 48 | 1/0 | espada_desgastada, armadura_cuero | 0 | ninguna |
| P2 B | 2 | 10/6/4/4/3 | 54 | 2/1 | espada_hierro, armadura_cuero, casco_cuero, botas_ligeras | 0 | ninguna |
| P2 C | 3 | 13/9/5/6/4 | 60 | 3/2 | espada_hierro, armadura_cuero, casco_cuero, botas_ligeras | 1 | ninguna |
| P3 A | 3 | 12/8/5/5/4 | 60 | 1/0 | espada_hierro, armadura_cuero, casco_cuero, botas_ligeras | 0 | ninguna |
| P3 B | 4 | 15/11/8/6/6 | 72 | 2/2 | espada_hierro, armadura_cuero, casco_hierro, guantes_cuero, botas_marcha, anillo_vigilancia, amuleto_vigor | 0 | ninguna |
| P3 C | 5 | 19/15/9/9/7 | 78 | 3/3 | espada_hierro, armadura_cuero, casco_hierro, guantes_cuero, botas_marcha, anillo_vigilancia, amuleto_vigor | 2 | ninguna |
| P4 A | 5 | 17/13/9/7/7 | 78 | 1/1 | espada_hierro, armadura_cuero, casco_hierro, guantes_cuero, botas_marcha, anillo_vigilancia, amuleto_vigor | 0 | ninguna |
| P4 B | 6 | 21/17/10/8/8 | 84 | 2/2 | espada_acero, armadura_reforzada, casco_hierro, guantes_reforzados, botas_marcha, anillo_vigilancia, amuleto_vigor | 0 | ninguna |
| P4 C | 8 | 27/23/12/12/10 | 96 | 3/3 | espada_acero, armadura_reforzada, casco_hierro, guantes_reforzados, botas_marcha, anillo_vigilancia, amuleto_vigor | 2 | Instinto + Sed de batalla, Nv.1 |
| P5 A | 7 | 23/19/11/9/9 | 90 | 1/1 | espada_acero, armadura_reforzada, casco_hierro, guantes_reforzados, botas_marcha, anillo_vigilancia, amuleto_vigor | 0 | ninguna |
| P5 B | 8 | 26/22/12/11/11 | 96 | 2/2 | espada_forja, armadura_acero, casco_hierro, guantes_reforzados, botas_veloces, anillo_vigilancia, amuleto_vigor | 0 | ninguna |
| P5 C | 11 | 34/30/16/15/14 | 118 | 3/3 | espada_forja, armadura_acero, casco_acero, guantes_acero, botas_veloces, anillo_reflejos, amuleto_vital | 1 | Instinto + Sed de batalla, Nv.1 |

Procedencia y presupuesto:

- Las dos espadas fijas se obtienen tras victorias 1/2. Hierro no exige botín de planta 1.
  Armadura/Casco básicos cuestan 16 juntos: tras una victoria hay 20 monedas brutas.
  Las botas básicas pueden ser botín de planta 1; las pociones grandes también pueden caer allí.
- El preparado de barracones alcanza N2 con 20 EXP: cuatro victorias de 5, no decenas.
  El descenso inmediato tras tres ratas sigue dejando N1 y corresponde al perfil A.
- Precripta usa equipo de planta 2 o anterior, sin acero ni armadura reforzada. Sus seis
  piezas no fijas cuestan 109 si se compran todas; varias pueden proceder del botín.
- Mejorado completo es obtenible en planta 3 o anteriores: referencia 185 monedas,
  161 comprables y 24 de Guantes reforzados exclusivos de botín. Mixto de abismo añade
  tres superiores que ya pueden caer en planta 4. Superior completo también puede
  reunirse allí; no hay objetos de una planta posterior ni mejoras fuera de rango.
- Las habilidades de C (plantas 4/5) son pergaminos aprendibles, nivel aprendido 1:
  Instinto y Sed de batalla, con magnitudes internas originales. Se ejecutan sus eventos
  reales, curación y limpieza; las pruebas comprueban estado temporal limpio y nivel intacto.
- Son condiciones iniciales alcanzables mediante entrenamiento, compras y hallazgos,
  no una promesa de comprarlo todo tras unas pocas victorias ni tiempos medios de adquisición.
  DatosBalanceTest verifica fuentes y capacidades. No se simula retrospectivamente el coste
  de conseguir cada pieza/pergamino ni se regalan esos objetos en la partida real.

## C. Estadísticas anteriores y nuevas

F/R/I/A = Fuerza/Resistencia/Iniciativa/Agilidad. Se mantienen velocidad, PS y nivel.
Las flechas indican antes → actual. Los aumentos responden al crecimiento combinado del
jugador y del equipo; no proceden de una fórmula de escalado. Ningún enemigo tiene habilidades.

| Planta | Enemigo (ID) | F/R/I/A antes → actual | Velocidad | PS | Nivel | EXP antes → actual |
|---|---|---|---:|---:|---:|---|
| 1 | Rata cavernaria (rata_cavernaria) | 2/0/2/1 → 2/0/2/1 | 2 | 10 | 1 | 5 → 5 |
| 1 | Murciélago (murcielago) | 2/0/3/2 → 2/0/3/2 | 3 | 10 | 1 | 5 → 5 |
| 1 | Esqueleto (esqueleto) | 3/2/1/1 → 3/2/1/1 | 1 | 14 | 1 | 6 → 6 |
| 1 | Slime (slime) | 2/1/1/0 → 2/1/1/0 | 1 | 12 | 1 | 5 → 5 |
| 1 | Goblin (goblin) | 3/1/2/2 → 3/1/2/2 | 3 | 12 | 1 | 5 → 5 |
| 2 | Soldado corrupto (soldado_corrupto) | 6/3/3/2 → 6/3/3/2 | 3 | 24 | 3 | 15 → 15 |
| 2 | Arquero espectral (arquero_espectral) | 6/2/4/3 → 6/2/4/3 | 4 | 22 | 3 | 15 → 15 |
| 2 | Sabueso (sabueso) | 6/2/4/3 → 6/2/4/3 | 4 | 22 | 3 | 15 → 15 |
| 2 | Armadura animada (armadura_animada) | 6/4/2/1 → 6/4/2/1 | 2 | 28 | 4 | 18 → 18 |
| 2 | Capitán de barracones (capitan_barracones) | 8/4/4/3 → 11/7/5/5 | 3 | 35 | 4 | 25 → 25 |
| 3 | Momia (momia) | 9/4/3/2 → 18/9/4/6 | 2 | 32 | 5 | 25 → 18 |
| 3 | Araña de cripta (arana_cripta) | 8/4/5/4 → 18/8/6/7 | 5 | 28 | 5 | 25 → 18 |
| 3 | Espectro (espectro) | 9/4/5/4 → 19/9/6/7 | 4 | 30 | 6 | 28 → 20 |
| 3 | Necrófago (necrofago) | 10/5/4/3 → 20/10/5/6 | 3 | 34 | 6 | 30 → 22 |
| 3 | Sacerdote oscuro (sacerdote_oscuro) | 10/4/5/3 → 20/9/6/7 | 4 | 38 | 6 | 32 → 24 |
| 4 | Gólem de hierro (golem_hierro) | 12/7/3/2 → 26/14/5/7 | 2 | 48 | 8 | 45 → 26 |
| 4 | Salamandra (salamandra) | 11/5/6/4 → 25/12/8/9 | 5 | 36 | 8 | 40 → 24 |
| 4 | Herrero maldito (herrero_maldito) | 12/6/4/3 → 26/13/6/8 | 3 | 42 | 8 | 45 → 26 |
| 4 | Centinela ígneo (centinela_igneo) | 12/6/5/4 → 27/13/7/9 | 4 | 44 | 9 | 48 → 28 |
| 4 | Demonio de forja (demonio_forja) | 13/6/6/4 → 28/14/8/8 | 4 | 46 | 9 | 50 → 30 |
| 5 | Caballero abisal (caballero_abisal) | 15/8/5/4 → 32/17/8/11 | 4 | 54 | 11 | 60 → 36 |
| 5 | Sombra ancestral (sombra_ancestral) | 14/7/7/6 → 31/16/10/13 | 6 | 48 | 11 | 60 → 36 |
| 5 | Bestia del vacío (bestia_vacio) | 16/7/6/5 → 33/17/9/12 | 5 | 58 | 12 | 70 → 40 |
| 5 | Hechicero abisal (hechicero_abisal) | 15/7/7/5 → 32/16/10/13 | 5 | 52 | 12 | 70 → 40 |
| 5 | Guardián del abismo (guardian_abismo) | 16/8/7/5 → 34/18/10/12 | 5 | 60 | 12 | 80 → 45 |

## D. Combates aislados

Columnas: N, victorias, derrotas, simultáneas, victoria %, PS medios al vencer, turnos
iniciados, daño efectivo recibido y pociones pequeñas/grandes. Huidas: **0 en todas**;
la política no huye. Daño incluye contraataques y puede superar PS máximos por curaciones.
No se confunde daño con pérdida neta de vida. Capitán se presenta también por separado.

| Muestra | N | Victorias | Derrotas | Simultáneas | Victoria % | PS vencedor | Turnos | Daño | Pequeñas | Grandes |
|---|---|---|---|---|---|---|---|---|---|---|
| ANTES P1 A aislado | 500 | 500 | 0 | 0 | 100.0 | 44.61 | 2.64 | 3.39 | 0.00 | 0.00 |
| ACTUAL P1 A aislado | 500 | 500 | 0 | 0 | 100.0 | 44.61 | 2.64 | 3.39 | 0.00 | 0.00 |
| ANTES P1 B aislado | 500 | 500 | 0 | 0 | 100.0 | 45.92 | 2.41 | 2.10 | 0.00 | 0.00 |
| ACTUAL P1 B aislado | 500 | 500 | 0 | 0 | 100.0 | 45.92 | 2.41 | 2.10 | 0.00 | 0.00 |
| ANTES P1 C aislado | 500 | 500 | 0 | 0 | 100.0 | 53.35 | 1.36 | 0.65 | 0.00 | 0.00 |
| ACTUAL P1 C aislado | 500 | 500 | 0 | 0 | 100.0 | 53.35 | 1.36 | 0.65 | 0.00 | 0.00 |
| ANTES P2 A aislado | 500 | 411 | 89 | 0 | 82.2 | 28.33 | 6.47 | 28.81 | 0.51 | 0.00 |
| ANTES P2 A capitan | 200 | 56 | 144 | 0 | 28.0 | 16.38 | 8.23 | 50.94 | 0.94 | 0.00 |
| ACTUAL P2 A aislado | 500 | 386 | 114 | 0 | 77.2 | 29.02 | 5.98 | 29.76 | 0.52 | 0.00 |
| ACTUAL P2 A capitan | 200 | 8 | 192 | 0 | 4.0 | 17.00 | 5.56 | 54.56 | 0.91 | 0.00 |
| ANTES P2 B aislado | 500 | 500 | 0 | 0 | 100.0 | 46.82 | 3.69 | 8.34 | 0.02 | 0.06 |
| ANTES P2 B capitan | 200 | 199 | 1 | 0 | 99.5 | 40.68 | 5.50 | 18.08 | 0.15 | 0.21 |
| ACTUAL P2 B aislado | 500 | 455 | 45 | 0 | 91.0 | 45.37 | 4.43 | 17.91 | 0.27 | 0.19 |
| ACTUAL P2 B capitan | 200 | 102 | 98 | 0 | 51.0 | 24.30 | 9.63 | 66.69 | 1.42 | 0.86 |
| ANTES P2 C aislado | 500 | 500 | 0 | 0 | 100.0 | 55.92 | 2.56 | 4.34 | 0.00 | 0.02 |
| ANTES P2 C capitan | 200 | 200 | 0 | 0 | 100.0 | 53.16 | 3.47 | 7.84 | 0.01 | 0.06 |
| ACTUAL P2 C aislado | 500 | 499 | 1 | 0 | 99.8 | 54.29 | 2.90 | 6.70 | 0.01 | 0.05 |
| ACTUAL P2 C capitan | 200 | 196 | 4 | 0 | 98.0 | 46.62 | 5.23 | 20.63 | 0.13 | 0.33 |
| ANTES P3 A aislado | 500 | 498 | 2 | 0 | 99.6 | 49.98 | 4.01 | 10.92 | 0.09 | 0.00 |
| ACTUAL P3 A aislado | 500 | 106 | 394 | 0 | 21.2 | 24.03 | 5.21 | 61.56 | 0.83 | 0.00 |
| ANTES P3 B aislado | 500 | 500 | 0 | 0 | 100.0 | 67.23 | 3.09 | 5.17 | 0.00 | 0.02 |
| ACTUAL P3 B aislado | 500 | 394 | 106 | 0 | 78.8 | 43.36 | 6.27 | 57.90 | 0.62 | 0.94 |
| ANTES P3 C aislado | 500 | 500 | 0 | 0 | 100.0 | 74.92 | 2.27 | 3.21 | 0.00 | 0.01 |
| ACTUAL P3 C aislado | 500 | 498 | 2 | 0 | 99.6 | 67.50 | 3.26 | 13.48 | 0.03 | 0.16 |
| ANTES P4 A aislado | 500 | 500 | 0 | 0 | 100.0 | 68.42 | 3.64 | 10.49 | 0.01 | 0.05 |
| ACTUAL P4 A aislado | 500 | 147 | 353 | 0 | 29.4 | 36.90 | 6.04 | 85.63 | 0.70 | 0.81 |
| ANTES P4 B aislado | 500 | 500 | 0 | 0 | 100.0 | 77.09 | 2.89 | 7.07 | 0.00 | 0.01 |
| ACTUAL P4 B aislado | 500 | 365 | 135 | 0 | 73.0 | 51.71 | 5.94 | 66.79 | 0.63 | 0.97 |
| ANTES P4 C aislado | 500 | 500 | 0 | 0 | 100.0 | 91.73 | 2.22 | 4.85 | 0.00 | 0.00 |
| ACTUAL P4 C aislado | 500 | 494 | 6 | 0 | 98.8 | 84.42 | 3.02 | 16.24 | 0.02 | 0.11 |
| ANTES P5 A aislado | 500 | 500 | 0 | 0 | 100.0 | 80.99 | 3.44 | 9.69 | 0.01 | 0.04 |
| ACTUAL P5 A aislado | 500 | 127 | 373 | 0 | 25.4 | 40.35 | 5.90 | 98.41 | 0.68 | 0.83 |
| ANTES P5 B aislado | 500 | 500 | 0 | 0 | 100.0 | 88.56 | 3.09 | 8.00 | 0.01 | 0.03 |
| ACTUAL P5 B aislado | 500 | 313 | 187 | 0 | 62.6 | 56.16 | 6.20 | 84.36 | 0.76 | 1.09 |
| ANTES P5 C aislado | 500 | 500 | 0 | 0 | 100.0 | 113.78 | 2.30 | 4.78 | 0.00 | 0.01 |
| ACTUAL P5 C aislado | 500 | 493 | 7 | 0 | 98.6 | 104.77 | 3.32 | 19.16 | 0.05 | 0.15 |

Detalle por enemigo para el preparado B (100 por registro, mismas reglas):

| Perfil B por enemigo | N | Victorias | Derrotas | Simultáneas | Victoria % | PS vencedor | Turnos | Daño | Pequeñas | Grandes |
|---|---|---|---|---|---|---|---|---|---|---|
| DETALLE ANTES P1 B rata_cavernaria | 100 | 100 | 0 | 0 | 100.0 | 46.90 | 1.88 | 1.10 | 0.00 | 0.00 |
| DETALLE ANTES P1 B murcielago | 100 | 100 | 0 | 0 | 100.0 | 46.55 | 2.14 | 1.45 | 0.00 | 0.00 |
| DETALLE ANTES P1 B esqueleto | 100 | 100 | 0 | 0 | 100.0 | 44.90 | 2.97 | 3.10 | 0.00 | 0.00 |
| DETALLE ANTES P1 B slime | 100 | 100 | 0 | 0 | 100.0 | 46.18 | 2.32 | 1.82 | 0.00 | 0.00 |
| DETALLE ANTES P1 B goblin | 100 | 100 | 0 | 0 | 100.0 | 45.05 | 2.74 | 3.03 | 0.01 | 0.00 |
| DETALLE ACTUAL P1 B rata_cavernaria | 100 | 100 | 0 | 0 | 100.0 | 46.90 | 1.88 | 1.10 | 0.00 | 0.00 |
| DETALLE ACTUAL P1 B murcielago | 100 | 100 | 0 | 0 | 100.0 | 46.55 | 2.14 | 1.45 | 0.00 | 0.00 |
| DETALLE ACTUAL P1 B esqueleto | 100 | 100 | 0 | 0 | 100.0 | 44.90 | 2.97 | 3.10 | 0.00 | 0.00 |
| DETALLE ACTUAL P1 B slime | 100 | 100 | 0 | 0 | 100.0 | 46.18 | 2.32 | 1.82 | 0.00 | 0.00 |
| DETALLE ACTUAL P1 B goblin | 100 | 100 | 0 | 0 | 100.0 | 45.05 | 2.74 | 3.03 | 0.01 | 0.00 |
| DETALLE ANTES P2 B soldado_corrupto | 100 | 100 | 0 | 0 | 100.0 | 48.87 | 3.16 | 5.45 | 0.00 | 0.02 |
| DETALLE ANTES P2 B arquero_espectral | 100 | 100 | 0 | 0 | 100.0 | 47.20 | 3.07 | 7.60 | 0.00 | 0.05 |
| DETALLE ANTES P2 B sabueso | 100 | 100 | 0 | 0 | 100.0 | 47.25 | 2.97 | 6.91 | 0.00 | 0.01 |
| DETALLE ANTES P2 B armadura_animada | 100 | 100 | 0 | 0 | 100.0 | 48.97 | 3.78 | 5.83 | 0.00 | 0.05 |
| DETALLE ANTES P2 B capitan_barracones | 100 | 100 | 0 | 0 | 100.0 | 41.83 | 5.46 | 15.93 | 0.09 | 0.19 |
| DETALLE ACTUAL P2 B soldado_corrupto | 100 | 100 | 0 | 0 | 100.0 | 48.87 | 3.16 | 5.45 | 0.00 | 0.02 |
| DETALLE ACTUAL P2 B arquero_espectral | 100 | 100 | 0 | 0 | 100.0 | 47.20 | 3.07 | 7.60 | 0.00 | 0.05 |
| DETALLE ACTUAL P2 B sabueso | 100 | 100 | 0 | 0 | 100.0 | 47.25 | 2.97 | 6.91 | 0.00 | 0.01 |
| DETALLE ACTUAL P2 B armadura_animada | 100 | 100 | 0 | 0 | 100.0 | 48.97 | 3.78 | 5.83 | 0.00 | 0.05 |
| DETALLE ACTUAL P2 B capitan_barracones | 100 | 55 | 45 | 0 | 55.0 | 25.73 | 9.16 | 63.77 | 1.33 | 0.83 |
| DETALLE ANTES P3 B momia | 100 | 100 | 0 | 0 | 100.0 | 68.13 | 2.88 | 4.03 | 0.00 | 0.01 |
| DETALLE ANTES P3 B arana_cripta | 100 | 100 | 0 | 0 | 100.0 | 67.05 | 3.05 | 5.27 | 0.00 | 0.02 |
| DETALLE ANTES P3 B espectro | 100 | 100 | 0 | 0 | 100.0 | 67.17 | 3.11 | 5.71 | 0.01 | 0.05 |
| DETALLE ANTES P3 B necrofago | 100 | 100 | 0 | 0 | 100.0 | 67.54 | 3.04 | 4.62 | 0.00 | 0.01 |
| DETALLE ANTES P3 B sacerdote_oscuro | 100 | 100 | 0 | 0 | 100.0 | 66.24 | 3.35 | 6.24 | 0.00 | 0.03 |
| DETALLE ACTUAL P3 B momia | 100 | 91 | 9 | 0 | 91.0 | 46.65 | 5.87 | 44.91 | 0.38 | 0.77 |
| DETALLE ACTUAL P3 B arana_cripta | 100 | 85 | 15 | 0 | 85.0 | 44.15 | 5.88 | 52.15 | 0.55 | 0.83 |
| DETALLE ACTUAL P3 B espectro | 100 | 84 | 16 | 0 | 84.0 | 42.51 | 5.89 | 54.93 | 0.53 | 0.90 |
| DETALLE ACTUAL P3 B necrofago | 100 | 74 | 26 | 0 | 74.0 | 43.57 | 6.47 | 59.84 | 0.65 | 0.93 |
| DETALLE ACTUAL P3 B sacerdote_oscuro | 100 | 60 | 40 | 0 | 60.0 | 38.17 | 7.26 | 77.66 | 0.99 | 1.29 |
| DETALLE ANTES P4 B golem_hierro | 100 | 100 | 0 | 0 | 100.0 | 78.11 | 3.18 | 6.05 | 0.00 | 0.01 |
| DETALLE ANTES P4 B salamandra | 100 | 100 | 0 | 0 | 100.0 | 78.88 | 2.38 | 5.12 | 0.00 | 0.00 |
| DETALLE ANTES P4 B herrero_maldito | 100 | 100 | 0 | 0 | 100.0 | 78.02 | 2.82 | 6.14 | 0.00 | 0.01 |
| DETALLE ANTES P4 B centinela_igneo | 100 | 100 | 0 | 0 | 100.0 | 76.47 | 2.98 | 7.53 | 0.00 | 0.00 |
| DETALLE ANTES P4 B demonio_forja | 100 | 100 | 0 | 0 | 100.0 | 73.96 | 3.10 | 10.52 | 0.00 | 0.03 |
| DETALLE ACTUAL P4 B golem_hierro | 100 | 82 | 18 | 0 | 82.0 | 51.91 | 6.84 | 63.83 | 0.64 | 1.08 |
| DETALLE ACTUAL P4 B salamandra | 100 | 81 | 19 | 0 | 81.0 | 57.41 | 4.98 | 51.34 | 0.43 | 0.65 |
| DETALLE ACTUAL P4 B herrero_maldito | 100 | 80 | 20 | 0 | 80.0 | 53.85 | 5.45 | 58.76 | 0.51 | 0.86 |
| DETALLE ACTUAL P4 B centinela_igneo | 100 | 56 | 44 | 0 | 56.0 | 49.93 | 6.05 | 79.88 | 0.78 | 1.10 |
| DETALLE ACTUAL P4 B demonio_forja | 100 | 66 | 34 | 0 | 66.0 | 43.39 | 6.39 | 80.16 | 0.80 | 1.15 |
| DETALLE ANTES P5 B caballero_abisal | 100 | 100 | 0 | 0 | 100.0 | 90.78 | 3.04 | 5.38 | 0.00 | 0.01 |
| DETALLE ANTES P5 B sombra_ancestral | 100 | 100 | 0 | 0 | 100.0 | 88.84 | 2.87 | 7.48 | 0.00 | 0.02 |
| DETALLE ANTES P5 B bestia_vacio | 100 | 100 | 0 | 0 | 100.0 | 87.47 | 3.30 | 9.17 | 0.02 | 0.03 |
| DETALLE ANTES P5 B hechicero_abisal | 100 | 100 | 0 | 0 | 100.0 | 88.91 | 2.99 | 8.29 | 0.01 | 0.07 |
| DETALLE ANTES P5 B guardian_abismo | 100 | 100 | 0 | 0 | 100.0 | 86.79 | 3.24 | 9.69 | 0.00 | 0.03 |
| DETALLE ACTUAL P5 B caballero_abisal | 100 | 80 | 20 | 0 | 80.0 | 59.73 | 6.04 | 67.82 | 0.61 | 0.92 |
| DETALLE ACTUAL P5 B sombra_ancestral | 100 | 63 | 37 | 0 | 63.0 | 59.46 | 6.05 | 81.66 | 0.77 | 1.06 |
| DETALLE ACTUAL P5 B bestia_vacio | 100 | 57 | 43 | 0 | 57.0 | 50.33 | 6.57 | 95.79 | 0.90 | 1.33 |
| DETALLE ACTUAL P5 B hechicero_abisal | 100 | 69 | 31 | 0 | 69.0 | 54.62 | 5.92 | 78.87 | 0.67 | 0.95 |
| DETALLE ACTUAL P5 B guardian_abismo | 100 | 44 | 56 | 0 | 44.0 | 54.93 | 6.40 | 97.67 | 0.85 | 1.19 |

B queda en 78,8 %/73 %/62,6 % en plantas 3/4/5, aceptando que abismo quede algo por debajo
del rango orientativo. C puede dominarlos: no hay dificultad infinita. Planta 1 y los
normales de barracones son deliberadamente asequibles. No se endurecen para forzar una tasa.
El Capitán puede durar cerca de diez turnos para B; requiere comprobación manual de ritmo.

## E. Expediciones con desgaste

Cuatro encuentros con el mismo jugador, hasta muerte. Planta 2: tres normales y Capitán;
las otras: cuatro normales. No se evalúa aquí completar tres especies distintas.
No se restablece vida entre encuentros: solo pociones, habilidades y +6 PS reales al subir.
Se aplican EXP y reparto después de ganar. Solo la victoria concede oro y tiradas:
pociones 50/20 %, equipo 20 %, pergaminos 10/5/2 %. Equipo/pergaminos se conservan,
sin equiparlos ni usarlos durante la secuencia. No se venden hallazgos ni se repiten
las ofertas fijas en esta simulación parcial; sus flujos reales siguen probados en app.

Entre encuentros se reponen hasta las cantidades iniciales con Jugador.comprar, saldo
disponible y tienda de la planta (grandes primero). Después se toman grandes con déficit
>=16 y pequeñas con déficit >=8. Nunca se compra ni se cura después del cuarto combate.
Se usan habilidades reales y sus EstadoHabilidad. No se introduce reposición gratuita.

N = secuencias; victorias/derrotas/simultáneas son totales; encuentros es media de iniciados;
supervivencia es llegar vivo al cuarto; PS finales incluye muertos; turnos y daño son por
encuentro y pequeñas/grandes por secuencia. Curación en tabla posterior contabiliza
pociones/habilidades efectivas, sin los +6 de subida, que no pasan por curarse.

| Muestra | N sec. | Victorias | Derrotas | Simultáneas | Encuentros | Supervivencia % | PS finales | Turnos | Daño | Pequeñas | Grandes |
|---|---|---|---|---|---|---|---|---|---|---|---|
| ANTES P1 A expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 46.64 | 2.56 | 3.10 | 0.53 | 0.05 |
| ACTUAL P1 A expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 46.64 | 2.56 | 3.10 | 0.53 | 0.05 |
| ANTES P1 B expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 49.96 | 2.29 | 1.95 | 0.43 | 0.02 |
| ACTUAL P1 B expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 49.96 | 2.29 | 1.95 | 0.43 | 0.02 |
| ANTES P1 C expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 52.54 | 1.32 | 0.57 | 0.10 | 0.00 |
| ACTUAL P1 C expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 52.54 | 1.32 | 0.57 | 0.10 | 0.00 |
| ANTES P2 A expedicion | 100 | 329 | 37 | 0 | 3.66 | 63.0 | 16.47 | 5.62 | 19.92 | 3.08 | 0.49 |
| ACTUAL P2 A expedicion | 100 | 283 | 83 | 0 | 3.66 | 17.0 | 3.57 | 5.45 | 23.13 | 3.23 | 0.51 |
| ANTES P2 B expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 57.19 | 3.37 | 5.52 | 0.62 | 0.52 |
| ACTUAL P2 B expedicion | 100 | 391 | 9 | 0 | 4.00 | 91.0 | 39.44 | 4.29 | 13.81 | 1.37 | 1.14 |
| ANTES P2 C expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 58.14 | 2.54 | 4.03 | 0.41 | 0.31 |
| ACTUAL P2 C expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 56.00 | 2.79 | 5.04 | 0.47 | 0.40 |
| ANTES P3 A expedicion | 100 | 389 | 4 | 0 | 3.93 | 96.0 | 57.78 | 3.53 | 8.19 | 1.58 | 0.36 |
| ACTUAL P3 A expedicion | 100 | 26 | 100 | 0 | 1.26 | 0.0 | 0.00 | 4.58 | 54.62 | 1.08 | 0.00 |
| ANTES P3 B expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 76.11 | 2.85 | 5.58 | 0.49 | 0.77 |
| ACTUAL P3 B expedicion | 100 | 225 | 61 | 0 | 2.86 | 39.0 | 19.46 | 5.64 | 51.62 | 3.23 | 4.16 |
| ANTES P3 C expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 78.97 | 2.24 | 3.68 | 0.25 | 0.48 |
| ACTUAL P3 C expedicion | 100 | 397 | 1 | 0 | 3.98 | 99.0 | 71.53 | 3.19 | 14.33 | 1.16 | 2.21 |
| ANTES P4 A expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 80.02 | 3.33 | 9.00 | 0.67 | 1.29 |
| ACTUAL P4 A expedicion | 100 | 23 | 100 | 0 | 1.23 | 0.0 | 0.00 | 5.93 | 83.97 | 1.00 | 1.08 |
| ANTES P4 B expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 88.08 | 2.78 | 7.30 | 0.36 | 1.15 |
| ACTUAL P4 B expedicion | 100 | 205 | 69 | 0 | 2.74 | 31.0 | 16.84 | 5.44 | 59.63 | 3.35 | 4.20 |
| ANTES P4 C expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 99.02 | 2.24 | 4.82 | 0.34 | 0.55 |
| ACTUAL P4 C expedicion | 100 | 385 | 6 | 0 | 3.91 | 94.0 | 84.84 | 3.19 | 18.62 | 0.68 | 2.57 |
| ANTES P5 A expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 93.68 | 3.22 | 9.41 | 0.95 | 1.09 |
| ACTUAL P5 A expedicion | 100 | 28 | 100 | 0 | 1.28 | 0.0 | 0.00 | 5.66 | 90.28 | 1.00 | 1.09 |
| ANTES P5 B expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 99.57 | 2.76 | 7.55 | 0.60 | 1.05 |
| ACTUAL P5 B expedicion | 100 | 134 | 86 | 0 | 2.20 | 14.0 | 6.27 | 5.56 | 74.15 | 2.65 | 3.18 |
| ANTES P5 C expedicion | 100 | 400 | 0 | 0 | 4.00 | 100.0 | 125.06 | 2.21 | 5.35 | 0.24 | 0.65 |
| ACTUAL P5 C expedicion | 100 | 375 | 10 | 0 | 3.85 | 90.0 | 99.61 | 3.29 | 20.61 | 0.78 | 2.48 |

Economía/recursos por secuencia: oro/EXP **obtenidos**, gasto real en compras y saldo final.
Se verifica `saldo = 15 + oro - gasto`, unidades consumidas y límites de vida.

| Muestra | Oro obtenido | Gasto | Saldo final | EXP obtenida | Nivel final | PS curados |
|---|---|---|---|---|---|---|
| ANTES P1 A expedicion | 20.00 | 0.00 | 35.00 | 20.81 | 2.00 | 5.04 |
| ACTUAL P1 A expedicion | 20.00 | 0.00 | 35.00 | 20.81 | 2.00 | 5.04 |
| ANTES P1 B expedicion | 20.00 | 0.25 | 34.75 | 20.81 | 2.00 | 3.76 |
| ACTUAL P1 B expedicion | 20.00 | 0.25 | 34.75 | 20.81 | 2.00 | 3.76 |
| ANTES P1 C expedicion | 20.00 | 0.10 | 34.90 | 20.81 | 2.00 | 0.80 |
| ACTUAL P1 C expedicion | 20.00 | 0.10 | 34.90 | 20.81 | 2.00 | 0.80 |
| ANTES P2 A expedicion | 16.45 | 4.75 | 26.70 | 57.15 | 2.48 | 32.48 |
| ACTUAL P2 A expedicion | 22.64 | 4.75 | 32.89 | 45.65 | 2.04 | 34.00 |
| ANTES P2 B expedicion | 20.00 | 2.60 | 32.40 | 71.77 | 4.00 | 13.28 |
| ACTUAL P2 B expedicion | 31.28 | 2.60 | 43.68 | 69.52 | 3.91 | 29.20 |
| ANTES P2 C expedicion | 20.00 | 1.20 | 33.80 | 71.77 | 4.00 | 8.24 |
| ACTUAL P2 C expedicion | 32.00 | 1.20 | 45.80 | 71.77 | 4.00 | 10.16 |
| ANTES P3 A expedicion | 19.45 | 1.55 | 32.90 | 109.16 | 4.93 | 18.40 |
| ACTUAL P3 A expedicion | 3.90 | 0.55 | 18.35 | 5.24 | 3.03 | 8.64 |
| ANTES P3 B expedicion | 20.00 | 2.90 | 32.10 | 112.29 | 5.70 | 16.24 |
| ACTUAL P3 B expedicion | 33.75 | 25.30 | 23.45 | 45.72 | 4.45 | 92.40 |
| ANTES P3 C expedicion | 20.00 | 1.65 | 33.35 | 112.29 | 6.00 | 9.68 |
| ACTUAL P3 C expedicion | 59.55 | 12.25 | 62.30 | 81.20 | 5.99 | 44.64 |
| ANTES P4 A expedicion | 20.00 | 6.45 | 28.55 | 182.18 | 7.00 | 26.00 |
| ACTUAL P4 A expedicion | 5.52 | 1.75 | 18.77 | 5.94 | 5.00 | 25.28 |
| ANTES P4 B expedicion | 20.00 | 6.05 | 28.95 | 182.18 | 8.00 | 21.28 |
| ACTUAL P4 B expedicion | 49.20 | 27.35 | 36.85 | 54.64 | 6.37 | 94.00 |
| ANTES P4 C expedicion | 20.00 | 2.75 | 32.25 | 182.18 | 9.12 | 15.57 |
| ACTUAL P4 C expedicion | 92.40 | 14.50 | 92.90 | 103.06 | 8.94 | 56.01 |
| ANTES P5 A expedicion | 20.00 | 4.50 | 30.50 | 272.00 | 9.71 | 25.04 |
| ACTUAL P5 A expedicion | 9.80 | 1.75 | 23.05 | 11.23 | 7.02 | 25.44 |
| ANTES P5 B expedicion | 20.00 | 4.75 | 30.25 | 272.00 | 10.03 | 21.60 |
| ACTUAL P5 B expedicion | 46.90 | 15.10 | 46.80 | 52.00 | 8.22 | 72.08 |
| ANTES P5 C expedicion | 20.00 | 2.35 | 32.65 | 272.00 | 12.99 | 16.51 |
| ACTUAL P5 C expedicion | 131.25 | 11.85 | 134.40 | 147.60 | 11.91 | 55.51 |

B sobrevive 39/100, 31/100 y 14/100 en plantas 3/4/5; C, 99/100, 94/100 y 90/100.
Los perfiles A profundos no completan cuatro encuentros en esta muestra. Esto advierte
que ganar un combate no garantiza encadenarlos; entrenar o hacer incursiones cortas es
válido. No se ha añadido regeneración para ocultar el desgaste.

## F. Oro, experiencia y progresión

Planta.oroVictoria centraliza la recompensa, consultada con la planta de la definición
derrotada dentro del bloque ya protegido contra duplicación. El panel muestra exactamente
ese valor. Capitán recibe ocho monedas; derrotas, huidas e interrupciones no conceden oro.

| Planta | Oro antes → actual | EXP media antes → actual | Siguiente umbral de B | Victorias desde EXP 0 |
|---|---|---|---:|---:|
| 1 | 5 → 5 | 5,2 → 5,2 | 20 | 3,85 |
| 2 | 5 → 8 | 17,6 → 17,6 (15,75 sin Capitán) | 30 | 1,90 en normales |
| 3 | 5 → 15 | 28 → 20,4 | 50 | 2,45 |
| 4 | 5 → 24 | 45,6 → 26,8 | 70 | 2,61 |
| 5 | 5 → 35 | 68 → 39,4 | 90 | 2,28 |

Son victorias, no intentos ni tiempos garantizados. EXP acumulada para niveles
2/3/4/5/6/8/11 sigue 20/50/90/140/200/350/650. No cambian puntos ni +6 PS por nivel.
Cuatro victorias parten aproximadamente de B N4→N5, N6→N7 y N8→N9; las tablas anteriores
incluyen también las secuencias que mueren antes. Una ruta mínima ilustrativa de diez
victorias (tres ratas, Capitán, Momia/Araña/Espectro, Salamandra/Herrero/Centinela) ahora
suma 174 EXP y 140 oro: abismo N5, EXP 34 y saldo 155 antes de gastos. Sigue siendo un
cálculo, no una ruta garantizada; hasta A de abismo es N7 y tiene dificultades.

Pociones intactas: pequeña 8 PS/5 monedas, grande 16/10. Botín esperado nominal 7,2 PS
por victoria, sin garantizar existencias o curación útil. Conjunto superior de 325:
como mínimo nueve victorias de 35 desde 15 monedas, sin gastos, frente a 62 de cinco.
Es una cota económica, no una garantía de conseguirlo sin gastar en supervivencia.

Comparación justa: el mismo veterano N11, equipo superior, habilidades y política contra
normales de cada planta (sin repetir al Capitán), conservando desgaste y compras reales:

| Planta, mismo veterano | Supervivencia % | Oro | Gasto | Saldo final | EXP | Turnos/encuentro | Encuentros | PS finales |
|---|---|---|---|---|---|---|---|---|
| ECONOMIA P1 veterano | 100.0 | 20.00 | 0.05 | 34.95 | 20.81 | 1.17 | 4.00 | 117.75 |
| ECONOMIA P2 veterano | 100.0 | 32.00 | 0.10 | 46.90 | 62.58 | 1.17 | 4.00 | 117.33 |
| ECONOMIA P3 veterano | 100.0 | 60.00 | 1.55 | 73.45 | 81.86 | 1.55 | 4.00 | 115.15 |
| ECONOMIA P4 veterano | 100.0 | 96.00 | 8.65 | 102.35 | 107.10 | 2.19 | 4.00 | 111.19 |
| ECONOMIA P5 veterano | 90.0 | 131.25 | 11.85 | 134.40 | 147.60 | 3.29 | 3.85 | 99.61 |

Neto por expedición: 19,95/31,90/58,45/87,35/119,40 monedas. Neto por turno iniciado:
4,26/6,82/9,43/9,97/9,43. Se evita que una planta inicial sea siempre la mejor, pero NO
se garantiza que la última sea óptima para todos: forja es ligeramente mejor por turno
para este veterano. Abismo da más oro total, EXP y superiores. No se ajusta la tabla
por esa diferencia marginal; necesita comprobación humana por minuto y riesgo real.

Las ventas de botín no se incluyen en esos saldos. Las piezas conservan valoración global
por mitad entera. Referencia condicional: básicos venden de media por 33/7 y superiores
por 161/7 monedas; con aparición 20 %, 0,94 y 4,60 monedas adicionales por victoria si se
vendiera todo. Las mezclas dependen del peso de calidad por nivel, que no cambia.
No se obliga a vender ni equipar, ni se impone límite al inventario.

## G. Limitaciones

- Condiciones iniciales posibles, no aventuras completas desde creación ni tiempo medio
  de adquisición. Conseguir siete ranuras, mejoras o habilidades concretas puede tardar
  bastante más que subir de nivel. No se garantiza el botín usado en los perfiles.
- Un reparto F/R y una política no representan todas las decisiones humanas. Huidas
  simuladas cero; las pruebas de integración sí comprueban huida real sin recompensas.
- Ninguna de las trece habilidades aprendibles implementa alMorir actualmente: no se
  inventa una resurrección. Se mantiene la resolución real y las regresiones anteriores
  con resurrecciones controladas. Instinto/Sed sí actúan y limpian mediante el motor.
- Las compras son reales; navegación y duración humana no se simulan. Rentabilidad por
  turno no es tiempo real. No se aplican/equipan/venden los nuevos hallazgos: hacerlo
  podría mejorar los resultados. Las ofertas fijas están cubiertas por separado.
- Vida completa solo al iniciar la muestra, nunca entre encuentros. 0/100 o 500/500 no
  significan imposibilidad o invulnerabilidad universal. Semillas y resultados son finitos.

## H. Comprobación manual pendiente

1. Capitán N2 con cuatro piezas/pociones y después N3: comprobar duración y salto por nivel.
2. Incursiones de uno o dos encuentros antes de volver a comprar en plantas 3–5.
3. Tiempo de reunir tres especies en criptas con el riesgo actual, sin cambiar el requisito.
4. Reparto a Agilidad y otras habilidades antes de proponer más ajustes a enemigos.
5. Conseguir/sustituir superiores y medir oro por minuto en forja/abismo antes de tocar
   cantidades por pequeñas diferencias de esta muestra.

## Reproducir

`mvn test -Dtest=BalanceProgresionTest,DatosBalanceTest,RecompensasPorPlantaTest`
En PowerShell, entrecomillar el argumento -Dtest completo. `mvn clean test` verifica todo.
Producción limitada a RepositorioEnemigosMemoria, Planta y cálculo/texto de oro en app.
No cambian Combate, Jugador, equipamiento, repositorios externos, menús ni pausas.
Los observadores y catálogos de simulación existen solo en src/test. Sin commit ni push.

<details>
<summary>Histórico de fase 3: medición anterior, cifras y propuestas ya revisadas</summary>

# Equilibrio de las cinco plantas — fase 3

Datos y mediciones del 20 de octubre de 2026. No se han modificado estadísticas,
EXP ni niveles de los 25 enemigos, los 13 precios anteriores, las 5 monedas por
victoria, los umbrales de nivel ni ninguna fórmula de combate. Las once piezas
nuevas y sus precios provisionales están detallados en README.

## Método reproducible

`EquilibrioPlantasTest` usa el motor automático real, sin habilidades, pociones,
huida ni esperas. Para cada planta ejecuta 200 encuentros por definición (incluye
el Capitán), 1.000 por perfil y 25.000 en total. Cada encuentro empieza con un
jugador nuevo y vida completa. No es una simulación de supervivencia de una aventura.
El perfil de ruta usa niveles 1/1/2/4/6 al entrar a las plantas 1–5. El entrenado
usa niveles 1/3/5/8/11; son escenarios de comparación, no niveles obligatorios.

Jugador inicial: F6/R2/I3/A2/V2, 48 PS. Se otorga la EXP exacta del nivel objetivo
mediante Jugador y se reparten los puntos adicionales alternando Fuerza/Resistencia
(un punto en cada una por nivel). Los conjuntos completos no tienen pergaminos
aplicados; se equipan mediante el API real, sin modificar definiciones.
En mejorado se utiliza Espada de acero, no las dos armas simultáneamente.
Se llena la vida para igualar la condición inicial de cada muestra, también con
amuletos: eso NO añade curación al juego. Los conjuntos de otras calidades en
plantas tempranas son comparaciones hipotéticas, no cambios en disponibilidad.

Semilla por encuentro: `20261020L + planta * 10000L + índiceEnemigo * 200L + muestra`.
Dados: `new Random(semilla).nextInt(6) + 1`. Se conserva orden de catálogo, motor,
eventos y número de dados de cada combate; una rama puede consumir dados distintos
al variar el equipo. El promedio de daño incluye derrotas y muertes simultáneas.
Las pruebas comprueban identidad de estadísticas, equipamiento efectivo y
terminación, sin imponer una tasa de victoria como nueva regla del juego.

Reproducción: `mvn test -Dtest=EquilibrioPlantasTest`.

## Dificultad observada

| Planta | Nivel ruta | Victorias sin equipo | PS perdidos medios | Nivel entrenado | Victorias sin equipo | PS perdidos medios |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 1 | 1000/1000 | 3,76 | 1 | 1000/1000 | 3,76 |
| 2 | 1 | 697/1000 | 32,39 | 3 | 999/1000 | 7,75 |
| 3 | 2 | 667/1000 | 38,43 | 5 | 1000/1000 | 6,74 |
| 4 | 4 | 949/1000 | 25,14 | 8 | 1000/1000 | 7,07 |
| 5 | 6 | 968/1000 | 23,19 | 11 | 1000/1000 | 8,28 |

Planta 1 resulta asequible. El salto a planta 2 es fuerte si se baja inmediatamente:
contra el Capitán, el perfil nivel 1 gana solo 27/200 y pierde 46,14 PS de media.
Contra los cuatro normales gana 166/200, 166/200, 160/200 y 178/200, respectivamente.
En planta 3 el nivel 2 gana 178/200 contra Momia, 145/200 Araña, 125/200 Espectro,
111/200 Necrófago y 108/200 Sacerdote; entrenar o mejorar equipo tiene utilidad real.

La dificultad no crece de forma monótona con la profundidad para un jugador que
sube de nivel: en planta 4 a nivel 4 se gana 94,9 %, y en planta 5 a nivel 6, 96,8 %.
Todavía se pierden 25,14/23,19 PS por encuentro, importantes en combates consecutivos.
A nivel comparable al enemigo, ambos pisos son fáciles en encuentros aislados.
Por tanto, NO queda acreditado que planta 5 sea siempre el desafío más difícil ni
que planta 4 exija preparación en todos los repartos de puntos.

## Efecto de las calidades

PS perdidos por encuentro con el nivel entrenado (mismas semillas):

| Planta | Sin equipo | Básico completo | Mejorado completo | Superior completo |
|---|---:|---:|---:|---:|
| 1 | 3,76 | 1,64 | 0,98 | 0,50 |
| 2 | 7,75 | 4,92 | 3,78 | 3,34 |
| 3 | 6,74 | 5,23 | 4,23 | 3,72 |
| 4 | 7,07 | 6,08 | 4,64 | 4,66 |
| 5 | 8,28 | 7,27 | 6,14 | 5,94 |

Todos los perfiles entrenados equipados ganan 1.000/1.000 en esta muestra; no es
una garantía de invulnerabilidad. En planta 5 los dados medios por encuentro
bajan de 17,74 sin equipo a 14,32 con mejorado y 13,93 con superior. La diferencia
de 0,02 PS entre mejorado y superior en planta 4 no prueba superioridad del
mejorado: las ramas alteran el consumo del generador; la muestra es finita.

La Agilidad participa tanto en ataque como defensa. Fuerza y Resistencia aumentan
la diferencia de daño; Iniciativa afecta al orden y Velocidad a la huida, no al
cálculo del daño ordinario. VIDA mejora el máximo, sin curar al equipar. El fallo
de defensa y los contraataques conservan su comportamiento y pueden causar daño
incluso con gran resistencia. No se ha añadido escalado ni mitigación nueva.

Las nuevas piezas mejoran moderadamente la calidad anterior: cascos R1/2/3,
guantes F1/2/3, anillos I1/2/3; las botas superiores dan A2/V2 frente a A1/V1;
amuletos VIDA2/6/10. La armadura conserva R1/R3 y añade R4; las armas mantienen
F1/F2–3/F4. No se han añadido habilidades. Hasta tres mejoras por ejemplar pueden
elevar aún más las cifras; esta medición no representa un conjunto perfecto mejorado.

## EXP, oro y ritmo de adquisición

Los umbrales adicionales siguen 20,30,40… EXP. La prueba de ruta mínima ilustrativa
aplica recompensas reales, no combates simulados: tres ratas (15 EXP), Capitán
(25), Momia/Araña/Espectro (78) y Salamandra/Herrero/Centinela (133).
Se desbloquea planta 5 tras diez victorias y 251 EXP acumulada: nivel 6, 51 EXP
sobrante y 65 monedas antes de gastos. Las entradas a plantas 2/3/4/5 son niveles
1/2/4/6. No es una ruta garantizada ni una sugerencia de vencer al Capitán a nivel 1;
el sorteo de especies y los combates adicionales normalmente alargan el recorrido.

| Planta | Niveles enemigos | EXP por victoria | Oro por victoria |
|---|---|---|---:|
| 1 | 1 | 5–6 | 5 |
| 2 | 3–4 | 15–25 | 5 |
| 3 | 5–6 | 25–32 | 5 |
| 4 | 8–9 | 40–50 | 5 |
| 5 | 11–12 | 60–80 | 5 |

La EXP de plantas profundas acelera el nivel; no parece insuficiente en este
reparto de puntos. El oro no aumenta con el riesgo: volver a una planta fácil
es rentable para comprar. Las piezas nuevas mejoradas cuestan 20–25 (4–5 victorias
si no se usa oro previo); superiores 40–50 (8–10). Los Guantes reforzados tienen
referencia 24, pero son exclusivos de botín y no se pueden comprar.

Un conjunto superior cuesta 325 monedas; partiendo de 15 son 62 victorias sin
otros gastos ni ventas. Las diez victorias ilustrativas dejan 65, suficiente para
una compra relevante, no para completar el conjunto. El botín 20 % reduce esa
dependencia de compras; no garantiza una ranura específica y no se equipa solo.
Las piezas exclusivas conservan una valoración global y las ventas dan la mitad
entera, sin rentabilizar comprar para revender.

Pociones: pequeña 8 PS por 5 monedas, grande 16 por 10. El botín esperado nominal
es `0,5*8 + 0,2*16 = 7,2 PS` por victoria, sin contar máximos, reservas y decisiones
de consumo. Se añade como mucho otra pequeña con las cinco monedas ganadas.
No se obtiene curación automática ni se garantizan existencias. Los encuentros
profundos prematuros pierden más PS que esa reposición esperada, así que la
continuidad importa más de lo que refleja un combate aislado a vida completa.

## Propuestas pendientes: NO implementadas

1. Revisar primero mediante partidas consecutivas el acceso al Capitán: probar
   como candidato manual F7/30 PS frente a los actuales F8/35 PS si el salto resulta
   excesivo. Volver a medir antes de aprobarlo; entrenar a nivel 3 ya evita el muro
   en los encuentros de la muestra. No imponer un nivel mínimo ni escalado automático.
2. Revisar en una fase de balance los datos de plantas 4–5, empezando por un
   candidato de +1 Fuerza y +1 Agilidad en planta 5, para medir si mantiene combates
   cortos y eleva el riesgo frente al nivel 6. No incrementar solo PS para alargar
   los combates. Las cifras actuales permanecen intactas y la propuesta no está validada.
3. Si conseguir equipo obliga a repetir demasiado plantas fáciles, valorar una
   recompensa de oro por planta (candidato 5/6/7/8/10), previa autorización de esa
   nueva regla. Esta fase conserva cinco en todas. No bajar automáticamente precios
   antiguos ni tocar EXP para corregir un problema que parece principalmente económico.
4. Revisar la diferencia mejorado/superior después de jugar con pergaminos reales.
   Evitar más Fuerza/Agilidad sin medición: el entrenamiento ya puede trivializar
   encuentros. No aumentar bonificaciones solo para que el color/calidad se note.

Limitaciones: muestras finitas, reparto concreto F/R, sin consumibles/habilidades
ni decisiones humanas, conjuntos completos hipotéticos y vida restaurada solo
en la prueba. Falta validación visual y de ritmo de una aventura completa en IntelliJ.
El informe detecta problemas; no certifica un equilibrio definitivo de las cinco plantas.

</details>
