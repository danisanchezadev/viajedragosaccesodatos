# Guía de integración de repositorios — Viaje a Dragos

El juego sigue utilizando exclusivamente colecciones Java en memoria. Esta guía
define los puntos que otro desarrollador podrá implementar con archivos, JDBC,
JPA u otra tecnología. No hay conexiones, formatos de archivo ni dependencias de
persistencia implementados. No es necesario modificar combate, inventario o
reglas comerciales para cambiar el origen de los datos admitidos.

## Contratos existentes

Todos están en `src/main/java/org/viajedragos/catalogo`.

| Interfaz | Métodos que implementar | Datos devueltos |
|---|---|---|
| `RepositorioEnemigos` | `List<DefinicionEnemigo> obtenerTodos()` | Definiciones de enemigos |
| `RepositorioObjetos` | `List<DefinicionArma> obtenerArmas()` y `List<DefinicionPocion> obtenerPociones()`; opcional `List<DefinicionEquipo> obtenerEquipamiento()` | Definiciones de objetos; el método nuevo devuelve adicionales, sin repetir armas |
| `RepositorioTienda` | `List<ProductoTienda> obtenerTodos()` | Ofertas y precios de compra |

`RepositorioTienda.buscarPorId(String)` ya tiene implementación predeterminada
mediante el listado. Se puede sobrescribir para usar un índice; debe devolver
`Optional.empty()` si el ID no existe y el mismo contenido que figura en la lista
si existe. `validarProductos` es una utilidad estática; no hay que implementarla.

Los modelos son records en `org.viajedragos.modelo`:

- `DefinicionEnemigo`: ID, nombre, descripción, fuerza, resistencia, iniciativa,
  agilidad, velocidad, vida máxima, experiencia positiva de recompensa y nivel
  (entero >= 1), planta (1–5) y marca de guardián especial (solo planta 2).
  `crearEnemigo()` crea un combatiente nuevo con esos mismos datos de combate;
  el repositorio no debe devolver ni conservar enemigos de partida mutables.
- `DefinicionArma`: ID, nombre y `Map<Atributo, Integer>` de bonificaciones.
  Copia y protege el mapa. `crearArma()` devuelve una instancia nueva, sin habilidades.
- `DefinicionEquipo`: ID, nombre, `TipoEquipo` y mapa protegido de bonificaciones.
  `crearEquipo()` crea una pieza independiente con ID de catálogo. Si es ARMA crea
  una Arma; en las otras ranuras, Equipo. VIDA representa bonificación de vida máxima.
- `DefinicionPocion`: ID, `TipoPocion`, nombre y recuperación de PS.
- `ProductoTienda`: exactamente una definición de equipo o poción, más el precio de
  compra. ID y nombre proceden de esa definición; no crear una segunda descripción
  del mismo objeto con atributos diferentes. `precioVenta()` calcula `precio / 2`
  mediante división entera, sin modificar el precio de compra.
  Los constructores anteriores con DefinicionArma siguen disponibles; `equipo()`
  es el acceso común para todo equipamiento. `arma()` se conserva para productos
  creados desde DefinicionArma; no usarlo para decidir si un producto es equipamiento.

## Dónde seleccionar las implementaciones

Editar únicamente los retornos de estos tres métodos de
`src/main/java/org/viajedragos/catalogo/ConfiguracionRepositorios.java`:

```java
public static RepositorioEnemigos enemigos() {
    return new RepositorioEnemigosMemoria();
}

public static RepositorioObjetos objetos() {
    return new RepositorioObjetosMemoria();
}

public static RepositorioTienda tienda(List<DefinicionArma> armas,
                                     List<DefinicionPocion> pociones,
                                     List<DefinicionEquipo> equipos) {
    return new RepositorioTiendaMemoria(armas, pociones,
            equipos.toArray(DefinicionEquipo[]::new));
}
```

`app.main()` llega a esta configuración mediante las entradas predeterminadas de
`app.ejecutar`. Los métodos de prueba y sobrecargas anteriores siguen disponibles;
un repositorio proporcionado explícitamente no se sustituye por el predeterminado.
No hay registro global mutable, contenedor, reflexión ni selección por archivos.
La sobrecarga anterior `tienda(armas, pociones)` se mantiene para compatibilidad;
la aventura utiliza la nueva de tres listas. Para sustituir el origen comercial
en el flujo actual, editar esa nueva sobrecarga.

Los enemigos y objetos se leen una vez al iniciar cada aventura y se copian antes
de usarlos. La tienda predeterminada se crea después de cargar los objetos: recibe
esas mismas definiciones, sin releer su origen. Un repositorio comercial propio
puede usar las listas recibidas para resolver sus referencias por ID.

Los repositorios deben devolver datos coherentes durante la aventura. El listado
y `buscarPorId` comercial deben concordar; las consultas comerciales pueden
repetirse al abrir la tienda o realizar operaciones. No hay actualización en vivo,
reintentos ni una transacción entre orígenes externos. Mantener la carga en los
métodos de lectura, sin efectuar trabajo de acceso a datos en el constructor.

## Ejemplo mínimo alternativo

Este ejemplo solo muestra el contrato; no resuelve ninguna lectura de archivos
ni conexión a base de datos:

```java
package ejemplo;

import java.util.List;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.modelo.DefinicionEnemigo;

public final class MisEnemigos implements RepositorioEnemigos {
    @Override
    public List<DefinicionEnemigo> obtenerTodos() {
        return List.of(new DefinicionEnemigo(
                "enemigo_ejemplo", "Enemigo de ejemplo", "Datos de prueba",
                2, 1, 1, 1, 1, 8, 5, 1));
    }
}
```

Después, `ConfiguracionRepositorios.enemigos()` devolvería
`new ejemplo.MisEnemigos()`. No se cambia `Combate` ni su sistema de dados.
Para objetos o tienda, seguir sus interfaces de la misma forma. Las pruebas
`RepositoriosAlternativosTest` contienen ejemplos completos únicamente en `src/test`.

## Datos y construcción de la tienda

`CatalogoTiendaMemoria` conserva las cuatro ofertas iniciales y sus precios:
pequeña 5, grande 10, espada desgastada 12 y espada de hierro 20. Resuelve las
definiciones recibidas, sin duplicarlas. Añade las seis piezas de memoria si se
proporcionan entre las adicionales: armadura/casco/botas a 8, guantes/anillo/amuleto
a 10 monedas. También conserva acero 35, forja 50 y armadura reforzada 35, y añade
las once valoraciones de fase 3 descritas al final: 24 productos en total con el
catálogo completo. No asigna precios a IDs desconocidos. `RepositorioTiendaMemoria` se ocupa de
validar, copiar e indexar, sin fijar las ofertas en su lógica de construcción.

Además de los constructores anteriores, admite:

```java
new RepositorioTiendaMemoria(productos, armasCargadas, pocionesCargadas);
// Con definiciones adicionales:
new RepositorioTiendaMemoria(productos, armasCargadas, pocionesCargadas, equiposCargados);
```

Esa lista puede tener otra cantidad de productos, otro orden y otros precios,
siempre que sus definiciones existan en los objetos cargados. Un repositorio propio
que implemente la interfaz no está obligado a usar la clase en memoria.

Al iniciar la aventura, `app` aplica `RepositorioTienda.validarProductos` también
a implementaciones externas y comprueba la coherencia de su búsqueda por ID.
Se compara el contenido completo de los records, no su identidad Java: un origen
puede reconstruir una definición equivalente. Nunca se inventan valores para IDs
desconocidos ni se acepta un mismo ID con estadísticas distintas.

## Validaciones y tratamiento de errores

| Datos | Condiciones |
|---|---|
| Todos los listados | Lista y registros no nulos; preferiblemente listas inmutables |
| IDs y nombres | Obligatorios y no blancos; comparación exacta, sensible a mayúsculas |
| Enemigos | IDs únicos dentro de enemigos; descripción no nula, aunque puede estar vacía; atributos no negativos; vida máxima y experiencia de recompensa positivas; nivel >= 1 |
| Armas y equipo | IDs únicos entre todas las armas, piezas y pociones; tipo obligatorio; mapas, claves y valores no nulos; bonificaciones enteras positivas, negativas o cero (excepto Integer.MIN_VALUE, que no se puede negar al retirarlas); `VIDA` significa vida máxima |
| Pociones | IDs únicos entre objetos; tipo obligatorio y único; recuperación estrictamente positiva |
| Productos | ID único en tienda, heredado del objeto; exactamente una definición; precio de compra entero positivo; definición completa coincidente con el catálogo de objetos |

Los constructores de los records ya validan campos y magnitudes. Las validaciones
de conjunto detectan nulos y duplicados. No se descartan silenciosamente registros
erróneos, ni se sustituyen por objetos o enemigos de respaldo.

- **Enemigos vacíos:** no se inicia la aventura y se vuelve al menú inicial.
- **Planta sin encuentros:** se informa y se vuelve a exploración sin combate,
  tiradas ni recompensas. El nivel del jugador ya no excluye enemigos.
- **Objetos vacíos o incompletos:** no se inicia. Se necesitan las armas con IDs
  `espada_desgastada` y `espada_hierro` por las recompensas existentes, y exactamente
  una definición para cada tipo de poción (`PEQUENA` y `GRANDE`).
- **Tienda vacía:** válida; no hay productos para comprar ni correspondencias para
  vender. Los enemigos y el resto de la aventura siguen disponibles.
- **Nulos, duplicados, referencias inválidas o error de lectura expresado como
  `RuntimeException`:** se rechaza el catálogo al iniciar, con el aviso existente
  y regreso al menú. No se consume entrada, se tira ningún dado ni se inicia combate.
  La configuración de pociones del jugador se aplica solo después de validar todo.
- Las excepciones comprobadas de una tecnología futura deben tratarse dentro de
  su implementación; los contratos actuales no declaran `throws`. No devolver una
  lista vacía para ocultar un error de lectura: tiene un significado funcional distinto.

Los IDs de enemigos forman un espacio separado del de objetos. Los productos
comparten intencionadamente el ID de su objeto; no generan otro identificador.
Los últimos campos de `DefinicionEnemigo` son `int experiencia, int nivel,
int planta, boolean guardian`. Experiencia y nivel se transmiten al enemigo;
planta y guardián se usan en la selección y el progreso de la aventura.
Los constructores anteriores siguen disponibles con planta 1, `guardian=false`,
nivel 1 cuando no se indica y 5 EXP cuando no se especifica recompensa.
Para nuevos orígenes deben usarse los cuatro campos explícitos.
Los enemigos iniciales tienen 5–10 EXP, pero otros datos pueden proporcionar
recompensas mayores. El repositorio no calcula subidas de nivel ni entrega puntos:
la aventura concede la EXP solo ante victoria y Jugador gestiona su progresión.

`RepositorioEnemigos.obtenerTodos()` devuelve el catálogo completo. La aventura
sortea entre `definicion.planta() == plantaActual && !definicion.guardian()`, sin
filtrar por nivel. Los enemigos pueden superar el nivel del jugador; no se escalan
atributos. Se toma una instantánea por aventura y cada encuentro crea un enemigo
independiente. El motor no conoce el repositorio ni las plantas.

Planta debe estar entre 1 y 5. Solo planta 2 admite `guardian=true` y el catálogo
admite como máximo uno: la opción contextual inicia su encuentro y no aparece en
el sorteo. La marca, no el nombre, gobierna ese comportamiento. Un catálogo externo
sin guardián permite combatir pero no completar el descenso de planta 2. No se
exigen cinco enemigos por planta a un repositorio externo; esa distribución
pertenece al catálogo predeterminado. IDs nulos/repetidos o campos inválidos
rechazan el catálogo entero. Los IDs también identifican las especies del progreso.
`ConfiguracionRepositorios` sigue siendo el punto de selección.

Ejemplo de registro externo, sin persistencia:

```java
new DefinicionEnemigo("soldado_prueba", "Soldado de prueba", "",
        6, 3, 3, 2, 3, 24, 15, 3, 2, false);
```

### Asociaciones de productos a plantas

`RepositorioTienda.obtenerTodos()` sigue siendo el catálogo global y
`buscarPorId()` conserva su contrato. `obtenerPorPlanta(int)` devuelve un subconjunto
de esos mismos productos con el mismo precio. Por compatibilidad, su implementación
predeterminada devuelve todos en cualquier planta válida. Se puede sobrescribir
en un repositorio externo sin cambiar compras, ventas ni combate.

`paraPlanta(int)` copia y valida las ofertas (sin nulos, IDs duplicados ni productos
ajenos al catálogo global) y da el repositorio de selección para comprar.
La aplicación usa siempre el catálogo global para vender, incluso objetos que no
se compran en la planta actual. No filtrar ese catálogo para valorar ventas.
La fórmula sigue siendo la división entera `precioCompra / 2`.

En memoria se pueden pasar asociaciones independientes de las definiciones:

```java
new RepositorioTiendaMemoria(productos, armasCargadas, pocionesCargadas,
        equiposCargados, Map.of(1, List.of("espada_desgastada"),
                               3, List.of("espada_desgastada", "espada_hierro")));
```

Un mapa explícito deja vacías las plantas omitidas. Un mapa vacío conserva la
compatibilidad de ofrecer el catálogo completo en todas; para cinco tiendas vacías
con catálogo de valoración, asociar listas vacías a las cinco plantas. Las listas
y el mapa se copian; planta fuera de 1–5, IDs desconocidos/repetidos o listas nulas
se rechazan. Los constructores con ofertas explícitas anteriores siguen disponibles.
El catálogo predeterminado usa asociaciones en `CatalogoTiendaMemoria`, separadas
de la lógica de compra, y mantiene los precios anteriores. Añade acero FUERZA +3
por 35, forja FUERZA +4 por 50 y armadura reforzada RESISTENCIA +3 por 35.

## Añadir equipamiento de cualquier ranura

Implementar opcionalmente el método nuevo de RepositorioObjetos, por ejemplo:

```java
@Override
public List<DefinicionEquipo> obtenerEquipamiento() {
    return List.of(new DefinicionEquipo("botas_externas", "Botas del explorador",
            TipoEquipo.BOTAS, Map.of(Atributo.AGILIDAD, 2, Atributo.VELOCIDAD, 1)),
            new DefinicionEquipo("armadura_externa", "Armadura reforzada",
                    TipoEquipo.ARMADURA, Map.of(Atributo.RESISTENCIA, 3, Atributo.VIDA, 2)));
}
```

Las importaciones proceden de `modelo`, `arma` y `java.util`. El valor por defecto
es List.of(), por lo que los repositorios anteriores no tienen que cambiar. La
lista contiene definiciones adicionales, no las armas ya devueltas por obtenerArmas.
Puede incluir un arma nueva como DefinicionEquipo con tipo ARMA, pero las dos
espadas de recompensa siguen proporcionándose por el contrato anterior.
Todos los IDs de armas, piezas y pociones deben ser únicos entre categorías.

Para vender la definición adicional en una tienda propia, crear
`new ProductoTienda(definicionEquipo, precioCompra)`. Se valida su coincidencia
completa con los datos de RepositorioObjetos mediante la sobrecarga de cuatro
listas de `RepositorioTienda.validarProductos`. La aventura carga cada listado
una vez y pasa las mismas definiciones a la configuración comercial.

Los repositorios no devuelven Equipo ni Arma mutables. Dos llamadas a crearEquipo
producen dos ejemplares distintos con el mismo ID de definición. El inventario y
las ranuras guardan esas referencias; sus posiciones numeradas distinguen los
ejemplares. No se añade ID persistente de ejemplar. Cada creación genera 0–3 puntos
de mejora propios; conservar el ejemplar en el inventario, sin reconstruirlo desde
la definición al consultarlo, moverlo o venderlo, para no perder su estado.

## Definiciones frente a estado de mejora

Los contratos de los tres repositorios no cambian en esta fase. DefinicionEquipo y
DefinicionArma solo contienen los datos originales, inmutables. Crear un ejemplar
genera una vez 0–3 puntos de mejora uniformes, independientemente de su origen.
Las ofertas de ProductoTienda muestran definiciones; no crear piezas para mostrar
precios ni catálogos, porque eso ejecutaría una generación innecesaria.

Equipo guarda separadamente bonos base, mejoras añadidas y puntos disponibles.
Sus mapas se consultan mediante vistas no modificables. Jugador.usarPergamino es
la entrada de negocio: consume exactamente una unidad por +1 y mantiene sincronizado
el modificador del portador si la pieza está equipada. No debe modificarse el mapa
ni retirar/reaplicar habilidades para mejorar un solo atributo. Los cambios no
vuelven al repositorio ni modifican otras piezas creadas con la misma definición.

Pergamino(atributo, cantidad) es un record interno generado por el juego, con
cantidad obligatoriamente 1 y nombre calculado. Los seis atributos incluyen VIDA
como vida máxima. Jugador almacena cantidades por atributo. No añadir pergaminos,
sus cantidades o puntos de mejora a los métodos de RepositorioObjetos ni a productos
comerciales; no hay compra/venta de pergaminos ni persistencia de inventarios.

La venta de una pieza mejorada usa ID y definición original: el precio sigue siendo
la mitad entera del precio de compra, sin premio por mejoras. La comprobación de
correspondencia utiliza getBonificacionesBase(), no el mapa efectivo mejorado.
Los otros datos del modelo y las reglas de piezas antiguas sin ID se conservan.

Para pruebas, crearEquipo(int puntos) y crearArma(int puntos) permiten valores
explícitos 0–3; sus sobrecargas Random exigen una única nextInt(4). Hay constructores
de Equipo/Arma con puntos explícitos y comprar(id, repositorio, Random). El flujo
completo de app permite inyectar al final el generador de pergaminos, independiente
del botín de pociones y de los dados. No se necesitan archivos, configuraciones
globales de pruebas ni bases de datos.

Los pergaminos de habilidad y maestría también son recompensas internas: 5 % y
2 % independientes del 10 % de atributos. No añadirlos a repositorios ni tienda.
HabilidadDisponible es la enumeración estable de las 13 habilidades implementadas;
no es un repositorio y no se carga desde los datos externos. PergaminoHabilidad
guarda el contenido fijado en el sorteo; Jugador acumula cantidades por contenido
y un contador de maestría. No hay cambios en los contratos de repositorios.

La sobrecarga más completa de app añade Random azarHabilidades después del
generador de atributos. Es independiente de los demás y se conserva en la sesión;
las pruebas pueden controlar tanto la probabilidad como el índice uniforme.
EstadoHabilidad distingue nivelPermanente del nivel efectivo sumado con equipo.
La creación normal se realiza mediante aprenderHabilidad/agregarHabilidadEquipo;
para construir estados mixtos explícitos existe el constructor de cuatro
argumentos (aprendida, vecesEquipada, nivelEfectivo, nivelPermanente). El antiguo
asume todo permanente si aprendida es true, pues no recibe el reparto. No crear
ni compartir manualmente estados entre combatientes. El aprendizaje no cambia
las magnitudes internas fijas de las implementaciones de habilidades existentes.

VIDA no es vida actual: se aplica a los modificadores de vida máxima. El máximo
efectivo es base permanente más equipo, limitado a cero; equipar no cura. Quitar
equipo recorta la vida actual solo si supera el nuevo máximo. La curación usa ese
máximo efectivo; los +6 por nivel siguen incrementando una sola vez la base.
Los métodos genéricos de atributos de Combatiente siguen rechazando VIDA; Equipo
utiliza modificarVidaMaximaEquipo para mantener esa separación. El campo histórico
vidaMaxima y su setter representan la base; consultar getVidaMaxima para el total.

## Límites actuales

- Se pueden añadir enemigos con las estadísticas existentes y armas con nuevas
  bonificaciones sin modificar combate, inventario o tienda. Esto no garantiza que
  cualquier combinación esté equilibrada; un combate sin daño puede no terminar.
- Solo hay dos tipos de poción, una definición por tipo en el jugador. Se pueden
  proporcionar nombres y cantidades de curación distintos, pero un tercer tipo o
  efecto requeriría otra fase. No se amplía el enum ni el inventario de consumibles.
- DefinicionArma produce ARMA; DefinicionEquipo admite las siete ranuras actuales,
  sin habilidades desde datos. Las armas construidas con habilidades mediante el
  API anterior conservan su funcionamiento. La calidad se define en los metadatos
  de botín; no escala automáticamente los atributos de las piezas.
- Las ofertas fijas siguen usando los dos IDs obligatorios. El botín aleatorio usa
  metadatos del repositorio de objetos; las cinco tiendas conservan ofertas y valoración global.
- Las piezas de catálogo conservan el ID para venderse, además de comprobar nombre,
  ranura, bonificaciones originales y ausencia de habilidades adicionales. Dos modelos iguales
  con IDs diferentes se distinguen correctamente. Los constructores antiguos sin
  ID conservan la búsqueda estructural única; no usarlos para contenido nuevo.
- Los pergaminos y puntos de mejora están implementados solo en la partida, sin
  cargarse ni guardarse en los repositorios. Solo existen las tres calidades de botín,
  sin potencias mayores que +1 ni más de tres mejoras por ejemplar. El inventario sigue siendo ilimitado.
- No hay persistencia de jugadores, inventarios, oro o partidas. Los repositorios
  describen contenido; no guardan el estado de una aventura. No hay cierre automático
  de conexiones ni contratos de recursos externos implementados.

## Definiciones de botín de equipamiento

`RepositorioObjetos` incorpora dos métodos predeterminados compatibles:

```java
List<DefinicionBotinEquipo> obtenerBotinEquipamiento();
List<DefinicionBotinEquipo> obtenerBotinPorPlanta(int planta, CalidadEquipo calidad);
```

El primero devuelve metadatos de disponibilidad, no piezas mutables. Por defecto
devuelve una lista vacía: un repositorio anterior sigue funcionando y avisa de las
plantas sin botín. El segundo filtra la lista por rango/calidad y devuelve una
lista no modificable. Para otro origen basta con implementar el primero junto a
las definiciones normales; no hay otro repositorio ni un inventario paralelo.

Ejemplo mínimo, usando una definición ya cargada en obtenerEquipamiento:

```java
@Override
public List<DefinicionBotinEquipo> obtenerBotinEquipamiento() {
    return List.of(new DefinicionBotinEquipo(cascoYaCargado,
            CalidadEquipo.MEJORADO, 2, 4));
}
```

Los campos son `DefinicionEquipo equipo`, `CalidadEquipo calidad`, planta mínima
y máxima inclusivas. Para un arma se reutiliza `DefinicionArma.comoEquipo()`, que
conserva ID y bonificaciones; no copiar estadísticas a otro catálogo. Los valores
disponibles son BASICO, MEJORADO y SUPERIOR. Se valida rango 1–5 no invertido,
calidad/equipo no nulos y que MEJORADO no comience antes de planta 2 ni SUPERIOR
antes de planta 4. Un catálogo puede permitir calidad inferior en plantas profundas.

`RepositorioObjetos.validarBotin` verifica IDs únicos y coincidencia completa con
las armas/equipo ya cargados. No admite IDs ajenos, estadísticas contradictorias,
nulos ni registros duplicados. La aventura valida una instantánea al arrancar, sin
volver a consultar datos durante cada victoria. Catálogos vacíos o sin elegibles
se avisan sin bloquear el juego y no se sustituyen por objetos de otra planta.

`BotinEquipamiento` contiene las reglas de selección; README documenta la tabla
PESOS por nivel enemigo (1–12, saturada desde 12), disponibilidad actual y rangos
MEJORAS por planta. Se elige primero calidad entre las disponibles y luego una
definición uniforme. El nivel del jugador no interviene. Si hay una sola calidad,
su selección es obligada; no se desbloquean calidades externas al rango para variar.
Probabilidad de aparición siempre 20 %. Los cuatro sorteos usan el Random inyectado,
independiente de dados, pociones y pergaminos. Las sobrecargas completas de app.ejecutar
y jugarAventura reciben un último `Random azarEquipo`; las entradas anteriores delegan.
La entrada pública conserva ese generador durante la sesión.

La instancia se crea con `equipo.crearEquipo(mejoras)` y se incorpora con
`Jugador.agregarEquipo`. No usar el constructor sin número explícito ni crear piezas
desde el repositorio, pues las mejoras y el estado pertenecen a cada ejemplar.
La compra y el botín comparten definiciones; las ventas valoran el objeto por la
definición global original, sin depender de planta, calidad o mejoras aplicadas.
Una definición externa sin ProductoTienda válido no es vendible: no se inventa precio.

El catálogo de memoria contiene 22 definiciones de equipo: las once anteriores
intactas y once incorporaciones. Cada ranura tiene básico, mejorado y superior;
hay dos armas mejoradas. README contiene sus IDs, bonificaciones y precios.
La disponibilidad se asocia mediante IDs estables, sin depender de índices de listas.
Los datos se editan en RepositorioObjetosMemoria y la valoración/asociación comercial
en CatalogoTiendaMemoria; ninguna regla de combate conoce esos IDs.

| Planta | Calidades | Definiciones elegibles |
|---|---|---:|
| 1 | Básico | 7 |
| 2 | Básico / mejorado | 13 |
| 3 | Mejorado | 8 |
| 4 | Mejorado / superior | 15 |
| 5 | Superior | 7 |

Esta distribución es la configuración actual, no otra validación rígida del contrato
de repositorios: un origen alternativo puede definir otros rangos válidos.
El catálogo comercial global valora 24 productos (22 piezas y dos pociones).
`guantes_reforzados` es botín exclusivo: tiene ProductoTienda con precio 24,
pero no figura en ninguna asociación de compra por planta. Se vende por 12
desde cualquier planta. No excluir del catálogo global los objetos no comprables:
las ventas lo necesitan; únicamente las compras usan `paraPlanta`.
Las demás ofertas y precios figuran en README. No se han cambiado los trece precios
anteriores ni la división entera para vender; los orígenes externos conservan
libertad para suministrar otras definiciones, metadatos, productos y asociaciones.

Los datos y pesos son provisionales. [ANALISIS_EQUILIBRIO.md](ANALISIS_EQUILIBRIO.md)
describe los cálculos, simulaciones y recomendaciones pendientes, separadas de los
cambios aplicados. Se sustituye el origen mediante ConfiguracionRepositorios;
los lectores de archivos y bases de datos siguen pendientes.

## Ejecutar las pruebas

Desde la raíz, con el JDK y Maven configurados como en el proyecto:

```text
mvn clean test
```

`RepositoriosAlternativosTest` prueba carga, combate, compra, venta y rechazo de
errores con dobles locales. `IntegracionCatalogosTest` comprueba configuración,
ofertas extensibles, correspondencias, copias y validación. No necesitan ficheros
de datos, red, bases de datos ni esperas narrativas reales. Maven puede necesitar
descargar sus dependencias si aún no están disponibles en su caché.
