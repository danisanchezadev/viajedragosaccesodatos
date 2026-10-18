package org.viajedragos;

import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Equipo;
import org.viajedragos.catalogo.RepositorioObjetos;
import org.viajedragos.catalogo.RepositorioObjetosMemoria;
import org.viajedragos.catalogo.RepositorioTienda;
import org.viajedragos.catalogo.RepositorioTiendaMemoria;
import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.Dado;
import org.viajedragos.combate.ResultadoCombate;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;

import java.io.PrintStream;
import org.viajedragos.consola.PanelConsola;
import org.viajedragos.consola.RitmoConsola;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.List;
import java.util.HashSet;

public class app {
    public static void main(String[] args) {
        // También en Windows: los bordes deben emitirse como UTF-8, no con la página de códigos heredada.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        ejecutar(new Scanner(System.in, StandardCharsets.UTF_8), System.out, new Dado(),
                new RitmoConsola(RitmoConsola.Velocidad.NORMAL));
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados) {
        // Un generador de botín por sesión, distinto del generador interno de Dado.
        ejecutar(entrada, salida, dados, new Random());
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados, RitmoConsola ritmo) {
        ejecutar(entrada, salida, dados, new Random(), new RepositorioEnemigosMemoria(), new Random(),
                new RepositorioObjetosMemoria(), null, ritmo);
    }

    static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados, Random azarBotin) {
        ejecutar(entrada, salida, dados, azarBotin, new RepositorioEnemigosMemoria(), new Random());
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados, Random azarBotin,
                                RepositorioEnemigos repositorio, Random azarEncuentros) {
        ejecutar(entrada, salida, dados, azarBotin, repositorio, azarEncuentros,
                new RepositorioObjetosMemoria());
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados, Random azarBotin,
                                RepositorioEnemigos repositorio, Random azarEncuentros,
                                RepositorioObjetos objetos) {
        ejecutar(entrada, salida, dados, azarBotin, repositorio, azarEncuentros, objetos, null);
    }


    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados, Random azarBotin,
                                RepositorioEnemigos repositorio, Random azarEncuentros,
                                RepositorioObjetos objetos, RepositorioTienda tienda) {
        ejecutar(entrada, salida, dados, azarBotin, repositorio, azarEncuentros, objetos, tienda,
                new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA));
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados, Random azarBotin,
                                RepositorioEnemigos repositorio, Random azarEncuentros,
                                RepositorioObjetos objetos, RepositorioTienda tienda, RitmoConsola ritmo) {
        java.util.Objects.requireNonNull(ritmo);

        while (true) {
            PanelConsola panel = new PanelConsola(salida, "VIAJE A DRAGOS");
            panel.linea("Explora, combate y mejora tu equipo.");
            panel.linea("La iniciativa decide quién actúa primero.");
            panel.linea("Velocidad: " + ritmo.getVelocidad().getNombre());
            panel.separador();
            panel.opcion(1, "Nueva aventura");
            panel.opcion(2, "Velocidad de narración");
            panel.opcion(0, "Salir del juego");
            panel.cerrar();
            salida.print("Elige una opción > ");
            salida.flush();
            if (!entrada.hasNextLine()) {
                salida.println("Entrada finalizada. Hasta pronto.");
                return;
            }

            String opcion = entrada.nextLine().trim();

            if (opcion.equals("0")) {
                salida.println("Hasta pronto.");
                return;
            }

            if (opcion.equals("2")) {
                if (!configurarVelocidad(entrada, salida, ritmo)) return;
                continue;
            }

            if (!opcion.equals("1")) {
                salida.println("Entrada inválida. Escribe 1, 2 o 0.");
                continue;
            }

            // Un jugador por aventura; continuar nunca reconstruye este personaje.
            Jugador jugador = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 48, 48);

            if (!jugarAventura(jugador, entrada, salida, dados, azarBotin,
                    repositorio, azarEncuentros, objetos, tienda, ritmo)) {
                return;
            }
        }
    }

    static boolean configurarVelocidad(Scanner entrada, PrintStream salida, RitmoConsola ritmo) {
        PanelConsola panel = new PanelConsola(salida, "VELOCIDAD DE NARRACIÓN");
        panel.linea("Actual: " + ritmo.getVelocidad().getNombre());
        RitmoConsola.Velocidad[] velocidades = RitmoConsola.Velocidad.values();
        for (int i = 0; i < velocidades.length; i++) {
            panel.opcion(i + 1, velocidades[i].getNombre() + " (" + velocidades[i].getMilisegundos() + " ms)");
        }
        panel.opcion(0, "Volver sin cambiar");
        panel.cerrar();
        int seleccion = leerIndiceEquipo(entrada, salida, velocidades.length);
        if (seleccion == -1) return false;
        if (seleccion != 0) {
            ritmo.setVelocidad(velocidades[seleccion - 1]);
            salida.println("Velocidad de narración: " + ritmo.getVelocidad().getNombre() + ".");
        }
        return true;
    }


    // Devuelve true para volver al menú inicial y false para terminar la sesión.
    static boolean jugarAventura(Jugador jugador, Scanner entrada, PrintStream salida, TiradorDado dados) {
        return jugarAventura(jugador, entrada, salida, dados, new Random());
    }

    static boolean jugarAventura(Jugador jugador, Scanner entrada, PrintStream salida,
                                 TiradorDado dados, Random azarBotin) {
        return jugarAventura(jugador, entrada, salida, dados, azarBotin,
                new RepositorioEnemigosMemoria(), new Random());
    }

    static boolean jugarAventura(Jugador jugador, Scanner entrada, PrintStream salida,
                                 TiradorDado dados, Random azarBotin,
                                 RepositorioEnemigos repositorio, Random azarEncuentros) {
        return jugarAventura(jugador, entrada, salida, dados, azarBotin, repositorio, azarEncuentros,
                new RepositorioObjetosMemoria());
    }

    static boolean jugarAventura(Jugador jugador, Scanner entrada, PrintStream salida,
                                 TiradorDado dados, Random azarBotin,
                                 RepositorioEnemigos repositorio, Random azarEncuentros,
                                 RepositorioObjetos objetos) {
        return jugarAventura(jugador, entrada, salida, dados, azarBotin, repositorio, azarEncuentros, objetos, null);
    }

    // null selecciona el catálogo comercial en memoria a partir de los objetos de esta aventura.
    static boolean jugarAventura(Jugador jugador, Scanner entrada, PrintStream salida,
                                 TiradorDado dados, Random azarBotin,
                                 RepositorioEnemigos repositorio, Random azarEncuentros,
                                 RepositorioObjetos objetos, RepositorioTienda tienda) {
        return jugarAventura(jugador, entrada, salida, dados, azarBotin, repositorio, azarEncuentros, objetos, tienda,
                new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA));
    }

    static boolean jugarAventura(Jugador jugador, Scanner entrada, PrintStream salida,
                                 TiradorDado dados, Random azarBotin,
                                 RepositorioEnemigos repositorio, Random azarEncuentros,
                                 RepositorioObjetos objetos, RepositorioTienda tienda, RitmoConsola ritmo) {
        java.util.Objects.requireNonNull(ritmo);
        List<DefinicionEnemigo> catalogo;
        try {
            catalogo = List.copyOf(repositorio.obtenerTodos());
            if (catalogo.isEmpty()) {
                salida.println("No hay enemigos disponibles. No se inicia la aventura.");
                return true;
            }
            var identificadores = new HashSet<String>();
            for (DefinicionEnemigo definicion : catalogo) {
                if (!identificadores.add(definicion.id())) {
                    salida.println("El catálogo contiene identificadores de enemigo repetidos. No se inicia la aventura.");
                    return true;
                }
            }
        } catch (IllegalArgumentException | NullPointerException datosInvalidos) {
            salida.println("El catálogo de enemigos no es válido. No se inicia la aventura.");
            return true;
        }
        DefinicionArma espada = null;
        DefinicionArma espadaHierro = null;
        try {
            List<DefinicionArma> armas = List.copyOf(objetos.obtenerArmas());
            List<DefinicionPocion> pociones = List.copyOf(objetos.obtenerPociones());
            var identificadores = new HashSet<String>();
            for (DefinicionArma arma : armas) {
                if (!identificadores.add(arma.id())) {
                    throw new IllegalArgumentException("Identificadores repetidos");
                }
                if (arma.id().equals(DefinicionArma.ID_ESPADA_DESGASTADA)) espada = arma;
                if (arma.id().equals(DefinicionArma.ID_ESPADA_HIERRO)) espadaHierro = arma;
            }
            for (DefinicionPocion pocion : pociones) {
                if (!identificadores.add(pocion.id())) {
                    throw new IllegalArgumentException("Identificadores repetidos entre objetos");
                }
            }
            if (espada == null) throw new IllegalArgumentException("Falta la Espada desgastada");
            if (espadaHierro == null) throw new IllegalArgumentException("Falta la Espada de hierro");
            jugador.configurarPociones(pociones);
            if (tienda == null) tienda = new RepositorioTiendaMemoria(armas, pociones);
        } catch (IllegalArgumentException | NullPointerException datosInvalidos) {
            salida.println("El catálogo de objetos no es válido o está incompleto. No se inicia la aventura.");
            return true;
        }
        int encuentro = 1;
        boolean espadaOfrecida = false;
        int victorias = 0;
        while (true) {
            Enemigo enemigo = seleccionarEnemigo(catalogo, azarEncuentros);
            PanelConsola panelEncuentro = new PanelConsola(salida, "ENCUENTRO " + encuentro);
            panelEncuentro.linea("Comienza el combate: " + jugador.getNombre() + " contra " + enemigo.getNombre() + ".");
            panelEncuentro.columnas(jugador.getNombre(), enemigo.getNombre());
            panelEncuentro.columnas("PS " + jugador.getVida() + "/" + jugador.getVidaMaxima(),
                    "PS " + enemigo.getVida() + "/" + enemigo.getVidaMaxima());
            panelEncuentro.cerrar();
            ritmo.pausar(salida);
            ResultadoCombate resultado;
            try {
                resultado = new Combate(jugador, enemigo, dados, ritmo).combatir(entrada, salida);
            } catch (NoSuchElementException finEntrada) {
                salida.println("Entrada finalizada. Sesión interrumpida; el combate queda sin terminar.");
                return false;
            }
            PanelConsola panelFinal = new PanelConsola(salida, "FIN DEL ENCUENTRO");
            panelFinal.linea(String.format("Vida final: %s %d/%d | %s %d/%d",
                    jugador.getNombre(), jugador.getVida(), jugador.getVidaMaxima(),
                    enemigo.getNombre(), enemigo.getVida(), enemigo.getVidaMaxima()));
            panelFinal.cerrar();
            mostrarResultado(resultado, salida);
            if (resultado == ResultadoCombate.ENEMIGO_MUERE || resultado == ResultadoCombate.JUGADOR_MUERE
                    || resultado == ResultadoCombate.AMBOS_MUEREN) ritmo.pausar(salida);
            if (resultado == ResultadoCombate.JUGADOR_MUERE || resultado == ResultadoCombate.AMBOS_MUEREN) {
                salida.println("La aventura ha terminado.");
                return true;
            }

            if (resultado == ResultadoCombate.ENEMIGO_MUERE) {
                victorias++;
                jugador.agregarOro(5);
                PanelConsola panelRecompensa = new PanelConsola(salida, "RECOMPENSAS");
                panelRecompensa.linea("Oro obtenido: +5 monedas");
                panelRecompensa.linea("Oro total: " + jugador.getOro() + " monedas");
                panelRecompensa.cerrar();
                ritmo.pausar(salida);
                // Solo una vez tras la resolución definitiva, fuera de los menús.
                obtenerBotin(jugador, azarBotin, salida, ritmo);
                if (!espadaOfrecida) {
                    espadaOfrecida = true;
                    if (!ofrecerEspada(jugador, entrada, salida, espada, ritmo)) return false;
                }
                if (victorias == 2 && !ofrecerArmaGuardada(jugador, entrada, salida, espadaHierro, ritmo)) return false;
            } else if (resultado == ResultadoCombate.JUGADOR_HUYE) {
                salida.println("Regresas a exploración sin recompensas. Conservas tu personaje y sus pertenencias.");
            }

            while (true) {
                mostrarExploracion(jugador, salida);
                String opcion = leerOpcion(entrada, salida);
                if (opcion == null) return false;
                if (opcion.equals("0")) {
                    salida.println("Hasta pronto.");
                    return false;
                }
                if (opcion.equals("4")) {
                    salida.println("La aventura ha terminado.");
                    return true;
                }
                if (opcion.equals("3")) break;
                if (opcion.equals("1")) {
                    if (!consultarInventario(jugador, entrada, salida,
                            salida == System.out && Boolean.getBoolean("viajedragos.ansi"))) return false;
                } else if (opcion.equals("2")) {
                    if (!usarObjeto(jugador, entrada, salida)) return false;
                } else if (opcion.equals("5")) {
                    if (!consultarTienda(jugador, entrada, salida, tienda)) return false;
                } else {
                    salida.println("Entrada inválida. Escribe 1, 2, 3, 4, 5 o 0.");
                }
            }
            encuentro++;
        }
    }

    private static Enemigo seleccionarEnemigo(List<DefinicionEnemigo> catalogo, Random azarEncuentros) {
        return catalogo.get(azarEncuentros.nextInt(catalogo.size())).crearEnemigo();
    }

    private static void mostrarExploracion(Jugador jugador, PrintStream salida) {
        PanelConsola panel = new PanelConsola(salida, "VIAJE A DRAGOS - EXPLORACIÓN");
        panel.linea(jugador.getNombre());
        panel.linea("Vida: " + jugador.getVida() + "/" + jugador.getVidaMaxima() + " PS");
        panel.linea(PanelConsola.barraVida(jugador.getVida(), jugador.getVidaMaxima()));
        panel.linea("Oro: " + jugador.getOro() + " monedas");
        panel.separador();
        panel.opcion(1, "Revisar inventario");
        panel.opcion(2, "Usar objeto");
        panel.opcion(3, "Continuar al siguiente encuentro");
        panel.opcion(4, "Terminar aventura y volver al menú inicial");
        panel.opcion(5, "Tienda");
        panel.opcion(0, "Salir del juego");
        panel.cerrar();
    }
    static boolean consultarTienda(Jugador jugador, Scanner entrada, PrintStream salida, RepositorioTienda tienda) {
        List<org.viajedragos.modelo.ProductoTienda> productos;
        try {
            productos = List.copyOf(tienda.obtenerTodos());
            var ids = new HashSet<String>();
            for (var producto : productos) {
                if (!ids.add(producto.id())) throw new IllegalArgumentException("Producto repetido");
            }
        } catch (RuntimeException catalogoInvalido) {
            salida.println("El catálogo de la tienda no es válido. No se ha realizado ninguna compra.");
            return true;
        }
        while (true) {
            PanelConsola panel = new PanelConsola(salida, "TIENDA");
            panel.linea("Oro disponible: " + jugador.getOro() + " monedas");
            if (productos.isEmpty()) panel.linea("No hay productos disponibles.");
            panel.opcion(1, "Comprar objetos");
            panel.opcion(2, "Vender objetos");
            panel.opcion(0, "Volver a exploración");
            panel.cerrar();
            int opcion = leerIndiceEquipo(entrada, salida, 2);
            if (opcion == -1) return false;
            if (opcion == 0) return true;
            if (opcion == 1) {
                if (!comprarObjetos(jugador, entrada, salida, tienda, productos)) return false;
            } else if (!venderObjetos(jugador, entrada, salida, tienda)) return false;
        }
    }

    private static boolean comprarObjetos(Jugador jugador, Scanner entrada, PrintStream salida,
            RepositorioTienda tienda, List<org.viajedragos.modelo.ProductoTienda> productos) {
        while (true) {
            PanelConsola panel = new PanelConsola(salida, "COMPRAR OBJETOS");
            panel.linea("Oro disponible: " + jugador.getOro() + " monedas");
            if (productos.isEmpty()) panel.linea("No hay productos disponibles.");
            for (int i = 0; i < productos.size(); i++) {
                var producto = productos.get(i);
                panel.opcion(i + 1, producto.nombre() + " - " + producto.precio() + " monedas");
            }
            panel.opcion(0, "Volver a la tienda");
            panel.cerrar();
            int indice = leerIndiceEquipo(entrada, salida, productos.size());
            if (indice == -1) return false;
            if (indice == 0) return true;
            var producto = productos.get(indice - 1);
            switch (jugador.comprar(producto.id(), tienda)) {
                case COMPRADA -> salida.printf("Has comprado %s por %d monedas. Se guarda en tu inventario.%n",
                        producto.nombre(), producto.precio());
                case SALDO_INSUFICIENTE -> salida.println("No tienes suficiente oro para comprar ese producto.");
                case PRODUCTO_INEXISTENTE -> salida.println("Ese producto ya no está disponible.");
                case PRODUCTO_INVALIDO -> salida.println("El producto o su definición no son válidos. No se ha realizado la compra.");
                case INCORPORACION_FALLIDA -> salida.println("No se ha podido incorporar el objeto. Conservas tu oro y tu inventario.");
            }
        }
    }

    private record Venta(org.viajedragos.modelo.ProductoTienda producto, Equipo equipo,
                         boolean equipado, int cantidad) { }

    private static boolean venderObjetos(Jugador jugador, Scanner entrada, PrintStream salida,
            RepositorioTienda tienda) {
        while (true) {
            var ventas = new java.util.ArrayList<Venta>();
            for (TipoPocion tipo : TipoPocion.values()) {
                int cantidad = jugador.getCantidadPociones(tipo);
                if (cantidad > 0) jugador.productoVenta(tipo, tienda)
                        .ifPresent(p -> ventas.add(new Venta(p, null, false, cantidad)));
            }
            for (Equipo equipo : jugador.getInventarioEquipo()) {
                if (jugador.getInventarioEquipo().stream().filter(p -> p == equipo).count() != 1
                        || jugador.getEquipoEquipado().containsValue(equipo)) continue;
                jugador.productoVenta(equipo, tienda)
                        .ifPresent(p -> ventas.add(new Venta(p, equipo, false, 1)));
            }
            for (Equipo equipo : jugador.getEquipoEquipado().values()) {
                if (jugador.getInventarioEquipo().contains(equipo)) continue;
                jugador.productoVenta(equipo, tienda)
                        .ifPresent(p -> ventas.add(new Venta(p, equipo, true, 1)));
            }
            PanelConsola panel = new PanelConsola(salida, "VENDER OBJETOS");
            panel.linea("Oro disponible: " + jugador.getOro() + " monedas");
            if (ventas.isEmpty()) panel.linea("No tienes objetos vendibles.");
            for (int i = 0; i < ventas.size(); i++) {
                Venta venta = ventas.get(i);
                panel.opcion(i + 1, venta.producto().nombre() + (venta.equipado() ? " (equipada)" : "")
                        + " - Cantidad: " + venta.cantidad() + " - Venta: "
                        + venta.producto().precioVenta() + " monedas");
            }
            panel.opcion(0, "Volver a la tienda");
            panel.cerrar();
            int indice = leerIndiceEquipo(entrada, salida, ventas.size());
            if (indice == -1) return false;
            if (indice == 0) return true;
            Venta venta = ventas.get(indice - 1);
            PanelConsola confirmar = new PanelConsola(salida, "CONFIRMAR VENTA");
            confirmar.linea("Oro disponible: " + jugador.getOro() + " monedas");
            confirmar.linea("Vender una unidad de " + venta.producto().nombre()
                    + " por " + venta.producto().precioVenta() + " monedas.");
            if (venta.equipado()) confirmar.linea("Está equipada: se desequipará y perderás sus efectos.");
            confirmar.opcion(1, venta.equipado() ? "Desequipar y vender" : "Confirmar venta");
            confirmar.opcion(0, "Cancelar y volver a la tienda");
            confirmar.cerrar();
            int opcion = leerIndiceEquipo(entrada, salida, 1);
            if (opcion == -1) return false;
            if (opcion == 0) return true;
            boolean vendida = venta.equipo() == null
                    ? jugador.venderPocion(venta.producto().pocion().tipo(), tienda)
                    : jugador.venderEquipo(venta.equipo(), venta.equipado(), tienda);
            if (vendida) salida.printf("Has vendido %s por %d monedas. Oro disponible: %d monedas.%n",
                    venta.producto().nombre(), venta.producto().precioVenta(), jugador.getOro());
            else salida.println("No se ha podido vender el objeto. Conservas tus objetos y tu oro.");
        }
    }

    // null representa fin de entrada; los flujos pertenecen al llamador.
    private static String leerOpcion(Scanner entrada, PrintStream salida) {
        salida.print("Elige una opción > ");
        salida.flush();
        if (!entrada.hasNextLine()) {
            salida.println("Entrada finalizada. Hasta pronto.");
            return null;
        }
        return entrada.nextLine().trim();
    }

    static boolean consultarInventario(Jugador jugador, Scanner entrada, PrintStream salida) {
        return consultarInventario(jugador, entrada, salida, false);
    }

    static boolean consultarInventario(Jugador jugador, Scanner entrada, PrintStream salida, boolean coloresAnsi) {
        PanelConsola panel = new PanelConsola(salida, "INVENTARIO");
        panel.linea("Oro: " + jugador.getOro() + " monedas");
        panel.columnas("Fuerza: " + jugador.getFuerza(), "Resistencia: " + jugador.getResistencia());
        panel.columnas("Iniciativa: " + jugador.getIniciativa(), "Agilidad: " + jugador.getAgilidad());
        panel.linea("Velocidad: " + jugador.getVelocidad());
        panel.separador();
        panel.titulo("CONSUMIBLES");
        boolean tienePociones = false;
        for (TipoPocion tipo : TipoPocion.values()) {
            int cantidad = jugador.getCantidadPociones(tipo);
            if (cantidad > 0) {
                tienePociones = true;
                panel.linea(jugador.getDefinicionPocion(tipo).nombre() + " x" + cantidad);
                panel.linea("Recupera " + jugador.getDefinicionPocion(tipo).recuperacionPS() + " PS");
            }
        }
        if (!tienePociones) panel.linea("No tienes consumibles");

        panel.separador();
        panel.titulo("EQUIPO");
        if (jugador.getEquipoEquipado().isEmpty()) panel.linea("No tienes equipo equipado");
        for (TipoEquipo ranura : TipoEquipo.values()) {
            var equipo = jugador.getEquipoEquipado().get(ranura);
            if (equipo == null) continue;
            mostrarEquipo(panel, equipo);
        }
        panel.separador();
        panel.titulo("EQUIPAMIENTO GUARDADO");
        if (jugador.getInventarioEquipo().isEmpty()) panel.linea("No tienes equipamiento guardado");
        for (Equipo equipo : jugador.getInventarioEquipo()) {
            mostrarEquipo(panel, equipo);
        }
        panel.cerrar();
        while (true) {
            PanelConsola acciones = new PanelConsola(salida, "ACCIONES DE INVENTARIO");
            acciones.opcion(1, "Comparar equipamiento");
            acciones.opcion(2, "Cambiar equipamiento");
            acciones.opcion(0, "Volver a exploración");
            acciones.cerrar();
            String opcion = leerOpcion(entrada, salida);
            if (opcion == null) return false;
            if (opcion.equals("0")) return true;
            if (opcion.equals("1")) {
                if (!compararEquipamiento(jugador, entrada, salida, coloresAnsi)) return false;
            } else if (opcion.equals("2")) {
                if (!cambiarEquipamiento(jugador, entrada, salida, coloresAnsi)) return false;
            } else {
                salida.println("Entrada inválida. Escribe 1, 2 o 0 para volver.");
            }
        }
    }

    private static void mostrarEquipo(PanelConsola panel, Equipo equipo) {
        panel.linea(equipo.getTipoEquipo().getNombre() + ": " + equipo.getNombre());
        equipo.getBonificaciones().forEach((atributo, cantidad) ->
                panel.linea(String.format("%s %+d", atributo, cantidad)));
        equipo.getHabilidadesOtorgadas().forEach((habilidad, nivel) ->
                panel.linea(habilidad.getNombre() + ": nivel otorgado " + nivel));
    }

    private static boolean compararEquipamiento(Jugador jugador, Scanner entrada, PrintStream salida,
                                                boolean coloresAnsi) {
        if (jugador.getEquipoEquipado().isEmpty()) {
            salida.println("No tienes equipo equipado para comparar.");
            return true;
        }
        if (jugador.getInventarioEquipo().isEmpty()) {
            salida.println("No tienes equipamiento guardado para comparar.");
            return true;
        }
        List<Equipo> equipados = java.util.Arrays.stream(TipoEquipo.values())
                .map(tipo -> jugador.getEquipoEquipado().get(tipo)).filter(java.util.Objects::nonNull).toList();
        while (true) {
            PanelConsola panel = new PanelConsola(salida, "COMPARAR - PIEZA EQUIPADA");
            for (int i = 0; i < equipados.size(); i++) {
                Equipo equipo = equipados.get(i);
                panel.opcion(i + 1, equipo.getTipoEquipo().getNombre() + ": " + equipo.getNombre());
            }
            panel.opcion(0, "Volver al inventario");
            panel.cerrar();
            int indice = leerIndiceEquipo(entrada, salida, equipados.size());
            if (indice == -1) return false;
            if (indice == 0) return true;
            Equipo actual = equipados.get(indice - 1);
            List<Equipo> compatibles = jugador.getInventarioEquipo().stream()
                    .filter(equipo -> equipo.getTipoEquipo() == actual.getTipoEquipo()).toList();
            if (compatibles.isEmpty()) {
                salida.println("No tienes piezas guardadas compatibles con la ranura "
                        + actual.getTipoEquipo().getNombre() + ".");
                continue;
            }
            while (true) {
                PanelConsola guardadas = new PanelConsola(salida, "COMPARAR - PIEZA GUARDADA");
                guardadas.linea("Pieza actual: " + actual.getNombre() + " (" + actual.getTipoEquipo().getNombre() + ")");
                for (int i = 0; i < compatibles.size(); i++) {
                    guardadas.opcion(i + 1, compatibles.get(i).getNombre());
                }
                guardadas.opcion(0, "Volver a la selección de pieza equipada");
                guardadas.cerrar();
                int nueva = leerIndiceEquipo(entrada, salida, compatibles.size());
                if (nueva == -1) return false;
                if (nueva == 0) break;
                Equipo elegido = compatibles.get(nueva - 1);
                salida.printf("Actual: %s | Nuevo: %s%n", actual.getNombre(), elegido.getNombre());
                actual.compararCon(elegido, salida, coloresAnsi);
            }
        }
    }

    private static boolean cambiarEquipamiento(Jugador jugador, Scanner entrada, PrintStream salida,
                                               boolean coloresAnsi) {
        if (jugador.getInventarioEquipo().isEmpty()) {
            salida.println("No tienes equipamiento guardado para cambiar.");
            return true;
        }
        TipoEquipo[] ranuras = TipoEquipo.values();
        while (true) {
            PanelConsola panel = new PanelConsola(salida, "CAMBIAR - RANURA");
            for (int i = 0; i < ranuras.length; i++) {
                Equipo actual = jugador.getEquipoEquipado().get(ranuras[i]);
                panel.opcion(i + 1, ranuras[i].getNombre() + ": " + (actual == null ? "Sin equipo" : actual.getNombre()));
            }
            panel.opcion(0, "Volver al inventario");
            panel.cerrar();
            int ranura = leerIndiceEquipo(entrada, salida, ranuras.length);
            if (ranura == -1) return false;
            if (ranura == 0) return true;
            TipoEquipo tipo = ranuras[ranura - 1];
            List<Integer> compatibles = new java.util.ArrayList<>();
            for (int i = 0; i < jugador.getInventarioEquipo().size(); i++) {
                if (jugador.getInventarioEquipo().get(i).getTipoEquipo() == tipo) compatibles.add(i);
            }
            if (compatibles.isEmpty()) {
                salida.println("No tienes piezas guardadas compatibles con la ranura " + tipo.getNombre() + ".");
                continue;
            }
            PanelConsola guardadas = new PanelConsola(salida, "SELECCIONA UNA PIEZA GUARDADA");
            for (int i = 0; i < compatibles.size(); i++) {
                guardadas.opcion(i + 1, jugador.getInventarioEquipo().get(compatibles.get(i)).getNombre());
            }
            guardadas.opcion(0, "Volver a la selección de ranura");
            guardadas.cerrar();
            int pieza = leerIndiceEquipo(entrada, salida, compatibles.size());
            if (pieza == -1) return false;
            if (pieza == 0) continue;
            int indice = compatibles.get(pieza - 1);
            Equipo nueva = jugador.getInventarioEquipo().get(indice);
            Equipo actual = jugador.getEquipoEquipado().get(tipo);
            if (actual != null) {
                salida.printf("Actual: %s | Nuevo: %s%n", actual.getNombre(), nueva.getNombre());
                actual.compararCon(nueva, salida, coloresAnsi);
            }
            PanelConsola confirmacion = new PanelConsola(salida, "CONFIRMAR EQUIPAMIENTO");
            confirmacion.linea("¿Equipar " + nueva.getNombre() + "?");
            confirmacion.opcion(1, "Confirmar");
            confirmacion.opcion(0, "Cancelar");
            confirmacion.cerrar();
            int confirmar = leerIndiceEquipo(entrada, salida, 1);
            if (confirmar == -1) return false;
            if (confirmar == 0) continue;
            if (jugador.equiparDesdeInventario(tipo, indice)) {
                salida.printf("%s equipada. %s%n", nueva.getNombre(), actual == null
                        ? "La pieza deja el inventario." : "La pieza anterior pasa al inventario.");
                return true;
            }
            salida.println("No se ha podido cambiar el equipo. Se conserva el estado anterior.");
        }
    }

    // -1 indica fin de entrada; 0 vuelve al menú anterior.
    private static int leerIndiceEquipo(Scanner entrada, PrintStream salida, int cantidad) {
        while (true) {
            String opcion = leerOpcion(entrada, salida);
            if (opcion == null) return -1;
            try {
                int indice = Integer.parseInt(opcion);
                if (indice >= 0 && indice <= cantidad) return indice;
            } catch (NumberFormatException invalida) {
                // Un texto no numérico o fuera del rango de int no termina la navegación.
            }
            salida.printf("Entrada inválida. Escribe un número entre 0 y %d.%n", cantidad);
        }
    }

    static boolean usarObjeto(Jugador jugador, Scanner entrada, PrintStream salida) {
        PanelConsola panel = new PanelConsola(salida, "USAR OBJETO");
        panel.linea("Vida actual: " + jugador.getVida() + "/" + jugador.getVidaMaxima() + " PS");
        int numero = 1;
        for (TipoPocion tipo : TipoPocion.values()) {
            panel.opcion(numero++, jugador.getDefinicionPocion(tipo).nombre() + " (x"
                    + jugador.getCantidadPociones(tipo) + ") +" + jugador.getDefinicionPocion(tipo).recuperacionPS() + " PS");
        }
        panel.opcion(0, "Cancelar");
        panel.cerrar();
        while (true) {
            String opcion = leerOpcion(entrada, salida);
            if (opcion == null) return false;
            if (opcion.equals("0")) return true;
            TipoPocion tipo;
            if (opcion.equals("1")) tipo = TipoPocion.PEQUENA;
            else if (opcion.equals("2")) tipo = TipoPocion.GRANDE;
            else {
                salida.println("Entrada inválida. Escribe 1, 2 o 0.");
                continue;
            }
            if (jugador.getCantidadPociones(tipo) == 0) {
                salida.println("No tienes esa poción.");
                continue;
            }
            if (jugador.getVida() >= jugador.getVidaMaxima()) {
                salida.println("La vida está completa. No se ha consumido la poción.");
                continue;
            }
            int recuperacion = jugador.usarPocion(tipo);
            if (recuperacion > 0) {
                salida.printf("Has utilizado una %s.%nVida recuperada: +%d PS%nVida actual: %d/%d PS%n",
                        jugador.getDefinicionPocion(tipo).nombre(), recuperacion, jugador.getVida(), jugador.getVidaMaxima());
                salida.printf("Unidades restantes de %s: %d%n", jugador.getDefinicionPocion(tipo).nombre(), jugador.getCantidadPociones(tipo));
                return true;
            }
            salida.println("No se ha recuperado vida. No se ha consumido la poción.");
        }
    }

    static void obtenerBotin(Jugador jugador, Random azarBotin, PrintStream salida) {
        obtenerBotin(jugador, azarBotin, salida, new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA));
    }

    static void obtenerBotin(Jugador jugador, Random azarBotin, PrintStream salida, RitmoConsola ritmo) {
        if (azarBotin.nextInt(100) < 50) {
            jugador.agregarPocion(TipoPocion.PEQUENA);
            salida.printf("Botín: %s (+1).%n", jugador.getDefinicionPocion(TipoPocion.PEQUENA).nombre());
            ritmo.pausar(salida);
        }
        if (azarBotin.nextInt(100) < 20) {
            jugador.agregarPocion(TipoPocion.GRANDE);
            salida.printf("Botín: %s (+1).%n", jugador.getDefinicionPocion(TipoPocion.GRANDE).nombre());
            ritmo.pausar(salida);
        }
    }

    // La pieza se crea una sola vez por oferta. false indica cierre de entrada.
    static boolean ofrecerEspada(Jugador jugador, Scanner entrada, PrintStream salida) {
        return ofrecerEspada(jugador, entrada, salida,
                new RepositorioObjetosMemoria().obtenerArmas().getFirst());
    }

    private static boolean ofrecerArmaGuardada(Jugador jugador, Scanner entrada, PrintStream salida,
                                              DefinicionArma definicion, RitmoConsola ritmo) {
        Arma arma = definicion.crearArma();
        PanelConsola panel = new PanelConsola(salida, "BOTÍN - ARMA ENCONTRADA");
        panel.linea("Has encontrado una " + arma.getNombre() + ".");
        mostrarEquipo(panel, arma);
        panel.separador();
        panel.opcion(1, "Guardar en el inventario");
        panel.opcion(2, "Dejarla atrás");
        panel.cerrar();
        ritmo.pausar(salida);
        while (true) {
            String opcion = leerOpcion(entrada, salida);
            if (opcion == null) return false;
            if (opcion.equals("1")) {
                jugador.agregarEquipo(arma);
                salida.printf("%s guardada en el inventario.%n", arma.getNombre());
                return true;
            }
            if (opcion.equals("2")) {
                salida.printf("Dejas atrás la %s.%n", arma.getNombre());
                return true;
            }
            salida.println("Entrada inválida. Escribe 1 o 2.");
        }
    }


    static boolean ofrecerEspada(Jugador jugador, Scanner entrada,
                                 PrintStream salida, DefinicionArma definicion) {
        return ofrecerEspada(jugador, entrada, salida, definicion,
                new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA));
    }

    static boolean ofrecerEspada(Jugador jugador, Scanner entrada,
                                 PrintStream salida, DefinicionArma definicion, RitmoConsola ritmo) {

        Arma espada = definicion.crearArma();

        PanelConsola panel = new PanelConsola(salida, "BOTÍN - ARMA ENCONTRADA");
        panel.linea("Has encontrado una " + espada.getNombre() + ".");
        mostrarEquipo(panel, espada);
        panel.separador();
        panel.opcion(1, "Equipar " + espada.getNombre());
        panel.opcion(2, "Rechazar " + espada.getNombre());
        panel.cerrar();
        ritmo.pausar(salida);
        while (true) {
            salida.print("Elige una opción > ");
            salida.flush();

            if (!entrada.hasNextLine()) {
                salida.println("Entrada finalizada. Hasta pronto.");
                return false;
            }

            String opcion = entrada.nextLine().trim();

            if (opcion.equals("1")) {
                if (jugador.equipar(espada)) {
                    salida.printf("%s equipada. Fuerza actual: %d.%n",
                            espada.getNombre(), jugador.getFuerza());
                } else {
                    salida.println("No se puede equipar: la ranura de arma está ocupada.");
                }
                return true;
            }

            if (opcion.equals("2")) {
                salida.printf("Has rechazado la %s. La dejas atrás.%n",
                        espada.getNombre());
                return true;
            }

            salida.println("Entrada inválida. Escribe 1 o 2.");
        }
    }

    static void mostrarResultado(ResultadoCombate resultado, PrintStream salida) {
        salida.println(switch (resultado) {
            case ENEMIGO_MUERE -> "Victoria. " + resultado.getDescripcion();
            case JUGADOR_MUERE -> "Derrota. " + resultado.getDescripcion();
            case AMBOS_MUEREN -> "Ambos han muerto. " + resultado.getDescripcion();
            case JUGADOR_HUYE -> "Has escapado. " + resultado.getDescripcion();
            default -> resultado.getDescripcion();
        });
    }
}
