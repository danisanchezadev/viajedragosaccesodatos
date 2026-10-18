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
| `RepositorioObjetos` | `List<DefinicionArma> obtenerArmas()` y `List<DefinicionPocion> obtenerPociones()` | Definiciones de objetos |
| `RepositorioTienda` | `List<ProductoTienda> obtenerTodos()` | Ofertas y precios de compra |

`RepositorioTienda.buscarPorId(String)` ya tiene implementación predeterminada
mediante el listado. Se puede sobrescribir para usar un índice; debe devolver
`Optional.empty()` si el ID no existe y el mismo contenido que figura en la lista
si existe. `validarProductos` es una utilidad estática; no hay que implementarla.

Los modelos son records en `org.viajedragos.modelo`:

- `DefinicionEnemigo`: ID, nombre, descripción, fuerza, resistencia, iniciativa,
  agilidad, velocidad, vida máxima y experiencia positiva de recompensa. `crearEnemigo()` crea un combatiente nuevo;
  el repositorio no debe devolver ni conservar enemigos de partida mutables.
- `DefinicionArma`: ID, nombre y `Map<Atributo, Integer>` de bonificaciones.
  Copia y protege el mapa. `crearArma()` devuelve una instancia nueva, sin habilidades.
- `DefinicionPocion`: ID, `TipoPocion`, nombre y recuperación de PS.
- `ProductoTienda`: exactamente una definición de arma o poción, más el precio de
  compra. ID y nombre proceden de esa definición; no crear una segunda descripción
  del mismo objeto con atributos diferentes. `precioVenta()` calcula `precio / 2`
  mediante división entera, sin modificar el precio de compra.

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
                                     List<DefinicionPocion> pociones) {
    return new RepositorioTiendaMemoria(armas, pociones);
}
```

`app.main()` llega a esta configuración mediante las entradas predeterminadas de
`app.ejecutar`. Los métodos de prueba y sobrecargas anteriores siguen disponibles;
un repositorio proporcionado explícitamente no se sustituye por el predeterminado.
No hay registro global mutable, contenedor, reflexión ni selección por archivos.

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
                2, 1, 1, 1, 1, 8, 5));
    }
}
```

Después, `ConfiguracionRepositorios.enemigos()` devolvería
`new ejemplo.MisEnemigos()`. No se cambia `Combate` ni su sistema de dados.
Para objetos o tienda, seguir sus interfaces de la misma forma. Las pruebas
`RepositoriosAlternativosTest` contienen ejemplos completos únicamente en `src/test`.

## Datos y construcción de la tienda

`CatalogoTiendaMemoria` contiene las cuatro ofertas predeterminadas y sus precios:
pequeña 5, grande 10, espada desgastada 12 y espada de hierro 20. Resuelve las
definiciones recibidas, sin duplicarlas. `RepositorioTiendaMemoria` se ocupa de
validar, copiar e indexar, sin fijar las ofertas en su lógica de construcción.

Además de los constructores anteriores, admite:

```java
new RepositorioTiendaMemoria(productos, armasCargadas, pocionesCargadas);
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
| Enemigos | IDs únicos dentro de enemigos; descripción no nula, aunque puede estar vacía; atributos no negativos; vida máxima y experiencia de recompensa positivas |
| Armas | IDs únicos entre todas las armas y pociones; mapas, claves y valores no nulos; bonificaciones enteras positivas, negativas o cero; `VIDA` no admitida |
| Pociones | IDs únicos entre objetos; tipo obligatorio y único; recuperación estrictamente positiva |
| Productos | ID único en tienda, heredado del objeto; exactamente una definición; precio de compra entero positivo; definición completa coincidente con el catálogo de objetos |

Los constructores de los records ya validan campos y magnitudes. Las validaciones
de conjunto detectan nulos y duplicados. No se descartan silenciosamente registros
erróneos, ni se sustituyen por objetos o enemigos de respaldo.

- **Enemigos vacíos:** no se inicia la aventura y se vuelve al menú inicial.
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
El nuevo último campo de `DefinicionEnemigo` es `int experiencia`: se carga desde
los datos y se transmite al enemigo. El constructor anterior sigue disponible
con 5 EXP por compatibilidad; para nuevos orígenes debe usarse el campo explícito.
Los enemigos iniciales tienen 5–10 EXP, pero otros datos pueden proporcionar
recompensas mayores. El repositorio no calcula subidas de nivel ni entrega puntos:
la aventura concede la EXP solo ante victoria y Jugador gestiona su progresión.

## Límites actuales

- Se pueden añadir enemigos con las estadísticas existentes y armas con nuevas
  bonificaciones sin modificar combate, inventario o tienda. Esto no garantiza que
  cualquier combinación esté equilibrada; un combate sin daño puede no terminar.
- Solo hay dos tipos de poción, una definición por tipo en el jugador. Se pueden
  proporcionar nombres y cantidades de curación distintos, pero un tercer tipo o
  efecto requeriría otra fase. No se amplía el enum ni el inventario de consumibles.
- Las definiciones de armas solo producen ranura `ARMA`, sin habilidades. Otras
  ranuras, habilidades desde datos o bonificaciones de vida requieren nuevas reglas.
- Las recompensas siguen usando los dos IDs obligatorios; no hay botín de armas
  genérico ni múltiples tiendas.
- Las armas de partida no guardan su ID de catálogo: para vender se exige una
  correspondencia única por nombre, ranura, bonificaciones y ausencia de habilidades
  adicionales. Dos definiciones de armas indistinguibles hacen ambigua la venta y
  esta se rechaza. Usar definiciones distinguibles si se desea venderlas.
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
