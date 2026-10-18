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
  (entero >= 1). `crearEnemigo()` crea un combatiente nuevo con esos mismos datos;
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
a 10 monedas. No asigna precios a IDs desconocidos. `RepositorioTiendaMemoria` se ocupa de
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
- **Sin enemigos elegibles:** si todos superan el nivel actual del jugador, se
  informa y se vuelve al inicio sin combate, tiradas, lectura de acciones ni recompensas.
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
Los dos últimos campos de `DefinicionEnemigo` son `int experiencia, int nivel`:
se cargan desde los datos y se transmiten al enemigo. Los constructores anteriores
siguen disponibles con nivel 1 y, si no se indica experiencia, 5 EXP por compatibilidad;
para nuevos orígenes deben usarse ambos campos explícitos.
Los enemigos iniciales tienen 5–10 EXP, pero otros datos pueden proporcionar
recompensas mayores. El repositorio no calcula subidas de nivel ni entrega puntos:
la aventura concede la EXP solo ante victoria y Jugador gestiona su progresión.

`RepositorioEnemigos.obtenerTodos()` devuelve el catálogo completo. Al comenzar
cada encuentro, la aventura filtra `definicion.nivel() <= jugador.getNivel()` y
sortea uniformemente entre los elegibles, conservando el orden del listado.
El filtrado se repite después de subir de nivel, sin volver a leer el repositorio.
Los enemigos inferiores siguen disponibles; no se busca ningún ID concreto ni se
impone una lista fija en el motor. Un repositorio alternativo puede devolver otros
niveles, nombres, estadísticas y recompensas. El nivel no calcula ni escala atributos.
Los cuatro enemigos iniciales son de nivel 1; las variantes predeterminadas se
desbloquean en niveles 3, 4 y 5. ConfiguracionRepositorios sigue siendo el punto de selección.

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
ejemplares. No se añaden todavía ID persistente de ejemplar, pergaminos o mejoras.

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
  API anterior conservan su funcionamiento. No hay niveles ni rarezas de equipo.
- Las recompensas siguen usando los dos IDs obligatorios; no hay botín de armas
  genérico ni múltiples tiendas.
- Las piezas de catálogo conservan el ID para venderse, además de comprobar nombre,
  ranura, bonificaciones y ausencia de habilidades adicionales. Dos modelos iguales
  con IDs diferentes se distinguen correctamente. Los constructores antiguos sin
  ID conservan la búsqueda estructural única; no usarlos para contenido nuevo.
- La segunda fase definirá pergaminos, puntos de mejora y bonificaciones individuales.
  No están implementados; actualmente una pieza modificada que no coincide con su
  definición no se vende como si fuera otra. El inventario sigue siendo ilimitado.
- No hay persistencia de jugadores, inventarios, oro o partidas. Los repositorios
  describen contenido; no guardan el estado de una aventura. No hay cierre automático
  de conexiones ni contratos de recursos externos implementados.

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
