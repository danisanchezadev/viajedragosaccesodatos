# Viaje a Dragos

Demo Java de una aventura de combates consecutivos por consola, con datos en memoria.
Requiere JDK 23 y Maven. No utiliza persistencia ni servicios externos.

## Ejecutar desde IntelliJ

1. Abre el proyecto e importa `pom.xml` como proyecto Maven.
2. Selecciona JDK 23 como SDK del proyecto.
3. Abre `src/main/java/org/viajedragos/app.java` y ejecuta su método `main`.
4. En el menú inicial escribe `1` e introduce el nombre del personaje (1–30 caracteres).
   Se recortan los espacios exteriores; `0` cancela y vuelve al menú inicial.
5. La aventura comienza en exploración, en **planta 1 — El vestíbulo**. Escribe
   `3` para explorar y encontrar un enemigo de esa planta.
6. Cuando llegue la acción de tu personaje, escribe `1` para atacar, `2` para
   utilizar una poción que hayas obtenido en encuentros anteriores o `3` para huir.
7. Tras la primera victoria, encontrarás la **Espada desgastada**: `1` la equipa
   y `2` la rechaza definitivamente. Aporta **Fuerza +1** y ninguna habilidad.
8. Exploración ofrece `1` Menú de personaje, `2` Cambiar de planta, `3` Explorar,
   `4` Tienda, `5` Guardar y `0` Salir del juego. Guardar solo informa de que no
   está implementado y vuelve a exploración. Los consumibles y pergaminos siguen
   accesibles en Menú de personaje → Inventario; no se guarda ningún archivo.
9. Tras la segunda victoria real se ofrece la **Espada de hierro**, FUERZA +2:
   `1` la guarda y `2` la deja atrás. Para equiparla, entra en inventario y elige
   `2. Cambiar equipamiento`, selecciona ranura/pieza y confirma con `1`.

El menú inicial ofrece `[1] Nueva aventura`, `[2] Velocidad de narración` y `[0] Salir del juego`. Cada aventura crea un
Aventurero con 48/48 de vida, sin habilidades ni equipamiento. Cada encuentro crea
un enemigo nuevo del catálogo de la planta, sin filtro por nivel del jugador,
sin escalado automático de atributos ni límite de
encuentros. Al continuar se conserva la vida restante del Aventurero, salvo los
incrementos de una subida de nivel; no hay curación automática por continuar. La espada fija se ofrece
después de la primera victoria real y no vuelve a ofrecerse durante esa aventura,
aunque se rechace. Huir no cuenta como victoria. Además, cada
enemigo derrotado definitivamente puede dejar pociones. El Aventurero empieza con
15 monedas y recibe 5/8/15/24/35 por victoria definitiva según la planta 1–5
del enemigo derrotado; huir o perder no concede oro. El Capitán recibe 8.

## Botín aleatorio de equipamiento

Cada victoria definitiva, incluido el Capitán de barracones, realiza **una tirada
independiente del 20 %** (`nextInt(100) < 20`). Puede conceder como máximo una pieza
aleatoria, además de las ofertas fijas y demás recompensas existentes. Huir, perder,
morir ambos o interrumpir el combate no consulta ese generador. La victoria se
procesa una sola vez; abrir menús o volver a mostrar datos no repite el botín.

El catálogo contiene **22 definiciones de equipo**: conserva las 11 anteriores,
sin modificar sus atributos ni precios, y añade 11. Cada ranura tiene las tres calidades:

| Calidad | Definiciones | Plantas de botín |
|---|---|---|
| Básico | Espada desgastada; Armadura/Casco/Guantes de cuero; Botas ligeras; Anillo sencillo; Amuleto sencillo | 1–2 |
| Mejorado | Espada de hierro | 2–4 |
| Mejorado | Espada de acero; Armadura reforzada | 3–4 |
| Mejorado | Casco de hierro; Guantes reforzados; Botas de marcha; Anillo de vigilancia; Amuleto de vigor | 2–4 |
| Superior | Espada de la forja | 4–5 |
| Superior | Armadura de acero; Casco de acero; Guantes de acero; Botas veloces; Anillo de reflejos; Amuleto vital | 4–5 |

| Tipo | Básico | Mejorado | Superior |
|---|---|---|---|
| Arma | Espada desgastada | Espada de hierro / de acero | Espada de la forja |
| Armadura | Armadura de cuero | Armadura reforzada | Armadura de acero |
| Casco | Casco de cuero | Casco de hierro | Casco de acero |
| Guantes | Guantes de cuero | Guantes reforzados | Guantes de acero |
| Botas | Botas ligeras | Botas de marcha | Botas veloces |
| Anillo | Anillo sencillo | Anillo de vigilancia | Anillo de reflejos |
| Amuleto | Amuleto sencillo | Amuleto de vigor | Amuleto vital |

Bonificaciones y valoraciones nuevas, provisionales y centralizadas en los catálogos:

| ID | Bonificaciones | Compra de referencia | Venta |
|---|---|---:|---:|
| casco_hierro | RESISTENCIA +2 | 20 | 10 |
| guantes_reforzados | FUERZA +2 | 24 (solo botín) | 12 |
| botas_marcha | AGILIDAD +1, VELOCIDAD +1 | 22 | 11 |
| anillo_vigilancia | INICIATIVA +2 | 24 | 12 |
| amuleto_vigor | VIDA máxima +6 | 25 | 12 |
| armadura_acero | RESISTENCIA +4 | 50 | 25 |
| casco_acero | RESISTENCIA +3 | 40 | 20 |
| guantes_acero | FUERZA +3 | 45 | 22 |
| botas_veloces | AGILIDAD +2, VELOCIDAD +2 | 45 | 22 |
| anillo_reflejos | INICIATIVA +3 | 45 | 22 |
| amuleto_vital | VIDA máxima +10 | 50 | 25 |

VIDA aumenta únicamente el máximo: equipar no cura. Los Guantes reforzados no se
compran en ninguna planta, pero su valoración global permite venderlos en todas.
No hay habilidades nuevas ni cambios en pergaminos, mejoras o fórmulas.
El [análisis de equilibrio](ANALISIS_EQUILIBRIO.md) recoge perfiles, comparaciones
antes/después, expediciones con desgaste, costes y limitaciones. Distingue los
ajustes aplicados en fase 4 de las comprobaciones manuales todavía pendientes.

Los siete tipos tienen oportunidades reales de aparición desde planta 1.
`DefinicionBotinEquipo` enlaza una `DefinicionEquipo` existente con calidad y rango;
no duplica estadísticas. `RepositorioObjetos` proporciona esos metadatos y una
consulta por planta/calidad. El arranque valida IDs únicos y definiciones coincidentes.
Datos inválidos rechazan el catálogo; plantas sin botín se avisan una vez sin bloquear
los combates. Una tirada favorable sin definiciones elegibles no entrega sustitutos.

`BotinEquipamiento` ejecuta por separado aparición, calidad ponderada, definición
uniforme dentro de esa calidad y mejoras disponibles. Solo participan calidades
con definiciones elegibles para la planta. Esta tabla centralizada contiene pesos,
no porcentajes: se normalizan entre las calidades que estén disponibles.

| Nivel enemigo | Básico | Mejorado | Superior |
|---:|---:|---:|---:|
| 1 | 10 | 2 | 1 |
| 2 | 10 | 3 | 2 |
| 3 | 10 | 4 | 3 |
| 4 | 10 | 5 | 4 |
| 5 | 10 | 6 | 5 |
| 6 | 10 | 7 | 6 |
| 7 | 10 | 8 | 7 |
| 8 | 10 | 9 | 8 |
| 9 | 10 | 10 | 9 |
| 10 | 10 | 11 | 10 |
| 11 | 10 | 12 | 11 |
| 12 o más | 10 | 13 | 12 |

Por ejemplo, en planta 2 la calidad mejorada pasa de 4/14 con enemigo de nivel 3
a 5/15 con nivel 4. En planta 4 la superior pasa de 8/17 en nivel 8 a 9/19 en nivel 9.
La aparición sigue siendo siempre 20 %. Un nivel alto no habilita calidad superior
antes de planta 4 ni utiliza el nivel del jugador. Si solo hay una calidad disponible,
no hay otra que ponderar: actualmente ocurre en plantas 1, 3 y 5. Planta 5 tiene
siete definiciones superiores, una por ranura. Los conjuntos elegibles son 7, 13,
8, 15 y 7 piezas respectivamente; las calidades son B, B/M, M, M/S y S.

| Planta | Mejoras disponibles, uniformes e inclusivas |
|---|---|
| 1 | 0–1 |
| 2 | 0–2 |
| 3 | 1–2 |
| 4 | 1–3 |
| 5 | 2–3 |

Se usa `crearEquipo(int mejoras)`, sin un segundo sorteo del constructor. Cada
pieza conserva ID/nombre/tipo/bonos y tiene estado independiente. Las mejoras no
se aplican ni consumen pergaminos al recibirla. `Jugador.agregarEquipo` la guarda
sin equipar, sustituir ranuras ni agrupar nombres iguales. El panel RECOMPENSAS
incluye su tipo, nombre, bonos y mejoras, aprovechando la pausa ya existente.
Inventario, inspección, mejoras, equipar/desequipar y venta siguen sus flujos actuales.

La venta usa el catálogo global de tienda y `precioCompra / 2`, incluso en otra
planta o después de mejorar la pieza. Todos los objetos de botín actuales ya tienen
valoración; no se han añadido precios ni ofertas comerciales. Un objeto externo
sin valoración válida puede obtenerse y usarse, pero no venderse.

El azar de equipo es un `Random` independiente, creado una vez por sesión y
reutilizado entre aventuras/encuentros; las sobrecargas completas permiten inyectarlo.
Las firmas anteriores siguen disponibles. Las pruebas antiguas proporcionan un
generador que falla la aparición, conservando sus aserciones sin botín inesperado;
las nuevas controlan cada sorteo. No se añaden archivos de datos ni persistencia.

## Experiencia y subida de nivel

El Aventurero empieza en nivel 1, con 0 EXP y sin puntos pendientes. Cada victoria
definitiva concede la EXP definida en los datos del enemigo, además de las cinco
monedas, el botín y las ofertas de armas existentes. En planta 1, Rata cavernaria,
Murciélago, Slime y Goblin conceden 5 EXP; Esqueleto concede 6 EXP.
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

`Planta.oroVictoria()` centraliza 5/8/15/24/35 monedas para plantas 1–5.
El panel de recompensas muestra el importe realmente sumado; no depende del nivel
del jugador. La victoria definitiva se procesa una sola vez, incluido el Capitán.
Los precios de las 22 piezas y dos pociones, las ventas y el botín no cambian.

La opción `[4] Tienda` abre la tienda de la planta actual. Su menú
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
| Armadura reforzada | 35 | 17 |
| Espada de acero | 35 | 17 |
| Espada de la forja | 50 | 25 |

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
por ese ID y por nombre, ranura, bonificaciones originales y ausencia de habilidades adicionales;
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

## Plantas, encuentros y enemigos

La aventura empieza en exploración, en planta 1. Solo `[3] Explorar` inicia un
encuentro normal: selección uniforme entre las definiciones de la planta actual,
excluyendo guardianes especiales. No se filtra por nivel del jugador. `[6] Enfrentarse
al guardián` aparece únicamente en planta 2 mientras no haya sido derrotado.

`Planta` define ID estable, número, nombre y descripción. `ProgresoAventura` guarda
ubicación, desbloqueos, victorias, IDs de especies, guardián y fragmentos, solo en
memoria y por aventura. Los desbloqueos se conservan al subir y bajar. No hay saltos:
`[2] Cambiar de planta` permite subir una, bajar una o cancelar. Desde planta 1 no
se sube y desde planta 5 no se baja: «Todavía no se han desarrollado más plantas».

El panel de exploración muestra el progreso compacto: victorias 0–3/3, guardián
pendiente/derrotado, especies 0–3/3, fragmentos 0–3/3 o última planta disponible.
Consulta el mismo `ProgresoAventura`, sin pausas. La victoria que cumple un requisito
anuncia la siguiente planta en RECOMPENSAS una sola vez, sin descender. Las visitas
posteriores y victorias adicionales no repiten el aviso.

| Planta | Ubicación | Requisito para descender |
|---|---|---|
| 1 | El vestíbulo | Tres victorias; las especies pueden repetirse |
| 2 | Los barracones | Derrotar al Capitán de barracones |
| 3 | Las criptas | Tres IDs de especie diferentes |
| 4 | La forja olvidada | Tres fragmentos: uno por victoria, máximo tres |
| 5 | El abismo | No existe una sexta planta |

Solo la victoria definitiva registra progreso; huida, derrota o muerte simultánea
no cuentan. El número de encuentro impide repetir un registro. Los fragmentos son
progreso, no objetos. Encuentros normales y guardián comparten el bloque original
de oro, EXP, pergaminos y pociones, así como las ofertas únicas en victorias 1 y 2.
No cambian probabilidades ni reglas del motor de combate.

El catálogo en memoria contiene exactamente estas 25 definiciones; Goblin y
Esqueleto conservan sus datos. Cada fila indica FUERZA/RESISTENCIA/INICIATIVA/
AGILIDAD/VELOCIDAD; los números son provisionales y están en el repositorio.

| Planta | ID | Enemigo | Nivel | F/R/I/A/V | PS | EXP |
|---|---|---|---:|---|---:|---:|
| 1 | rata_cavernaria | Rata cavernaria | 1 | 2/0/2/1/2 | 10 | 5 |
| 1 | murcielago | Murciélago | 1 | 2/0/3/2/3 | 10 | 5 |
| 1 | esqueleto | Esqueleto | 1 | 3/2/1/1/1 | 14 | 6 |
| 1 | slime | Slime | 1 | 2/1/1/0/1 | 12 | 5 |
| 1 | goblin | Goblin | 1 | 3/1/2/2/3 | 12 | 5 |
| 2 | soldado_corrupto | Soldado corrupto | 3 | 6/3/3/2/3 | 24 | 15 |
| 2 | arquero_espectral | Arquero espectral | 3 | 6/2/4/3/4 | 22 | 15 |
| 2 | sabueso | Sabueso | 3 | 6/2/4/3/4 | 22 | 15 |
| 2 | armadura_animada | Armadura animada | 4 | 6/4/2/1/2 | 28 | 18 |
| 2 | capitan_barracones | Capitán de barracones | 4 | 11/7/5/5/3 | 35 | 25 |
| 3 | momia | Momia | 5 | 18/9/4/6/2 | 32 | 18 |
| 3 | arana_cripta | Araña de cripta | 5 | 18/8/6/7/5 | 28 | 18 |
| 3 | espectro | Espectro | 6 | 19/9/6/7/4 | 30 | 20 |
| 3 | necrofago | Necrófago | 6 | 20/10/5/6/3 | 34 | 22 |
| 3 | sacerdote_oscuro | Sacerdote oscuro | 6 | 20/9/6/7/4 | 38 | 24 |
| 4 | golem_hierro | Gólem de hierro | 8 | 26/14/5/7/2 | 48 | 26 |
| 4 | salamandra | Salamandra | 8 | 25/12/8/9/5 | 36 | 24 |
| 4 | herrero_maldito | Herrero maldito | 8 | 26/13/6/8/3 | 42 | 26 |
| 4 | centinela_igneo | Centinela ígneo | 9 | 27/13/7/9/4 | 44 | 28 |
| 4 | demonio_forja | Demonio de forja | 9 | 28/14/8/8/4 | 46 | 30 |
| 5 | caballero_abisal | Caballero abisal | 11 | 32/17/8/11/4 | 54 | 36 |
| 5 | sombra_ancestral | Sombra ancestral | 11 | 31/16/10/13/6 | 48 | 36 |
| 5 | bestia_vacio | Bestia del vacío | 12 | 33/17/9/12/5 | 58 | 40 |
| 5 | hechicero_abisal | Hechicero abisal | 12 | 32/16/10/13/5 | 52 | 40 |
| 5 | guardian_abismo | Guardián del abismo | 12 | 34/18/10/12/5 | 60 | 45 |

El Guardián del abismo es un encuentro normal en esta fase. Solo el Capitán tiene
`guardian=true`. Las plantas profundas elevan fuerza, resistencia y PS sin inflar
los combates mediante habilidades nuevas. Se puede descender con un jugador aún
débil: entrenar en plantas anteriores sigue siendo útil. No hay escalado dinámico
ni garantía de sobrevivir sin preparación. La fase 4 ajusta ataque/defensa/iniciativa
de Capitán y plantas 3–5, conservando sus PS, velocidad y nivel; reduce la EXP
profunda, sin cambiar la fórmula de subida. El motor interactivo se mide con tres
perfiles por planta, pociones, habilidades existentes y expediciones. B gana
78,8 %/73 %/62,6 % de aislados en plantas 3/4/5, pero solo sobrevive 39 %/31 %/14 %
de cuatro encuentros consecutivos. Contra el Capitán, B de N2 gana 51 %; C puede
dominar con entrenamiento. No son garantías para cualquier reparto o inventario.
Las tablas y las semillas están en ANALISIS_EQUILIBRIO.md; falta validación manual
en IntelliJ, especialmente duración del Capitán y rentabilidad por minuto.

`RepositorioEnemigos.obtenerTodos()` devuelve definiciones inmutables; `app` carga
una instantánea por aventura. Cada enemigo creado tiene vida completa y estado
independiente. Los constructores anteriores conservan planta 1 y `guardian=false`;
los nuevos orígenes deben indicar planta (1–5), guardián, EXP y nivel positivos.
IDs nulos/repetidos, datos inválidos o varios guardianes especiales rechazan el
catálogo. Un catálogo vacío impide iniciar; si una planta no tiene enemigos para
la acción solicitada se avisa y se vuelve a exploración sin tirar dados ni premiar.
`ConfiguracionRepositorios` sigue seleccionando las implementaciones externas.

### Tiendas por planta

El catálogo global conserva un precio por objeto. `CatalogoTiendaMemoria` asocia
IDs a cada planta; no duplica definiciones ni la lógica de compra. Todos los
precios anteriores permanecen iguales. Las dos pociones conservan 8 y 16 PS.

| Planta | Compra disponible |
|---|---|
| 1 | Poción pequeña, Espada desgastada, Armadura de cuero, Casco de cuero |
| 2 | Ambas pociones, Espada de hierro, Guantes de cuero, Botas ligeras, Casco de hierro, Botas de marcha |
| 3 | Ambas pociones, Espada de acero, Armadura reforzada, Casco de hierro, Botas de marcha, Anillo de vigilancia, Amuleto de vigor |
| 4 | Ambas pociones, Espada de acero, Armadura reforzada, Botas de marcha, Anillo de vigilancia, Amuleto de vigor, Armadura de acero, Casco de acero |
| 5 | Ambas pociones, Espada de la forja, Armadura de acero, Casco de acero, Guantes de acero, Botas veloces, Anillo de reflejos, Amuleto vital |

El catálogo global tiene 24 productos: dos pociones y las 22 piezas de equipo.
Conserva los 13 precios anteriores; solo las 11 incorporaciones tienen precios nuevos.
Las compras respetan la planta y la venta usa siempre la valoración global,
también para botín exclusivo o piezas encontradas en otra planta.

`obtenerPorPlanta(int)` amplía RepositorioTienda de forma compatible: sin sobrescribirlo,
un repositorio anterior ofrece su lista en todas las plantas. `paraPlanta(int)`
valida una selección sin duplicados ni productos ajenos al catálogo y proporciona
la selección para comprar. La venta usa siempre el repositorio global y su precio
entero `precioCompra / 2`, aunque el objeto se adquiriese en otra planta.

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

Inventario: `[1] Comparar equipo`, `[2] Equipar objeto`, `[3] Desequipar objeto`,
`[4] Perg. atributos`, `[5] Perg. habilidad`, `[6] Perg. maestría`,
`[7] Usar consumible`, `[8] Inspeccionar`, `[0] Volver`. Cambiar exige elegir una pieza compatible y confirmar. Desequipar
muestra las ocupadas y solicita confirmación; devuelve el mismo ejemplar al final
del inventario. Cancelar/EOF no confirman ni retiran efectos. La selección para
equipar conserva las siete ranuras; la lista principal muestra solo piezas poseídas.
No se añaden pausas en estos menús.

El catálogo comercial global tiene 24 productos; conserva los precios de las
cuatro ofertas iniciales y de las seis piezas básicas de la tabla. Las ofertas
por planta se detallan arriba. Pueden comprarse, equiparse, desequiparse y
venderse, incluidas equipadas. La venta usa ID y contenido para no valorar una
pieza desconocida o modificada como otra definición. El botín usa las mismas definiciones.
Los intercambios y ventas reutilizan la recuperación segura de ranuras, atributos,
vida y estados de habilidades. Los pergaminos y las mejoras individuales se
describen a continuación; la calidad pertenece a los metadatos de botín y no se
conceden habilidades desde las definiciones generales.

## Pergaminos y mejoras individuales

Cada ejemplar nuevo de las siete categorías recibe una sola tirada `nextInt(4)`:
0, 1, 2 o 3 mejoras disponibles, con igual probabilidad. Se genera al crear la pieza
inicial, comprada o encontrada; nunca al mostrar ofertas, equipar, desequipar,
comparar o vender. Las definiciones siguen inmutables y sin puntos de mejora.
Dos ejemplares del mismo modelo pueden tener distintos puntos y mejoras añadidas.

Cada victoria definitiva hace una comprobación del 10 % para obtener como máximo
un pergamino. Si tiene éxito, elige uniformemente FUERZA, RESISTENCIA, INICIATIVA,
AGILIDAD, VELOCIDAD o VIDA máxima. Hay una tirada `nextInt(100) < 10` y solo en caso
de éxito otra `nextInt(6)`. Su generador es independiente del de pociones, encuentros
y dados, y se conserva durante la sesión. Derrotas, muerte simultánea, huida o
combates interrumpidos no hacen tiradas. La comprobación ocurre antes del reparto
de nivel para que EOF en esa selección no omita una recompensa de una victoria real.
El oro, EXP, las dos tiradas de pociones y las ofertas de armas conservan sus reglas.

Los seis pergaminos se nombran automáticamente por atributo y mejoran siempre +1.
Se almacenan por cantidad en Jugador sin límite artificial y no pertenecen a
RepositorioObjetos ni RepositorioTienda: no se compran ni venden. Nueva aventura:
nuevo inventario vacío. Se conservan mientras se utiliza el mismo personaje.

En inventario, `[4] Utilizar pergaminos`: elegir pergamino, seleccionar una pieza
equipada o guardada y confirmar. La vista previa muestra sus bonificaciones, el
atributo antes/después y los puntos disponibles antes/después. `[0]` retrocede un
nivel; cancelar, inválidos o EOF no consumen nada. Con cero puntos no se admite mejora.
Tras éxito se muestra el resultado y se vuelve a acciones de inventario. No hay
pausas en las selecciones; solo al obtener realmente un pergamino como recompensa.

`Jugador.usarPergamino(atributo, pieza)` es la operación de consumo: verifica
propiedad, cantidad y puntos; aplica exactamente +1 y consume un punto y una unidad.
Una pieza equipada actualiza inmediatamente solo ese modificador, sin retirar ni
volver a aplicar habilidades. La recuperación segura existente restaura al portador
ante fallos; la pieza se modifica solo después de comprobar el incremento activo.
Los mapas de bonos originales, adicionales y totales siguen protegidos desde fuera.
Las mejoras permanecen al mover la pieza y al retirar sus efectos se descuenta su
bonificación total. Mejorar VIDA máxima no cura, y los +6 PS por nivel permanecen.

La venta identifica la definición original y compara los bonos originales:
las mejoras adicionales no alteran `precioCompra / 2`. Vender equipada retira
todos sus efectos y solo el ejemplar seleccionado. Las habilidades de armas
y sus estados no cambian por mejorar otro atributo.

Para pruebas deterministas existen `crearEquipo(int/Random)`, `crearArma(int/Random)`,
el constructor de Equipo/Arma con puntos explícitos y `Jugador.comprar(..., Random)`.
La sobrecarga de app conserva el Random de pergaminos de atributos. Los
constructores anteriores siguen disponibles y generan puntos normalmente.

## Pergaminos de habilidades y maestría

Cada victoria definitiva realiza tres comprobaciones independientes: atributo
10 %, habilidad 5 % y maestría 2 %. Puede entregar los tres tipos juntos, una
unidad como máximo de cada uno. No hay tiradas por derrota, muerte simultánea,
huida o combate incompleto. El oro, EXP, pociones y ofertas de armas no cambian.

`PergaminoHabilidad` fija una `HabilidadDisponible` al obtenerse. El contenido no
se vuelve a sortear al consultar el inventario. Los iguales se acumulan por
cantidad; la maestría es un contador separado sin habilidad preseleccionada.
Ambos inventarios pertenecen a Jugador y una aventura nueva empieza sin ellos.
No se compran ni venden y no intervienen los repositorios de objetos o tienda.

`HabilidadDisponible` enumera las 13 implementaciones jugables, sin duplicados,
en orden estable, tanto para el sorteo uniforme como para elegir una maestría.
Excluye el borrador no jugable FuriaMaximaBerserker. Primer impulso se construye
con su magnitud original 3 y Segundo aliento con 2. No se alteran los efectos:
Caparazón consulta el nivel efectivo; las habilidades que tienen magnitudes
internas fijas siguen utilizándolas aunque aumente su nivel registrado.

En inventario se conservan las opciones 0–4. `[5] Utilizar pergamino de habilidad`
permite seleccionar uno de los contenidos disponibles; `[6] Utilizar pergamino
de maestría` permite elegir entre las 13 habilidades. La confirmación muestra
nombre, aprendizaje previo, nivel permanente antes/después y contribución de
equipo/nivel efectivo cuando existe. Cancelar, entradas inválidas o EOF no
consumen. Los menús no incluyen pausas; las recompensas realmente obtenidas sí.

`Jugador.usarPergaminoHabilidad` y `usarPergaminoMaestria` aprenden un nivel
permanente y consumen exactamente una unidad tras comprobar el resultado.
`EstadoHabilidad` conserva el total efectivo y el nivel permanente; la diferencia
es la aportación del equipo. Por ejemplo, equipo 2 + pergamino = permanente 1,
efectivo 3; al desequipar queda 1. Permanente 2 + equipo 2 + pergamino = permanente
3, efectivo 5; al desequipar queda 3. Varias piezas siguen siendo una única
habilidad con varias contribuciones. No se ejecutan eventos al aprender ni se
modifican atributos, vida o EXP. Los efectos temporales activos conservan su
estado y se limpian en sus momentos habituales.

Los desbordamientos se comprueban antes de cambiar niveles/cantidades. El consumo
reutiliza la recuperación segura de Combatiente; ante un fallo restaura mapa,
instancias, niveles, estados temporales y atributos sin consumir. La copia de
EstadoHabilidad incluye el nivel permanente. Se mantienen constructores y firmas
anteriores; para construir un estado mixto directamente existe
`EstadoHabilidad(aprendida, vecesEquipada, nivelEfectivo, nivelPermanente)`.
El constructor antiguo de tres argumentos no puede inferir el reparto y considera
permanente todo el nivel si aprendida es true. Para incorporar habilidades al
juego se deben usar aprenderHabilidad/agregarHabilidadEquipo, no editar el mapa.

Las nuevas tiradas usan un Random independiente del pergamino de atributos,
pociones, encuentros y dados. La sobrecarga completa de ejecutar/jugarAventura
añade `Random azarHabilidades` después de `Random azarPergaminos`; se conserva
durante encuentros y aventuras. Cada victoria consulta nextInt(100) < 5, solo
si tiene éxito nextInt(13) para el contenido, y otra nextInt(100) < 2 para maestría.
Las sobrecargas anteriores siguen disponibles. Las secuencias del 10 % no cambian.

## Nombre y estado del personaje

Una nueva aventura solicita un nombre de 1–30 caracteres Unicode, recortando
espacios exteriores. Rechaza nombres vacíos, caracteres de control, separadores
de línea y caracteres de formato que alteren la consola. La opción `0` cancela;
EOF termina la sesión sin crear un jugador ni consultar catálogos o dados.
El nombre elegido se conserva entre encuentros. Cada aventura puede usar otro.

Exploración ofrece `[1] Menú de personaje`, que integra estado e inventario.
Exploración ofrece además Cambiar de planta, Explorar, Tienda, Guardar pendiente y Salir. El menú general
usa 100 columnas interiores (102 contando bordes); las demás ventanas conservan
48. Cabecera: nombre, nivel, oro y tiempo de aventura. Vida y EXP comparten fila
con barras proporcionales; los puntos pendientes aparecen cuando existen. Los seis
atributos muestran solamente valores efectivos en dos filas, sin desglose ni barras.

Entre atributos e inventario, HABILIDADES resume hasta seis habilidades activas,
ordenadas por nombre y sin duplicados. `[A]` indica aprendizaje permanente,
`[E]` procedencia exclusiva del equipo y `[A+E]` aprendizaje reforzado por equipo;
`Nv.` muestra directamente el nivel efectivo de EstadoHabilidad. Tres columnas
permiten dos filas cuando los nombres caben; se reducen a dos o una para mostrar
nombres y niveles completos. Si es necesario se ajusta el texto en varias líneas,
sin abreviarlo. Vacío: `Sin habilidades`; más de seis: `+N habilidades más`.
La opción `[6] Habilidades` conserva la consulta completa. El resumen se reconstruye
con el estado real al regresar, también después de equipar o desequipar.

Debajo aparecen CONSUMIBLES, PERGAMINOS y EQUIPAMIENTO en tres columnas, con las
cantidades alineadas a la derecha. Se muestran como máximo cinco entradas por
categoría, `+N objetos más` y `Sin objetos` cuando corresponde. Los nombres largos
se abrevian con `…` únicamente en este resumen. `[E]` distingue piezas equipadas;
dos piezas con el mismo nombre siguen siendo instancias independientes, cada una
con cantidad visual uno. Las acciones nunca identifican objetos por su abreviación.

Acciones: `[1] Estadísticas`, `[2] Inventario`, `[3] Usar objeto`, `[4] Equipar`,
`[5] Desequipar`, `[6] Habilidades`, `[0] Volver a exploración`. Estadísticas muestra
base actual, modificadores netos de equipo y total efectivo en columnas. Los cinco
atributos usan `getAtributoBase`/`getAtributo`; VIDA usa `getVidaMaximaBase` y
`getVidaMaxima`. Las consultas no recalculan ni modifican los valores del jugador.

La sección de habilidades aprendidas muestra nivel permanente, equipo y total.
Las exclusivas de equipo aparecen aparte, con nivel y piezas otorgantes cuando
están presentes en las ranuras. Las aprendidas con contribuciones de equipo no
se repiten. Se consultan los niveles de EstadoHabilidad sin recalcularlos ni
disparar eventos. Equipar, sustituir, mejorar y desequipar actualizan la siguiente
consulta sin mantener copias de datos en la presentación.

Limitación del modelo existente: las habilidades temporales modifican el mapa
base. Si hay efectos activos, la pantalla advierte que muestra la base actual
con esos efectos, no una base permanente reconstruida. La consulta no retira ni
reaplica efectos. No cambia fórmulas ni estadísticas; tampoco consume recursos,
turnos o tiradas, ni introduce pausas narrativas.

La presentación utiliza barras `█`/`░`: vida y experiencia tienen diez
segmentos proporcionales a sus máximos reales, redondeados al segmento más próximo.
Se acotan entre vacío y completo; con máximo no positivo quedan vacías. La
división se realiza antes del escalado, sin multiplicaciones de enteros grandes.
La vista general no incluye barras individuales de atributos ni un resumen
adicional de modificadores. La entrada anterior `consultarEstado` se conserva
para compatibilidad, con su presentación compacta, sin duplicarla en exploración.

Con `-Dviajedragos.ansi=true` y salida a la consola, el menú de personaje utiliza
fondo azul y texto claro, vida verde, EXP cian y bonos con signo en verde/rojo.
Cada línea restablece ANSI y el fondo se recupera después de colores parciales.
Sin activación explícita no se emiten escapes. Para terminales estrechos se puede
usar `-Dviajedragos.columnas=80`: presentación de 48 columnas interiores y categorías
verticales. No se detecta automáticamente el ancho; al abrir el menú ancho aparece
un aviso con esta alternativa. Los valores numéricos nunca se recortan.

`VistaMenuPersonaje` construye las vistas sin leer entradas ni modificar modelos;
`app` coordina las acciones existentes. `TiempoAventura` mide desde el inicio de
cada aventura con `System.nanoTime()`, incluye combates y menús, y muestra
`HH:MM:SS` incluso por encima de 99 horas. Consultar no acumula tiempo de nuevo;
una aventura nueva crea otro contador. Se actualiza al redibujar el menú, sin
hilos ni refresco asíncrono. El reloj es inyectable en pruebas y no se persiste.

## Equilibrio inicial

Solo cambia la vida del Aventurero de consola: antes 24/24, ahora 48/48.
Fuerza 6, resistencia 2, iniciativa 3, agilidad 2 y velocidad 2 permanecen iguales.
La fuerza efectiva pasa a 7 únicamente si el jugador acepta la espada. Jugador
conserva sus reglas de atributos y las fórmulas del motor no cambian; tampoco hay curación automática.

Como regresión histórica, EquilibrioAventuraTest utiliza el motor real con los cuatro enemigos anteriores en un catálogo exclusivo de pruebas, siempre Atacar, cuatro encuentros
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

El menú de exploración conserva nombre, vida actual/máxima, barra y oro en un panel Unicode; añade la planta actual.
Explorar inicia un combate de la planta actual; la aventura comienza en este menú. No hay tiradas de mazmorra.

Menú de personaje → `[2] Inventario` abre un panel con CONSUMIBLES, PERGAMINOS, EQUIPAMIENTO
y ACCIONES, en ese orden. Muestra solo cantidades positivas y piezas reales.
Pociones: nombre efectivo del catálogo y cantidad. Pergaminos: atributo, contenido
de habilidad o maestría agrupados por cantidad. Equipamiento: una entrada por
pieza, sin agrupar nombres repetidos; `[E]` identifica las equipadas. Las entradas
de hasta 21 columnas se emparejan; las largas usan todo el ancho y se ajustan sin
recortarse. Vacíos: «Sin consumibles», «Sin pergaminos», «Sin equipamiento».

Se conservan los números 0–6 y sus submenús de comparación, cambio, desequipado y
los tres pergaminos. Se añade 7 para usar una poción con el flujo existente y 8
para inspeccionar cualquier pieza equipada o guardada: tipo, bonos, habilidades
y mejoras aplicadas/disponibles mediante mostrarEquipo. En inspección, 0 vuelve un nivel;
no equipa, consume ni modifica las piezas. Las acciones van debajo del contenido,
en dos columnas. No se añaden pausas ni se alteran los colores de comparación.
Al regresar de cada operación se reconstruye el panel con los datos actuales.
El inventario ampliado muestra ocho entradas por categoría y página, sin límite
de capacidad: `[9] Página siguiente` y `[10] Página anterior` aparecen cuando
corresponde. La selección de inspección también se pagina, conservando nombres
completos e identidad de cada pieza. Estadísticas y habilidades se consultan
desde el menú general. Ningún menú introduce pausas narrativas.

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

Las ofertas fijas se conservan: la Espada de hierro se guarda o rechaza y la
Espada desgastada se equipa o rechaza. El botín aleatorio se guarda sin equiparse;
las tiendas y la economía usan los catálogos descritos arriba. No hay cofres.

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
regresa al menú inicial. `[0] Salir del juego` cierra la sesión y no guarda
el personaje ni sus pertenencias. `[5] Guardar` solo avisa y mantiene la aventura.
Una nueva aventura comienza con
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
El primer combate es de aprendizaje: ambos comienzan sin habilidades. Después
pueden aprenderse permanentemente mediante los pergaminos de recompensa.
Esta demo ofrece Atacar, usar las dos pociones y Huir en combate. Otros
consumibles, cofres, tiradas de mazmorra y desplazamiento quedan pendientes.
El comercio permite comprar y vender los productos de su catálogo; no admite pergaminos.

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

Las habilidades reales se comprueban con combatientes de prueba y también pueden
aprenderse mediante pergaminos en la aventura. No se asignan habilidades iniciales
al Aventurero ni al primer enemigo, ni se añade narración de activaciones.
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

Pergaminos añade 84 casos: MejorasPergaminosTest (49), BotinPergaminosTest (17) y
PergaminosConsolaTest (18), conservando los 962 anteriores. Enumeración exacta del
10 % y seis atributos, límites de generación, siete categorías, aplicación guardada
y equipada, consumo/agotamiento, fallos parciales/no aplicación, desbordamiento,
vida/niveles, habilidades, venta al precio original, recompensas, EOF y navegación.
Cuatro pruebas de consola anteriores se adaptan para controlar la nueva fuente de
botín y sustituir el inválido 4 por 5. En la comparación instantáneo/normal se
normaliza solo el nuevo contador aleatorio de mejoras; se comparan todos los textos
previos y resultados. No se eliminan casos ni aserciones de juego.
Resultado de `mvn clean test`: BUILD SUCCESS; 1046 pruebas, 0 fallos, 0 errores y
0 omitidas; 51 fuentes principales y 66 archivos de pruebas.

Pergaminos de habilidades y maestría añade 100 casos: PergaminosHabilidadesTest
(59), BotinHabilidadesTest (21) y PergaminosHabilidadesConsolaTest (20). Cubren
las probabilidades exactas, selección uniforme y fija, ocho combinaciones, ausencia
de botín sin victoria, continuidad, los cuatro casos de aprendizaje, equipo,
efectos temporales, combate, desbordamientos y recuperación, consumo, cancelación,
EOF y alineación. Se conservan los 1046 anteriores; cinco archivos se adaptan
únicamente para controlar el nuevo generador o sustituir el inválido 5 por 7.
Resultado final de `mvn clean test`: BUILD SUCCESS; 1146 pruebas, 0 fallos, 0 errores
y 0 omitidas; 53 fuentes principales y 69 archivos de pruebas. Sin esperas reales.

Nombre y estado añade 34 casos: NombrePersonajeTest (22) y EstadoPersonajeTest
(12). Se verifican nombre/validación/cancelación/EOF, continuidad, navegación,
alineación, atributos base/equipo, penalizaciones, mejoras, nivel, vida máxima,
habilidades aprendidas y otorgadas, retirada de contribuciones y consulta sin
mutaciones ni pausas. Doce archivos de pruebas existentes incorporan únicamente
la nueva entrada del nombre; mantienen las aserciones de comportamiento.
Resultado final de `mvn clean test`: BUILD SUCCESS; 1180 pruebas, 0 fallos,
0 errores y 0 omitidas; 53 fuentes principales y 71 archivos de pruebas.

El rediseño visual de estado conserva los 1180 casos anteriores y añade 28
en EstadoPersonajeTest (40 en total): barras/extremos/escala, máximos efectivos,
EXP tras subir, resumen neto y mejoras, valores grandes, nombres y habilidades
largos, muchas habilidades, ANSI explícito/propiedad, alineación y consulta sin
mutaciones. Se adaptan solo las aserciones de presentación afectadas.
Verificación: `mvn clean test`, BUILD SUCCESS; 1208 pruebas, 0 fallos, 0 errores
y 0 omitidas; 53 fuentes principales y 71 archivos de pruebas.

Rediseño de inventario: InventarioPresentacionTest añade 21 casos y se conservan
los 1210 anteriores. Se adaptan nueve archivos de pruebas a los nuevos textos,
la opción inválida 9, la inspección detallada y la consulta separada de estado,
sin quitar las comprobaciones de negocio. Se cubren categorías, cantidades,
instancias, siete ranuras, columnas, textos largos, ANSI, refresco de acciones,
cancelación/EOF y el ajuste de opciones de PanelConsola.
`mvn clean test`: BUILD SUCCESS; 1231 pruebas, 0 fallos, 0 errores y 0 omitidas;
53 fuentes principales y 72 archivos de pruebas.

Menú horizontal de personaje: se conservan esos 1231 casos y se añaden 37,
MenuPersonajeTest (25) y TiempoAventuraTest (12). Cubren inventarios vacíos y
poblados, cinco filas/omitidos, cantidades alineadas, nombres largos, piezas
repetidas, pergaminos diferentes, datos efectivos, reloj controlado durante
combates y menús, navegación, acciones, cancelación/EOF, paginación, ANSI,
paneles de 48/100 columnas, restauración de configuración de pruebas y ausencia
de mutaciones al consultar. Las pruebas anteriores adaptan entradas y anchuras,
conservando sus comprobaciones funcionales.
Verificación final: `mvn clean test`, BUILD SUCCESS; **1268 pruebas**, 0 fallos,
0 errores y 0 omitidas; 55 fuentes principales y 74 archivos de pruebas.

Resumen de habilidades en el menú general: HabilidadesMenuPersonajeTest añade
13 casos y conserva los 1268 anteriores sin modificaciones. Comprueba vacío,
procedencias y niveles, varias fuentes de equipo y retirada, entradas inactivas,
seis habilidades en dos filas, exceso y consulta detallada, nombres largos,
valores grandes, ancho compacto, ANSI y ausencia de mutaciones.
`mvn clean test`: BUILD SUCCESS; **1281 pruebas**, 0 fallos, 0 errores y
0 omitidas; 55 fuentes principales y 75 archivos de pruebas.

Plantas y tiendas por ubicación: conserva los 1281 casos anteriores, adaptando
entradas al menú inicial de exploración, tienda 4 y catálogo nuevo. Los escenarios
de aventuras nuevas pasan a sesiones consecutivas al desaparecer la opción de
terminar voluntariamente; mantienen generadores y comprobaciones de independencia.
Las regresiones históricas de daño/equilibrio usan datos fijos exclusivos de pruebas.
Se añaden 44 casos: PlantasTest (16), PlantasConsolaTest (10) y TiendasPorPlantaTest
(18), incluidos recorrido completo, cuatro desbloqueos, guardián, huidas, muertes,
EOF, Guardar sin persistencia, compras por planta, venta entre plantas y catálogos
externos. Las 25 definiciones también se comprueban con el motor real y dados controlados.
Verificación: `mvn clean test`, BUILD SUCCESS; **1325 pruebas**, 0 fallos, 0 errores
y 0 omitidas; 57 fuentes principales y 79 archivos de pruebas. `git diff --check`
sin errores. No se ha comprobado visualmente esta fase en IntelliJ.

Botín aleatorio de equipo: las 1325 pruebas anteriores reciben un azar de equipo
controlado, conservando sus aserciones. Se añaden 70 casos: BotinEquipamientoTest
(33) y BotinEquipamientoConsolaTest (37). Cubren los 100 resultados de aparición,
pesos y límites, siete tipos, rangos de mejoras, independencia de piezas y catálogo,
repositorios alternativos/vacíos/inválidos, combinaciones con pergaminos y pociones,
guardián, muerte/resurrección, continuidad, inspección, mejoras, equipamiento y venta.
`mvn clean test`: BUILD SUCCESS; **1395 pruebas**, 0 fallos, 0 errores y 0 omitidas;
60 fuentes principales y 81 archivos de pruebas. Sin esperas reales. `git diff --check`
sin errores. No se ha realizado commit ni push.

Equipamiento, equilibrio y progreso (fase 3): conserva las 1395 pruebas y añade
53: CatalogoProgresivoTest (35), EquilibrioPlantasTest (6), siete selecciones
exhaustivas por planta/calidad, independencia completa de dos botines, resumen
de cinco objetivos, dos recorridos de consola y venta de botín exclusivo.
Se adaptan únicamente expectativas de tamaño de catálogo, cantidades elegibles
y presentación; los trece precios y once definiciones previos se comprueban
explícitamente. No se eliminan casos ni se debilitan aserciones anteriores.
`mvn clean test`: BUILD SUCCESS; **1448 pruebas**, 0 fallos, 0 errores y 0 omitidas;
60 fuentes principales y 83 archivos de pruebas. `git diff --check` sin errores.
Sin esperas reales ni cambios en combate, probabilidades o recompensas. No se ha
realizado commit ni push. La comprobación visual en IntelliJ sigue pendiente.

Equilibrio de fase 4: conserva las 1448 pruebas y añade 82 (BalanceProgresionTest 10,
DatosBalanceTest 42, RecompensasPorPlantaTest 30). Incluye tres perfiles por planta,
comparación del motor interactivo antes/después, expediciones sin restaurar vida,
consumo/compra real de pociones, habilidades y limpieza, datos fijos de los 25
enemigos, legalidad del equipo, resultados deterministas y recompensas en las cinco
plantas. Solo dos casos anteriores adaptan importes/EXP autorizados, conservando
sus comprobaciones; el recorrido completo verifica cada panel de recompensa.
`mvn clean test`: BUILD SUCCESS; **1530 pruebas**, 0 fallos, 0 errores y 0 omitidas;
60 fuentes principales y 88 archivos de pruebas. `git diff --check` sin errores.
Sin esperas reales, commit ni push. Sigue pendiente la validación manual en IntelliJ.
