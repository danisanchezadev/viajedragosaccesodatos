# Viaje a Dragos

Demo Java de una aventura de combates consecutivos por consola, con datos en memoria.
Requiere JDK 23 y Maven. No utiliza persistencia ni servicios externos.

## Ejecutar desde IntelliJ

1. Abre el proyecto e importa `pom.xml` como proyecto Maven.
2. Selecciona JDK 23 como SDK del proyecto.
3. Abre `src/main/java/org/viajedragos/app.java` y ejecuta su método `main`.
4. En el menú inicial escribe `1` y pulsa Intro para comenzar una nueva aventura.
5. Cuando llegue la acción de Aventurero, escribe `1` para atacar, `2` para
   utilizar una poción que hayas obtenido en encuentros anteriores o `3` para huir.
6. Tras la primera victoria, encontrarás la **Espada desgastada**: `1` la equipa
   y `2` la rechaza definitivamente. Aporta **Fuerza +1** y ninguna habilidad.
7. Después del botín y de la oferta de espada aparece el menú de exploración:
   `1` revisa el inventario, `2` permite usar una poción, `3` continúa al siguiente
   encuentro, `4` termina la aventura y vuelve al inicio, `5` abre la tienda y `0` sale del juego.
8. Tras la segunda victoria real se ofrece la **Espada de hierro**, FUERZA +2:
   `1` la guarda y `2` la deja atrás. Para equiparla, entra en inventario y elige
   `2. Cambiar equipamiento`, selecciona ranura/pieza y confirma con `1`.

El menú inicial ofrece `[1] Nueva aventura`, `[2] Velocidad de narración` y `[0] Salir del juego`. Cada aventura crea un
Aventurero con 48/48 de vida, sin habilidades ni equipamiento. Cada encuentro crea
un enemigo nuevo del catálogo elegible por nivel, sin escalado automático de atributos ni límite de
encuentros. Al continuar se conserva la vida restante del Aventurero, salvo los
incrementos de una subida de nivel; no hay curación automática por continuar. La espada fija se ofrece
después de la primera victoria real y no vuelve a ofrecerse durante esa aventura,
aunque se rechace. Huir no cuenta como victoria. Además, cada
enemigo derrotado definitivamente puede dejar pociones. El Aventurero empieza con
15 monedas y recibe 5 por cada victoria definitiva; huir o perder no concede oro.

## Experiencia y subida de nivel

El Aventurero empieza en nivel 1, con 0 EXP y sin puntos pendientes. Cada victoria
definitiva concede la EXP definida en los datos del enemigo, además de las cinco
monedas, el botín y las ofertas de armas existentes. El catálogo inicial concede:
Goblin 5, Esqueleto 6, Bandido 7 y Lobo 10 EXP. Sus estadísticas no cambian.
Derrotas, muerte simultánea, huidas o combates interrumpidos no conceden EXP.

Para alcanzar el nivel 2 se necesitan 20 EXP; desde el 2 al 3 otras 30; desde el
3 al 4 otras 40, etc. Se descuenta cada umbral y se conserva el sobrante; una
recompensa puede provocar varias subidas. El indicador `EXP actual/necesaria`
muestra el progreso hacia el siguiente nivel, no la experiencia histórica total.

Cada subida añade permanentemente +1 a FUERZA, RESISTENCIA, AGILIDAD, INICIATIVA
y VELOCIDAD, +6 PS de vida máxima y +6 PS de vida actual, limitada al nuevo máximo.
VIDA se trata por separado. Estos aumentos modifican la base, sin duplicar las
bonificaciones de equipo. El aumento de vida por nivel no usa `curarse()` ni
activa habilidades `alCurarse`.

Después se reparten dos puntos por nivel alcanzado entre los cinco atributos:
se puede repetir atributo o elegir varios. La pantalla muestra base, valor
efectivo y puntos pendientes; no hay opción de salir sin repartirlos. Entradas
inválidas no consumen puntos. Si finaliza la entrada se interrumpe la sesión y
los puntos sin gastar quedan en el objeto Jugador; reutilizar ese personaje obliga
a asignarlos antes de iniciar otro combate. No existe guardado de partidas.
Si hay varias subidas se muestran todos los niveles y se reparten juntos todos
los puntos. El resultado final y el estado del inventario/exploración muestran
nivel y EXP. Solo el anuncio de subida utiliza una pausa narrativa; la selección
de atributos y sus validaciones son inmediatas.

La lógica está en `Jugador.agregarExperiencia` y `asignarPunto`, sin consola.
`app` concede las recompensas después de que `Combate` haya terminado y limpiado
los efectos temporales. Ejecutar solo el motor de combate no concede recompensas.
`DefinicionEnemigo` incluye `experiencia` positiva y `crearEnemigo` la transmite
al enemigo; los constructores anteriores se conservan con 5 EXP por compatibilidad.
Los repositorios nuevos deben indicar explícitamente la recompensa de cada registro.

## Presentación de consola

Todas las pantallas principales utilizan `PanelConsola`: bordes Unicode, títulos
centrados, interior de 48 columnas (44 de texto y dos de margen a cada lado),
opciones `[n]` y barras de vida de diez segmentos. Las líneas y las columnas se
ajustan automáticamente, incluidos nombres largos. Los códigos ANSI no cuentan
como anchura y se restablecen antes de los bordes. La comparación sigue coloreando
solo la diferencia cuando se habilita expresamente el color.

Se conserva la estética de los primeros paneles de inicio, encuentro, recompensa,
turno y ataque; se unifica con exploración, tienda, inventario, comparación,
cambio de equipo y uso de pociones. Los avisos breves, validaciones y confirmaciones
pueden permanecer fuera de paneles para no sobrecargar el registro.

La referencia es la consola de IntelliJ con fuente monoespaciada y soporte de
Unicode para los caracteres latinos, bordes y barras usados por el juego. Se han
verificado matemáticamente anchuras, ANSI y ajustes de texto mediante pruebas;
la apariencia concreta de la fuente en IntelliJ debe comprobarse allí.
`main` configura explícitamente UTF-8 para no heredar una codificación de Windows
que sustituya los bordes por interrogaciones. El menú real también se ha ejecutado
en terminal con bordes y acentos correctos. Las normas para futuras modificaciones
están en `AGENTS.md`.

## Pausas narrativas

Al ejecutar `main` desde IntelliJ, el ritmo predeterminado es **Normal**. En el menú
principal, `[2] Velocidad de narración` permite elegir una velocidad y volver:

| Opción | Modo | Pausa |
|---|---|---:|
| 1 | Instantánea | 0 ms |
| 2 | Rápida | 250 ms |
| 3 | Normal | 700 ms |
| 4 | Lenta | 1200 ms |
| 0 | Volver sin cambiar | — |

La elección permanece durante la sesión, también al comenzar otra aventura.
Se pausa tras presentar el encuentro y el orden del turno, antes del resultado
de un ataque enemigo, después de cada ataque, tras anunciar victoria/derrota y
al presentar recompensas u objetos encontrados. Cada panel se dibuja completo
antes de esperar; no hay animación por caracteres ni cambios de estilo.
La navegación, inventario, compras, validaciones y configuración no esperan.

`RitmoConsola` concentra los tiempos y la única espera real. Si se interrumpe,
conserva la señal, informa y omite nuevas esperas mientras siga interrumpido.
Las firmas anteriores de app con entrada/salida/dados proporcionados y el constructor
anterior de Combate siguen siendo instantáneos. Para otra velocidad se proporcionan
las nuevas sobrecargas con RitmoConsola. El modo automático nunca espera.
Las pruebas usan una espera sustituible que registra las llamadas sin dormir;
no requieren propiedades de Maven ni configuración del usuario.

## Oro y tienda

El saldo pertenece al mismo Jugador durante toda la aventura. `getOro`,
`agregarOro` y `gastarOro` permiten consultarlo y modificarlo sin saldo negativo:
las cantidades deben ser positivas, el gasto insuficiente falla y el desbordamiento
al sumar se rechaza. Una aventura nueva comienza con 15 monedas, sin pertenencias.

La opción `[5] Tienda` conserva las opciones anteriores de exploración. Su menú
ofrece `[1] Comprar objetos`, `[2] Vender objetos` y `[0] Volver a exploración`.
El oro se consulta directamente en Jugador y aparece en tienda, compra, venta y
confirmación; se actualiza inmediatamente tras cada operación. No hay stock limitado
ni pausas narrativas en estos menús.

| Producto | Compra | Venta |
|---|---:|---:|
| Poción pequeña | 5 | 2 |
| Poción grande | 10 | 5 |
| Espada desgastada | 12 | 6 |
| Espada de hierro | 20 | 10 |
| Armadura de cuero | 8 | 4 |
| Casco de cuero | 8 | 4 |
| Guantes de cuero | 10 | 5 |
| Botas ligeras | 8 | 4 |
| Anillo sencillo | 10 | 5 |
| Amuleto sencillo | 10 | 5 |

`ProductoTienda` enlaza una definición existente con su precio; su identificador
y nombre proceden de esa definición. `RepositorioTienda` permite listar y buscar
por identificador. `RepositorioTiendaMemoria` reutiliza las definiciones del catálogo
de objetos cargado por la aventura y no duplica atributos, curación ni habilidades.

`Jugador.comprar(id, repositorio)` resuelve la compra sin teclado. Las pociones
incrementan su cantidad y las armas se crean independientes y se guardan, sin
equiparlas automáticamente. Solo se cobra tras incorporarlas; un fallo recupera
saldo e inventarios. Una poción debe coincidir con la definición configurada en
el jugador, ya que su inventario cuenta unidades por tipo.

`ProductoTienda.precioVenta()` centraliza la división entera `precio / 2`.
La venta muestra cada poción con su cantidad y cada pieza guardada o equipada;
vende una unidad tras confirmar. Las equipadas se identifican y requieren
confirmar explícitamente su desequipado y venta. `[0]` en compra/venta vuelve
a tienda; cancelar la confirmación también vuelve a tienda sin cambios.

`Jugador.venderPocion` y `venderEquipo` validan catálogo, pertenencia y saldo
antes de retirar el objeto. Solo acreditan oro después de retirarlo. El desequipado
seguro reutiliza la restauración del intercambio: un fallo recupera ranuras,
atributos, vida, habilidades y sus estados. No se consumen pociones ni se disparan
eventos de curación por venderlas; las compras conservan su funcionamiento.

Las piezas creadas desde catálogo conservan su ID original. Se exige coincidencia
por ese ID y por nombre, ranura, bonificaciones y ausencia de habilidades adicionales;
las pociones deben coincidir con su definición completa configurada en Jugador.
Una correspondencia desconocida, ambigua o incoherente entre listado y búsqueda
no permite vender. Se usan los precios del repositorio proporcionado, también si
es alternativo. Las referencias al mismo objeto duplicadas manualmente entre
inventario/ranuras no se venden; las armas independientes de un mismo tipo sí.
Los constructores antiguos sin ID conservan la búsqueda estructural única como
compatibilidad. Las piezas nuevas no dependen solo del nombre o bonificaciones.
No se añaden restricciones al inventario ni se cambian precios de compra anteriores.

Para otro origen, seleccionar las implementaciones en `ConfiguracionRepositorios`:
`enemigos()`, `objetos()` y `tienda(armas, pociones, equipos)`. También se puede proporcionar
la instancia a la sobrecarga de `app.ejecutar` que acepta los repositorios.
Las firmas anteriores siguen disponibles. Actualmente solo existe memoria:
no hay implementaciones de ficheros, JDBC ni JPA. No cambian las ofertas de espadas,
el botín, la selección de enemigos ni los dados por comprar o recibir oro.

La [guía de acceso a datos](GUIA_ACCESO_DATOS.md) explica contratos, modelos,
configuración, validaciones y límites, con un ejemplo mínimo sin persistencia.
`CatalogoTiendaMemoria` contiene las ofertas predeterminadas; el repositorio valida,
copia e indexa. Su nuevo constructor acepta otra lista de productos y precios.
La aventura valida también las tiendas externas contra las definiciones completas
del catálogo de objetos, sin exigir las cuatro ofertas predeterminadas. Una tienda
vacía es válida; registros nulos, duplicados, desconocidos o incoherentes impiden
iniciar la aventura. Los datos predeterminados permanecen intactos.

## Catálogo de enemigos

Cada encuentro, incluido el primero, selecciona uniformemente una definición
cuyo nivel sea menor o igual que el del jugador. Los inferiores siguen disponibles
y se permiten repeticiones consecutivas. En niveles 1 y 2 participan los cuatro
iniciales (25 % cada uno); en nivel 3 hay cinco elegibles, en nivel 4 seis y desde
nivel 5 los siete actuales. No se escalan sus atributos automáticamente.

| Enemigo | Nivel | Fuerza | Resistencia | Iniciativa | Agilidad | Velocidad | Vida | EXP |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Lobo | 1 | 5 | 2 | 2 | 1 | 2 | 18 | 10 |
| Goblin | 1 | 3 | 1 | 2 | 2 | 3 | 12 | 5 |
| Bandido | 1 | 4 | 1 | 2 | 1 | 2 | 14 | 7 |
| Esqueleto | 1 | 3 | 2 | 1 | 1 | 1 | 14 | 6 |
| Goblin guerrero | 3 | 5 | 2 | 3 | 2 | 3 | 20 | 15 |
| Esqueleto veterano | 4 | 6 | 3 | 2 | 2 | 2 | 24 | 20 |
| Bandido de élite | 5 | 7 | 3 | 3 | 2 | 3 | 28 | 25 |

`RepositorioEnemigos.obtenerTodos()` devuelve `List<DefinicionEnemigo>` y no
selecciona enemigos. `RepositorioEnemigosMemoria` contiene una lista inmutable
de orden estable. El record DefinicionEnemigo guarda identificador, nombre,
descripción, cinco atributos, vida máxima, EXP y nivel; crearEnemigo construye una instancia
nueva con vida completa, mapas independientes y sin habilidades ni equipo.

La aventura carga una instantánea del catálogo una vez. La selección reside en
app y utiliza un Random de encuentros independiente, conservado durante toda la
sesión. No utiliza el Random de botín ni TiradorDado. Catálogos vacíos, entradas
nulas, identificadores duplicados o definiciones inválidas impiden iniciar la
aventura con un mensaje; no se inventa un enemigo de respaldo. Si no hay elegibles
para el nivel actual, se informa y se vuelve al inicio sin tirar dados ni consumir
acciones o conceder recompensas. El filtrado usa el nivel actualizado en cada
encuentro, sin recargar el catálogo. El panel inicial muestra el nombre y nivel del enemigo.

Los constructores anteriores de DefinicionEnemigo y Enemigo se mantienen con nivel
1. Los orígenes nuevos deben proporcionar `experiencia, nivel` explícitamente;
nivel < 1 se rechaza. No cambian ConfiguracionRepositorios ni el contrato del repositorio.

Equilibrio de variantes: 18.000 combates de prueba con semillas fijas, al nivel
mínimo de desbloqueo, vida completa, puntos en fuerza/resistencia/velocidad y
arma +0/+2. Todas las muestras terminan en victoria sin pociones; medias entre
2,128 y 3,222 turnos. Esto no garantiza supervivencia empezando herido. La simulación
histórica de los cuatro iniciales conserva sus resultados y comprobaciones.

Para sustituir el origen, otro desarrollador implementa RepositorioEnemigos y
conecta su instancia mediante la sobrecarga pública de app.ejecutar que recibe
entrada, salida, dados, azar de botín, repositorio y azar de encuentros. No necesita
cambiar Combate ni la selección. Las firmas anteriores delegan en memoria.
No hay lectura de archivos, JDBC, JPA, frameworks ni persistencia en esta versión.

## Catálogo de objetos y equipamiento

La aventura carga una sola vez las definiciones de `RepositorioObjetos`:
`obtenerArmas()`, `obtenerPociones()` y el método opcional `obtenerEquipamiento()`.
Este último devuelve definiciones adicionales, sin repetir las armas. Las
implementaciones anteriores siguen funcionando con su valor predeterminado vacío.
`RepositorioObjetosMemoria` devuelve listas
inmutables con Espada desgastada (`espada_desgastada`, FUERZA +1, sin habilidades),
Espada de hierro (`espada_hierro`, FUERZA +2, sin habilidades),
Poción pequeña (`pocion_pequena`, 8 PS) y Poción grande (`pocion_grande`, 16 PS).

`DefinicionArma` es un record con id, nombre y copia inmutable de bonificaciones.
`crearArma()` crea una instancia independiente en la ranura ARMA. Las bonificaciones
pueden ser negativas; VIDA se admite como bonificación de vida máxima, separada de
vida actual. `crearArma()` conserva también el ID de su definición.
`DefinicionPocion` contiene id, TipoPocion, nombre y recuperación positiva; nunca
cantidades poseídas. El inventario sigue perteneciendo exclusivamente a Jugador.

Para sustituir la fuente se implementa RepositorioObjetos y se pasa como último
argumento de la nueva sobrecarga pública:

```java
app.ejecutar(entrada, salida, dados, azarBotin, repositorioEnemigos,
        azarEncuentros, repositorioObjetos);
```

No se necesita cambiar la lógica del combate ni la de consumo. Las firmas
anteriores siguen disponibles y delegan en memoria. No hay consultas al repositorio
en cada encuentro, menú o uso. Cada aventura nueva vuelve a cargar sus definiciones.
Colecciones nulas, entradas nulas, ids repetidos entre cualquier categoría, alguna
de las dos espadas ausente, tipos de poción repetidos o incompletos y datos inválidos impiden iniciar
la aventura y devuelven al menú inicial, sin objetos de respaldo.

TipoPocion conserva sus métodos, nombres y 8/16 PS por compatibilidad. Son valores
predeterminados para los constructores antiguos y para el repositorio en memoria.
Jugador admite además un constructor con List<DefinicionPocion>, o configurarPociones
al preparar la aventura. La configuración se valida y copia antes de sustituir la
anterior, sin alterar cantidades. getDefinicionPocion devuelve los datos efectivos:
si otro repositorio define PEQUENA con otro nombre y 10 PS, inventario, botín y
ambos menús muestran esos datos, y usarPocion utiliza 10 PS mediante curarse.
No hay configuración global mutable ni consultas al enum que prevalezcan sobre
las definiciones configuradas.

Se mantienen el límite de vida, alCurarse, el descuento solo si hay recuperación,
la acción de combate y la oferta voluntaria única de espada tras la primera
victoria real. La espada se identifica por id, independientemente de su nombre
visible. El catálogo no genera botín ni ejecuta efectos.

Limitaciones: dos tipos de poción y sin catálogo de habilidades desde datos,
persistencia ni tecnologías externas. El catálogo puede añadir piezas de las siete ranuras. El sistema
existente de equipo con habilidades permanece intacto. Archivos/JDBC/JPA podrán
implementar este contrato posteriormente; no están implementados aquí.

## Equipamiento general: primera fase

`Equipo` es la representación común concreta y `Arma` se conserva como clase
compatible, incluidas sus habilidades. Las ranuras son ARMA, ARMADURA, CASCO,
GUANTES, BOTAS, ANILLO y AMULETO. Solo una pieza por ranura y un único anillo;
inventario ilimitado, sin requisitos de nivel, atributos o clase.

`DefinicionEquipo(id, nombre, tipo, bonificaciones)` es inmutable y crea ejemplares
independientes con `crearEquipo()`. Cada ejemplar conserva `getIdDefinicion()`;
dos piezas del mismo modelo comparten ID de catálogo, pero son objetos distintos,
seleccionables por su posición numerada. No se redefine equals por nombre ni
bonificaciones. `DefinicionArma` mantiene crearArma y añade comoEquipo; no se
necesita una subclase distinta para cada categoría.

Bonificaciones de todas las ranuras se suman; las permanentes del nivel se mantienen
en la base. En VIDA, la bonificación afecta exclusivamente al máximo efectivo:
20/30 con +5 pasa a 20/35, con otro +3 a 20/38, y retirando +5 a 20/33. Equipar no
cura ni dispara alCurarse; retirar recorta la vida actual solo si supera el máximo
resultante. Un intercambio conserva la vida hasta el máximo final, sin perder PS
por el máximo intermedio. La curación posterior respeta el máximo efectivo.
Los +6 PS por nivel se añaden una sola vez a la base y a la vida actual.

Inventario: `[1] Comparar`, `[2] Cambiar equipamiento`, `[3] Desequipar una pieza`,
`[0] Volver`. Cambiar exige elegir una pieza compatible y confirmar. Desequipar
muestra las ocupadas y solicita confirmación; devuelve el mismo ejemplar al final
del inventario. Cancelar/EOF no confirman ni retiran efectos. Los paneles muestran
todas las ranuras, incluidas vacías, y VIDA se presenta como Vida máxima.
No se añaden pausas en estos menús.

La tienda predeterminada tiene diez productos; conserva los cuatro anteriores y
añade las seis piezas de la tabla. Pueden comprarse, equiparse, desequiparse y
venderse, incluidas equipadas. La venta usa ID y contenido para no valorar una
pieza desconocida o modificada como otra definición. No hay botín aleatorio nuevo.
Los intercambios y ventas reutilizan la recuperación segura de ranuras, atributos,
vida y estados de habilidades. Siguen pendientes pergaminos, puntos de mejora,
bonificaciones aleatorias, rarezas y habilidades en las definiciones generales.

## Equilibrio inicial

Solo cambia la vida del Aventurero de consola: antes 24/24, ahora 48/48.
Fuerza 6, resistencia 2, iniciativa 3, agilidad 2 y velocidad 2 permanecen iguales.
La fuerza efectiva pasa a 7 únicamente si el jugador acepta la espada. Jugador
conserva sus reglas de atributos y las fórmulas del motor no cambian; tampoco hay curación automática.

EquilibrioAventuraTest utiliza el motor real, siempre Atacar, cuatro encuentros
uniformes y espada FUERZA +1 desde el segundo. Sin pociones ni huida. Compara
10.000 sesiones por candidato, con semillas reproducibles independientes: generadores
de semillas 20261018 (dados) y 20261019 (encuentros), usando nextLong por sesión.

| Vida inicial | Cuatro victorias / 10.000 | Supervivencia |
|---|---:|---:|
| 24 | 5.334 | 53,34 % |
| 28 | 6.387 | 63,87 % |
| 32 | 7.263 | 72,63 % |
| 36 | 8.046 | 80,46 % |
| 40 | 8.618 | 86,18 % |
| 48 | 9.402 | 94,02 % |

Con 48 PS, la duración media es 2,991 turnos por combate (Lobo 3,609; Goblin 2,651;
Bandido 2,691; Esqueleto 2,997). Quedan 25,66 PS de media entre quienes completan
los cuatro encuentros. El ajuste supera el objetivo del 90 % sin alargar combates
ni modificar daño, críticos, contraataques o iniciativa. Es una muestra fija, no
una garantía frente a cualquier secuencia de dados ni frente a encuentros ilimitados.
La espada sigue siendo una elección voluntaria en la consola normal.

La espada permanece equipada y su bonificación no se acumula. La fuerza base
sigue siendo 6 y la efectiva pasa a 7. Equiparla no recupera vida. Las entradas
inválidas repiten la elección; cerrar la entrada durante la oferta termina la
sesión sin equiparla. Rechazar no guarda la espada en el inventario de equipamiento;
su oferta y rechazo no se modifican al añadir la segunda espada.

## Espada de hierro e intercambio

Después de la segunda victoria real de cada aventura se ofrece una única Espada
de hierro: ARMA, FUERZA +2, sin habilidades. Huidas y derrotas no cuentan como
victorias. La oferta se realiza una sola vez, sin azar adicional: `1` guarda la
pieza mediante agregarEquipo y `2` la deja atrás definitivamente. Guardarla no
equipa, no reemplaza la espada anterior ni modifica atributos. El cierre de
entrada interrumpe la sesión sin conceder la pieza.

En inventario, `2. Cambiar equipamiento` muestra todas las ranuras, incluidas
las vacías. Se selecciona una ranura, una pieza guardada compatible y se confirma
con `1`; `0` permite cancelar. Si hay pieza equipada, se muestra la comparación
existente antes de confirmar. Si está vacía, se equipa directamente la guardada.

Jugador.equiparDesdeInventario(TipoEquipo, int) recibe el índice de lista desde
cero y valida límites y compatibilidad. Tras éxito retira solo esa entrada,
conservando duplicados, y añade la pieza anterior al final del inventario. Se
conservan ambas identidades, sin copiar piezas ni duplicar efectos. El equipo
seleccionado permanece en los encuentros posteriores; una aventura nueva empieza
sin equipo guardado ni equipado.

Combatiente.cambiarEquipoSeguro es un auxiliar protegido: reutiliza equipar y
desequipar sin cambiar sus reglas. Si devuelven fallo o lanzan RuntimeException,
restaura las ranuras, atributos, vida y estados originales de habilidades antes
de informar del fallo; el inventario no se modifica. La recuperación no vuelve
a ejecutar efectos que podrían fallar de nuevo. No se ejecutan eventos de combate.

Al desaparecer la última fuente de una habilidad de equipo se retira primero su
efecto temporal activo y después se elimina la habilidad. Las habilidades que
siguen aprendidas o concedidas por otra pieza conservan su efecto, nivel y estado.
La limpieza reutiliza la misma operación que el cierre de combate de Instinto de
supervivencia, Ímpetu, Primer impulso, Furia contenida y Rabia; no simula un evento
de fin de combate. Cada habilidad consulta su indicador/contador real, por lo que
limpiar un efecto inactivo no resta bonificaciones.

El intercambio guarda también los indicadores y contadores de EstadoHabilidad. Si
falla después de limpiar, restaura tanto los atributos como dichos indicadores;
la curación o limpieza posterior retira el efecto exactamente una vez. Caso
corregido: vida 1/100, base 6, arma +1 con Instinto, poción pequeña e intercambio
por arma +2 sin habilidad; la fuerza efectiva queda en 8, no en 11, y el siguiente
combate no deja residuos. No cambian los valores de curación ni sus eventos.

Los datos temporales de esas cinco habilidades pertenecen al EstadoHabilidad de
cada combatiente. Una misma instancia externa de Habilidad puede concederse a
dos personajes sin compartir activaciones ni contadores. Cada personaje conserva
una sola entrada por clase, con sus niveles aprendidos y contribuciones de equipo.
No se copian habilidades ni cambian sus referencias, fórmulas o eventos. Las llamadas
directas a eventos deben reutilizar el estado correspondiente, como hace el motor.
La manipulación directa del mapa sigue siendo un API de bajo nivel: no debe insertarse
el mismo EstadoHabilidad mutable en dos personajes.

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

El menú de exploración muestra nombre, vida actual/máxima, barra y oro en un panel Unicode.
Continuar inicia directamente otro combate; no hay tiradas de mazmorra.

Revisar inventario muestra únicamente las pociones disponibles, sus cantidades y
su recuperación máxima. Si no hay, indica «No tienes consumibles». También muestra
el equipo realmente equipado, sus bonificaciones y las habilidades que otorgue,
sin volver a aplicar efectos. Añade la sección EQUIPAMIENTO GUARDADO, con ranuras,
nombres, bonificaciones y habilidades de las piezas almacenadas. Consultar no cambia el estado.

El inventario ofrece `1. Comparar equipamiento`, `2. Cambiar equipamiento` y `0. Volver a exploración`.
Para comparar se elige primero una pieza equipada, numerada en orden de TipoEquipo,
y después una guardada de la misma ranura. Las categorías incompatibles no aparecen
en esa selección. `0` en la selección de guardada vuelve a la equipada; `0` en la
equipada vuelve al inventario. Tras comparar se puede elegir otra candidata o volver.
Inventarios vacíos, ausencia de equipo o de piezas compatibles se explican con
mensajes. Entradas inválidas repiten la elección; el cierre de entrada termina
la sesión sin modificar el personaje.

Jugador almacena el equipo en una List<Equipo> independiente de las ranuras y las
pociones. agregarEquipo conserva la instancia y admite duplicados sin límite de
capacidad, peso ni espacios. getInventarioEquipo devuelve una vista no modificable.
Guardar no equipa ni aplica efectos; desequipar tampoco incorpora automáticamente
la pieza a esa lista. El intercambio explícito sí conserva la pieza anterior;
no hay descarte de piezas ni inventario genérico.
Una nueva aventura crea un jugador con esa lista vacía.

La comparación reutiliza Equipo.compararCon: ACTUAL es la equipada, NUEVO la
guardada y DIFERENCIA = nueva - actual. Muestra la unión de atributos en orden de
Atributo, sin modificar equipo, atributos, habilidades, cantidades ni piezas.
La firma compararCon(Equipo) sigue disponible y produce salida sin colores;
la sobrecarga compararCon(Equipo, PrintStream, boolean) permite elegir salida y ANSI.

Los colores están desactivados por defecto. Solo se colorean los valores de
DIFERENCIA: positivos en verde, negativos en rojo y cero sin color. Cada valor
coloreado lleva restauración inmediata; los espacios de alineación y las demás
columnas permanecen normales. Para una consola que admita ANSI, se puede habilitar:

```shell
java -Dviajedragos.ansi=true -cp target/classes org.viajedragos.app
```

En IntelliJ se utiliza `-Dviajedragos.ansi=true` en las opciones de la VM. El flujo
de aventura solo aplica esa opción a System.out; una salida arbitraria no recibe
ANSI automáticamente. Las pruebas pueden habilitar/deshabilitar explícitamente
el booleano de consultarInventario. No se detecta soporte ANSI ni se asume que exista.

**Limitaciones:** solo la Espada de hierro tiene la nueva oferta para guardar.
La Espada desgastada mantiene su oferta de equipar/rechazar. No hay tiendas,
cofres, economía, botín aleatorio de equipo, nuevas armaduras ni habilidades.

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
`2. Usar objeto` y `3. Huir`. El submenú conserva `1. Poción pequeña`, `2. Poción grande` y
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
Atacar; elegir 2 o 3 es inválido y no consume su acción. No hay conversión forzada.
El modo automático siempre ataca y nunca consume pociones ni intenta huir.

## Huida del combate

Seleccionar `3. Huir` realiza el intento inmediatamente, sin confirmación. Usa
exactamente dos tiradas del TiradorDado existente: escapa si
`velocidad efectiva del jugador + 1D6 > velocidad efectiva del enemigo + 1D6`.
Incluye equipo y efectos activos; un empate falla y no se repite. No hay límites
adicionales de probabilidad: con igual velocidad, 15 de las 36 parejas dan éxito.

El éxito devuelve JUGADOR_HUYE y termina inmediatamente, sin ataque del jugador,
acción enemiga pendiente ni eventos finales de ese turno. Sí ejecuta la limpieza
alFinalCombate existente. No es victoria ni derrota y no concede botín o espada.
Conserva la vida, inventario, equipo y habilidades, retirando los efectos temporales
que deban restaurarse. Regresa a exploración con el mismo personaje, sin curación
automática. Continuar crea un nuevo enemigo; el abandonado no se conserva.

El fallo consume una acción sin ataque del jugador, sin eventos de su ataque y
sin penalizaciones ni daño adicional. El enemigo conserva su acción pendiente,
pero no recibe otra si ya actuó. Eventos finales de turno, iniciativa, muertes y
resurrecciones siguen su recorrido habitual. No hay persecución ni huida enemiga;
alHuir y alPerseguir permanecen sin conectar.

La Espada desgastada se ofrece una sola vez tras la primera victoria efectiva:
si huyes del encuentro 1 y ganas el 2, entonces aparece su oferta. Aceptarla o
rechazarla marca la oferta como realizada. Una aventura nueva tiene su propia oferta.

Si muere el Aventurero, incluso si mueren ambos, se muestra el resultado y se
regresa al menú inicial. Terminar voluntariamente la aventura también descarta
el personaje junto con su equipo e inventario. Una nueva aventura comienza con
otro Aventurero a 48/48, sin espada ni pociones, y puede conseguir sus propias recompensas.

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

Los valores de Aventurero se definen en `app.ejecutar` y los enemigos en
`RepositorioEnemigosMemoria`. `app.jugarAventura` conserva el jugador recibido, crea un nuevo
enemigo/Combate por encuentro y deja la limpieza de efectos a alFinalCombate.
`app.ofrecerEspada` crea una pieza por oferta y utiliza únicamente jugador.equipar;
no modifica fuerza directamente ni llama a aplicarEfectos manualmente.
El primer combate es de aprendizaje: ambos comienzan sin habilidades. Su
adquisición mediante progresión, recompensas o equipamiento se definirá más adelante.
Esta demo ofrece Atacar, usar las dos pociones y Huir en combate. Otros
consumibles, cofres, tiradas de mazmorra y desplazamiento quedan pendientes.
El comercio actual permite únicamente las cuatro compras descritas arriba.

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
instancias compartidas y reglas especiales. La suite completa pasa con 751 pruebas.
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
publicados en `e363c95`. El uso de pociones durante el combate está publicado en
`f97e7fc`. La huida y la oferta de espada tras la primera victoria real están
publicadas en `40463bd`. El catálogo de enemigos y el equilibrio inicial están
publicados en `a630e26`. El catálogo de objetos está publicado en `d796ffc`.
El inventario, la comparación y la obtención/intercambio son cambios locales actuales;
no se ha hecho commit ni push de esta fase.

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
2 a 4. La fase de pociones en combate conservó los 369 casos anteriores y añadió 29:
25 en CombateConsolaTest (37 en total) y 4 en ExploracionTest (21 en total).
Verifican ambas pociones, sustitución de ataque, orden/eventos/estados, recuperación
real, cancelación/rechazo, muerte/resurrección, limpieza, participantes no Jugador,
modo automático, interrupciones y continuidad hacia exploración. Los dados de
combate y el generador de botín se controlan por separado.

La fase de huida conserva los 398 casos previos, cambiando solo el 3 utilizado
como inválido del menú principal por 4. Añade 18 casos a CombateConsolaTest
(55 en total) y 6 a RecompensaTest (17 en total). Se comprueban las 36 parejas,
empates, velocidad efectiva, iniciativa, ausencia de ataques/eventos adicionales,
enemigo antes/después, limpieza, muerte/resurrección, interrupción y continuidad
sin premios por escapar. Aceptar/rechazar la espada tras huir y ganar no repite
oferta ni botín. La verificación de consola usa entradas y dados controlados.

La fase de enemigos conservó los 422 casos anteriores y añadió 30: 15 en
RepositorioEnemigosTest, 14 en CatalogoAventuraTest y 1 simulación de equilibrio
con seis candidatos. Los cinco archivos previos de pruebas de aplicación inyectan
un repositorio solo-Lobo y un Random de encuentros separado. AppTest, AventuraTest
y dos escenarios de RecompensaTest que arrancan aventuras adaptan vida 24 a 48
y secuencias letales al ajuste autorizado; no cambian los datos de los jugadores
construidos expresamente por otros escenarios. Ninguna prueba se elimina o deshabilita.

La fase del catálogo de objetos conservó sin modificar las 452 pruebas anteriores y añadió 49:
18 en RepositorioObjetosTest y 31 en CatalogoObjetosIntegracionTest. Comprueban
datos/ids/validación, colecciones protegidas, armas independientes, repositorios
alternativos, carga una vez por aventura, inventario separado, espada voluntaria
persistente y única, consumo con los valores efectivos en ambos menús, curación
parcial/completa, eventos/estado/nivel, Instinto con equipo, modo automático y
aventura nueva. Resultado real de `mvn clean test`: BUILD SUCCESS; 501 pruebas,
0 fallos, 0 errores y 0 omitidas. Se recompilan 41 fuentes principales y 35 de pruebas.

La fase de inventario y comparación conservó las 501 pruebas anteriores sin modificarlas y añadió 37:
InventarioEquipoTest (8), ComparacionEquipoTest (9) y ComparacionInventarioTest (20).
Comprueban capacidad sin tope artificial (hasta 10.000 inserciones), duplicados,
identidad de instancias, protección de colecciones, categorías, navegación/cierre,
ausencia de efectos al comparar, orden y diferencias, ANSI limitado a su columna,
activación explícita en la aventura y continuidad sin cambiar las recompensas.
Resultado real final: `mvn clean test`, BUILD SUCCESS; 538 pruebas, 0 fallos,
0 errores, 0 omitidas; 41 fuentes principales y 38 archivos de pruebas recompilados.

La fase de obtención/intercambio conserva los 538 casos y añade 34: 15 en
IntercambioEquipoTest, 18 en ObtencionCambioEquipoTest y un caso de catálogo sin
espada de hierro. Se adaptan entradas para rechazar la segunda recompensa en los
escenarios anteriores y el inválido de inventario pasa de 2 a 3. Se conserva toda
aserción previa salvo el tamaño del catálogo de armas, que pasa justificadamente
de una a dos; se añaden comprobaciones de la nueva definición. Los repositorios
alternativos deben proporcionar también espada_hierro, sin respaldos silenciosos.
Se prueban fallos parciales de aplicar/retirar efectos y retornos falsos, identidad
de piezas/estados, habilidades aprendidas y compartidas, cancelación/EOF, equipar
en vacío, ofertas únicas, huida/muerte simultánea y continuidad/reinicio.
Resultado final: mvn clean test, BUILD SUCCESS; 572 pruebas, 0 fallos, 0 errores,
0 omitidas; 41 fuentes principales y 40 archivos de pruebas recompilados.

La corrección posterior del efecto temporal persistente conserva intactos los
572 casos y añade 29: RetiradaEfectosTemporalesTest (26) y
CancelacionEfectoTemporalTest (3). Incluyen el defecto exacto, cinco habilidades
activas/inactivas, fallos tras aplicar/retirar, indicadores internos restaurados,
vida/base/equipo/estados, habilidad aprendida/compartida, clave equivalente de otra
instancia, cancelación y repetición sin acumulación. Resultado final de
`mvn clean test`: BUILD SUCCESS; 601 pruebas, 0 fallos, 0 errores y 0 omitidas;
41 fuentes principales y 42 archivos de pruebas recompilados.

El aislamiento posterior del estado temporal conserva los 601 casos y añade 23
en AislamientoHabilidadesTest. Comprueba la misma instancia externa entre dos
combatientes, las cinco habilidades, aprendizaje y equipo, varias piezas, limpieza
independiente, ataques reales, intercambio exitoso y fallido, restauración de vida,
atributos y estados, y ciclos sin acumulación. Se adapta solo la preparación de
ImpetuTest y PrimerImpulsoTest para conservar el mismo EstadoHabilidad entre eventos;
no se eliminan casos ni se cambian sus aserciones. Resultado de mvn clean test:
BUILD SUCCESS; 624 pruebas, 0 fallos, 0 errores y 0 omitidas; 41 fuentes principales
y 43 archivos de pruebas recompilados. Sin cambios de disparo ni secuencia de combate.

La fase de oro y comercio (`88934c5`) conserva los 624 casos y añade 67:
OroCompraTest (33), RepositorioTiendaTest (16) y ComercioAventuraTest (18).
La presentación unificada conserva esos 691 casos y añade 32: PanelConsolaTest
(23) e InterfazConsolaTest (9). Se adaptan las comprobaciones de textos antiguos
a los paneles, columnas y opciones `[n]`, sin eliminar pruebas funcionales.
TextoConsola extrae el contenido para esas regresiones; las pruebas visuales usan
la salida original, comprobando bordes, centrado, ajuste, ANSI, barras y flujos.
Resultado real de `mvn clean test`: BUILD SUCCESS; 723 pruebas, 0 fallos,
0 errores y 0 omitidas; 46 fuentes principales y 49 archivos de pruebas.

La fase de ritmo conserva esos 723 casos y añade 28: RitmoConsolaTest (9) y
RitmoAventuraTest (19), sin esperas reales. Comprueban velocidades, configuración,
interrupción, persistencia de sesión, paneles completos, menús inmediatos, ataque
enemigo, fallo/contraataque, victoria/derrota, ambas espadas, botín, modo automático
y equivalencia de texto y resultados con ritmo instantáneo. Solo se cambia a 9
una entrada inválida del menú principal en AventuraTest, porque 2 ahora configura
el ritmo; se conservan sus aserciones. Resultado: mvn clean test, BUILD SUCCESS;
751 pruebas, 0 fallos/errores/omitidas; 47 fuentes principales y 51 archivos de pruebas.

La venta conserva los 751 casos anteriores y añade 42: VentaObjetosTest (28) y
VentaConsolaTest (14). Solo se adaptan entradas de ComercioAventuraTest e
InterfazConsolaTest al nuevo submenú de compra, conservando sus aserciones.
Se verifican precios y redondeo, venta unitaria, ambas ubicaciones de armas,
cancelación/EOF/inválidos, catálogo alternativo y ambiguo, objetos inexistentes,
duplicaciones, desbordamiento y recuperación tras fallos, conservación de habilidades
y otras piezas, saldo mostrado y alineación. Resultado de `mvn clean test`:
BUILD SUCCESS; 793 pruebas, 0 fallos, 0 errores y 0 omitidas; 47 fuentes principales
y 53 archivos de pruebas. Sin cambios de combate, recompensas o probabilidades.

La preparación de repositorios externos conserva los 793 casos y añade 25:
RepositoriosAlternativosTest (12) e IntegracionCatalogosTest (13), sin modificar
pruebas anteriores. Verifican datos nuevos, carga única de enemigos/objetos,
combate, compra/venta con precios alternativos, configuración predeterminada,
definiciones equivalentes, catálogos vacíos e inválidos y errores de los orígenes.
Resultado de `mvn clean test`: BUILD SUCCESS; 818 pruebas, 0 fallos, 0 errores y
0 omitidas; 49 fuentes principales y 55 archivos de pruebas. No se implementa
ningún acceso a archivos, bases de datos ni servicios externos.

La progresión añade 49 casos: ProgresionJugadorTest (34) y ProgresionAventuraTest
(15). Comprueban umbrales, sobrantes, subidas múltiples, atributos/vida, reparto,
equipo, limpieza temporal, datos externos, resultados sin victoria, recompensas,
EOF, estado, paneles y pausas sin esperas reales. La única adaptación de las
pruebas anteriores actualiza los valores esperados de EXP en el catálogo, sin
alterar sus comprobaciones de estadísticas. Resultado de `mvn clean test`:
BUILD SUCCESS; 867 pruebas, 0 fallos, 0 errores y 0 omitidas; 49 fuentes principales
y 57 archivos de pruebas.

Los encuentros por nivel añaden 31 casos: EncuentrosPorNivelTest (16),
NivelesEnemigosTest (12) y EquilibrioVariantesTest (3, con 18.000 simulaciones).
Cubren validación, datos sin escalado, compatibilidad de constructores, selección
de todos los elegibles, desbloqueo dentro de una aventura, catálogos alternativos,
vacíos/sin elegibles, EXP, paneles y compatibilidad con el motor. Se conservan las
867 pruebas anteriores, adaptando las expectativas del catálogo a siete entradas,
las comprobaciones iniciales a los cuatro de nivel 1 y el jugador de la prueba
automática al nivel mínimo de cada enemigo. Sin aserciones eliminadas.
Resultado de `mvn clean test`: BUILD SUCCESS; 898 pruebas, 0 fallos, 0 errores y
0 omitidas; 49 fuentes principales y 60 archivos de pruebas.

Equipamiento general añade 63 casos: EquipoGeneralTest (25),
CatalogoEquipoGeneralTest (17) y EquipoGeneralConsolaTest (21). Se conservan los
899 casos que tenía el estado local al iniciar esta fase, adaptando solo la
aceptación de VIDA, diez productos, ranuras vacías, el nuevo válido 3 del menú y
la compatibilidad de venta para armas antiguas sin ID. Se comprueban todas las
ranuras, acumulación/ciclos, vida máxima/curación/niveles, ejemplares, habilidades,
recuperación ante fallos, compra/venta y repositorio alternativo con menús inmediatos.
Resultado de `mvn clean test`: BUILD SUCCESS; 962 pruebas, 0 fallos, 0 errores y
0 omitidas; 50 fuentes principales y 63 archivos de pruebas.
