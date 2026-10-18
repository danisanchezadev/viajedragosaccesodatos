package org.viajedragos;

import org.viajedragos.arma.Arma;
import org.viajedragos.catalogo.RepositorioObjetos;
import org.viajedragos.catalogo.RepositorioObjetosMemoria;
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
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.List;
import java.util.HashSet;

public class app {
    public static void main(String[] args) {
        ejecutar(new Scanner(System.in), System.out, new Dado());
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados) {
        // Un generador de botín por sesión, distinto del generador interno de Dado.
        ejecutar(entrada, salida, dados, new Random());
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
        salida.println("=== Viaje a Dragos ===");
        salida.println("Un combate por turnos. La iniciativa decide quién actúa primero.");
        while (true) {
            salida.println("1. Nueva aventura");
            salida.println("0. Salir");
            salida.print("> ");
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
            if (!opcion.equals("1")) {
                salida.println("Entrada inválida. Escribe 1 o 0.");
                continue;
            }
            // Un jugador por aventura; continuar nunca reconstruye este personaje.
            Jugador jugador = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 48, 48);
            if (!jugarAventura(jugador, entrada, salida, dados, azarBotin, repositorio, azarEncuentros, objetos)) return;
        }
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
        try {
            List<DefinicionArma> armas = List.copyOf(objetos.obtenerArmas());
            List<DefinicionPocion> pociones = List.copyOf(objetos.obtenerPociones());
            var identificadores = new HashSet<String>();
            for (DefinicionArma arma : armas) {
                if (!identificadores.add(arma.id())) {
                    throw new IllegalArgumentException("Identificadores repetidos");
                }
                if (arma.id().equals(DefinicionArma.ID_ESPADA_DESGASTADA)) espada = arma;
            }
            for (DefinicionPocion pocion : pociones) {
                if (!identificadores.add(pocion.id())) {
                    throw new IllegalArgumentException("Identificadores repetidos entre objetos");
                }
            }
            if (espada == null) throw new IllegalArgumentException("Falta la Espada desgastada");
            jugador.configurarPociones(pociones);
        } catch (IllegalArgumentException | NullPointerException datosInvalidos) {
            salida.println("El catálogo de objetos no es válido o está incompleto. No se inicia la aventura.");
            return true;
        }
        int encuentro = 1;
        boolean espadaOfrecida = false;
        while (true) {
            Enemigo enemigo = seleccionarEnemigo(catalogo, azarEncuentros);
            salida.printf("%nEncuentro %d.%n", encuentro);
            salida.printf("Comienza el combate: %s contra %s.%n", jugador.getNombre(), enemigo.getNombre());
            ResultadoCombate resultado;
            try {
                resultado = new Combate(jugador, enemigo, dados).combatir(entrada, salida);
            } catch (NoSuchElementException finEntrada) {
                salida.println("Entrada finalizada. Sesión interrumpida; el combate queda sin terminar.");
                return false;
            }
            salida.printf("Vida final: %s %d/%d | %s %d/%d%n",
                    jugador.getNombre(), jugador.getVida(), jugador.getVidaMaxima(),
                    enemigo.getNombre(), enemigo.getVida(), enemigo.getVidaMaxima());
            mostrarResultado(resultado, salida);
            if (resultado == ResultadoCombate.JUGADOR_MUERE || resultado == ResultadoCombate.AMBOS_MUEREN) {
                salida.println("La aventura ha terminado.");
                return true;
            }

            if (resultado == ResultadoCombate.ENEMIGO_MUERE) {
                // Solo una vez tras la resolución definitiva, fuera de los menús.
                obtenerBotin(jugador, azarBotin, salida);
                if (!espadaOfrecida) {
                    espadaOfrecida = true;
                    if (!ofrecerEspada(jugador, entrada, salida, espada)) return false;
                }
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
                    if (!consultarInventario(jugador, entrada, salida)) return false;
                } else if (opcion.equals("2")) {
                    if (!usarObjeto(jugador, entrada, salida)) return false;
                } else {
                    salida.println("Entrada inválida. Escribe 1, 2, 3, 4 o 0.");
                }
            }
            encuentro++;
        }
    }

    private static Enemigo seleccionarEnemigo(List<DefinicionEnemigo> catalogo, Random azarEncuentros) {
        return catalogo.get(azarEncuentros.nextInt(catalogo.size())).crearEnemigo();
    }

    private static void cabecera(String titulo, PrintStream salida) {
        salida.printf("%n----------------------------------------%n");
        salida.println("  " + titulo);
        salida.println("----------------------------------------");
    }

    private static void mostrarExploracion(Jugador jugador, PrintStream salida) {
        cabecera("VIAJE A DRAGOS - EXPLORACIÓN", salida);
        salida.printf("  %s%n  Vida: %d/%d PS%n%n",
                jugador.getNombre(), jugador.getVida(), jugador.getVidaMaxima());
        salida.println("1. Revisar inventario");
        salida.println("2. Usar objeto");
        salida.println("3. Continuar al siguiente encuentro");
        salida.println("4. Terminar aventura y volver al menú inicial");
        salida.println("0. Salir del juego");
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
        cabecera("INVENTARIO", salida);
        salida.println("CONSUMIBLES");
        boolean tienePociones = false;
        for (TipoPocion tipo : TipoPocion.values()) {
            int cantidad = jugador.getCantidadPociones(tipo);
            if (cantidad > 0) {
                tienePociones = true;
                salida.printf("  %-24s x%d%n    Recupera %d PS%n",
                        jugador.getDefinicionPocion(tipo).nombre(), cantidad, jugador.getDefinicionPocion(tipo).recuperacionPS());
            }
        }
        if (!tienePociones) salida.println("  No tienes consumibles");

        salida.printf("%nEQUIPO%n");
        if (jugador.getEquipoEquipado().isEmpty()) salida.println("  No tienes equipo equipado");
        for (TipoEquipo ranura : TipoEquipo.values()) {
            var equipo = jugador.getEquipoEquipado().get(ranura);
            if (equipo == null) continue;
            salida.printf("  %s: %s%n", ranura.getNombre(), equipo.getNombre());
            equipo.getBonificaciones().forEach((atributo, cantidad) ->
                    salida.printf("    %s %+d%n", atributo, cantidad));
            equipo.getHabilidadesOtorgadas().forEach((habilidad, nivel) ->
                    salida.printf("    %s: nivel otorgado %d%n", habilidad.getNombre(), nivel));
        }
        salida.printf("%n0. Volver a exploración%n");
        while (true) {
            String opcion = leerOpcion(entrada, salida);
            if (opcion == null) return false;
            if (opcion.equals("0")) return true;
            salida.println("Entrada inválida. Escribe 0 para volver.");
        }
    }

    static boolean usarObjeto(Jugador jugador, Scanner entrada, PrintStream salida) {
        cabecera("USAR OBJETO", salida);
        salida.printf("  Vida actual: %d/%d PS%n%n", jugador.getVida(), jugador.getVidaMaxima());
        salida.printf("1. %s (x%d) +%d PS%n", jugador.getDefinicionPocion(TipoPocion.PEQUENA).nombre(),
                jugador.getCantidadPociones(TipoPocion.PEQUENA), jugador.getDefinicionPocion(TipoPocion.PEQUENA).recuperacionPS());
        salida.printf("2. %s (x%d) +%d PS%n", jugador.getDefinicionPocion(TipoPocion.GRANDE).nombre(),
                jugador.getCantidadPociones(TipoPocion.GRANDE), jugador.getDefinicionPocion(TipoPocion.GRANDE).recuperacionPS());
        salida.println("0. Cancelar");
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
        if (azarBotin.nextInt(100) < 50) {
            jugador.agregarPocion(TipoPocion.PEQUENA);
            salida.printf("Botín: %s (+1).%n", jugador.getDefinicionPocion(TipoPocion.PEQUENA).nombre());
        }
        if (azarBotin.nextInt(100) < 20) {
            jugador.agregarPocion(TipoPocion.GRANDE);
            salida.printf("Botín: %s (+1).%n", jugador.getDefinicionPocion(TipoPocion.GRANDE).nombre());
        }
    }

    // La pieza se crea una sola vez por oferta. false indica cierre de entrada.
    static boolean ofrecerEspada(Jugador jugador, Scanner entrada, PrintStream salida) {
        return ofrecerEspada(jugador, entrada, salida,
                new RepositorioObjetosMemoria().obtenerArmas().getFirst());
    }

    static boolean ofrecerEspada(Jugador jugador, Scanner entrada, PrintStream salida, DefinicionArma definicion) {
        Arma espada = definicion.crearArma();
        salida.printf("Has encontrado una %s.%n", espada.getNombre());
        espada.getBonificaciones().forEach((atributo, cantidad) -> {
            String nombre = atributo.name().toLowerCase(java.util.Locale.ROOT);
            salida.printf("%s %+d%n", Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1), cantidad);
        });
        while (true) {
            salida.printf("1. Equipar %s%n", espada.getNombre());
            salida.printf("2. Rechazar %s%n", espada.getNombre());
            salida.print("> ");
            salida.flush();
            if (!entrada.hasNextLine()) {
                salida.println("Entrada finalizada. Hasta pronto.");
                return false;
            }
            String opcion = entrada.nextLine().trim();
            if (opcion.equals("1")) {
                if (jugador.equipar(espada)) {
                    salida.printf("%s equipada. Fuerza actual: %d.%n", espada.getNombre(), jugador.getFuerza());
                } else {
                    salida.println("No se puede equipar: la ranura de arma está ocupada.");
                }
                return true;
            }
            if (opcion.equals("2")) {
                salida.printf("Has rechazado la %s. La dejas atrás.%n", espada.getNombre());
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
