package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BotinEquipamientoConsolaTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 100, 48, 48); }
    private Random ninguno() { return TextoConsola.sinBotinEquipamiento(); }
    private static class Tiradas extends Random {
        final int[] valores; int usadas; final List<Integer> limites = new ArrayList<>();
        Tiradas(int... valores) { super(0); this.valores = valores; }
        public int nextInt(int limite) {
            assertTrue(usadas < valores.length, "Tirada inesperada"); limites.add(limite);
            int valor = valores[usadas++]; assertTrue(valor >= 0 && valor < limite); return valor;
        }
        void agotadas() { assertEquals(valores.length, usadas); }
    }
    private RepositorioEnemigos enemigo() {
        return () -> List.of(new DefinicionEnemigo("externo", "Enemigo de prueba", "", 1, 0, 1, 0, 0, 1, 1));
    }
    private void resolverAlInicio(Jugador j) {
        j.aprenderHabilidad(new Habilidad("Resolución controlada de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) { o.setVida(0); }
        }, 1);
    }
    private boolean aventura(Jugador j, String entradas, RepositorioEnemigos enemigos, RepositorioObjetos objetos,
                             Random equipo, Random atributos, Random habilidades, Random pociones, int[] dados, int[] pausas) {
        var ritmo = new RitmoConsola(RitmoConsola.Velocidad.NORMAL, ms -> pausas[0]++);
        boolean resultado = app.jugarAventura(j, new Scanner(entradas), salida(), () -> { dados[0]++; return 4; }, pociones,
                enemigos, new Random(0), objetos, null, ritmo, atributos, habilidades, new TiempoAventura(() -> 0), equipo);
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8)); return resultado;
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    void equipoPergaminosYOroExpSonIndependientesSinTocarDadosNiAnadirPausas(int mascara) {
        boolean equipo = (mascara & 1) != 0, atributo = (mascara & 2) != 0,
                habilidad = (mascara & 4) != 0, maestria = (mascara & 8) != 0;
        Jugador j = jugador();
        var azarEquipo = equipo ? new Tiradas(19, 0, 0, 1) : new Tiradas(20);
        var azarAtributos = atributo ? new Tiradas(9, 0) : new Tiradas(10);
        var azarHabilidades = habilidad ? new Tiradas(4, 0, maestria ? 1 : 2) : new Tiradas(5, maestria ? 1 : 2);
        var azarPociones = new Tiradas(99, 99); int[] dados = {0}, pausas = {0};
        assertFalse(aventura(j, "3\n1\n2\n0\n", enemigo(), new RepositorioObjetosMemoria(),
                azarEquipo, azarAtributos, azarHabilidades, azarPociones, dados, pausas));
        azarEquipo.agotadas(); azarAtributos.agotadas(); azarHabilidades.agotadas(); azarPociones.agotadas();
        assertEquals(equipo ? List.of(100, 10, 7, 2) : List.of(100), azarEquipo.limites);
        assertEquals(6, dados[0]); assertEquals(6 + (atributo ? 1 : 0) + (habilidad ? 1 : 0) + (maestria ? 1 : 0), pausas[0]);
        assertEquals(equipo ? 1 : 0, j.getInventarioEquipo().size());
        assertEquals(atributo ? 1 : 0, j.getCantidadPergaminos(Atributo.FUERZA));
        assertEquals(habilidad ? 1 : 0, j.getCantidadPergaminosHabilidad(HabilidadDisponible.CAPARAZON));
        assertEquals(maestria ? 1 : 0, j.getCantidadPergaminosMaestria());
        assertEquals(20, j.getOro()); assertEquals(1, j.getExperiencia()); assertEquals(48, j.getVida());
        assertEquals(6, j.getFuerza()); assertTrue(j.getEquipoEquipado().isEmpty()); assertTrue(j.getHabilidades().isEmpty());
        assertEquals(equipo, texto().contains("BOTÍN DE EQUIPAMIENTO"));
        if (equipo) {
            assertTrue(texto().contains("Arma: Espada desgastada FUERZA +1 Mejoras aplicadas: 0 Mejoras disponibles: 1"));
            assertTrue(texto().indexOf("RECOMPENSAS") < texto().indexOf("BOTÍN DE EQUIPAMIENTO"));
        }
    }

    @Test void dosPiezasIgualesNoSeAgrupanYSeInspeccionanSinNuevasTiradas() {
        Jugador j = jugador(); resolverAlInicio(j);
        var azar = new Tiradas(0, 0, 0, 0, 0, 0, 0, 1); int[] dados = {0};
        assertFalse(aventura(j, "3\n2\n3\n2\n1\n2\n8\n1\n0\n0\n0\n0\n0\n", enemigo(),
                new RepositorioObjetosMemoria(), azar, ninguno(), ninguno(), ninguno(), dados, new int[]{0}));
        azar.agotadas(); assertEquals(0, dados[0]); assertEquals(2, j.getInventarioEquipo().size());
        Equipo a = j.getInventarioEquipo().get(0), b = j.getInventarioEquipo().get(1);
        assertNotSame(a, b); assertEquals(a.getNombre(), b.getNombre()); assertEquals(a.getIdDefinicion(), b.getIdDefinicion());
        assertEquals(0, a.getMejorasDisponibles()); assertEquals(1, b.getMejorasDisponibles());
        assertEquals(6, j.getFuerza()); assertTrue(j.getEquipoEquipado().isEmpty()); assertEquals(1, j.getHabilidades().size());
        assertTrue(texto().contains("DETALLE DE EQUIPAMIENTO")); assertTrue(texto().contains("Guardada"));
        assertTrue(texto().contains("1. Menú de personaje")); assertTrue(texto().contains("2. Cambiar de planta"));
        assertTrue(texto().contains("3. Explorar")); assertTrue(texto().contains("4. Tienda")); assertTrue(texto().contains("5. Guardar"));
    }

    @Test void dosBotinesIgualesPermitenMejorarEquiparPrimeroYVenderSegundoSinInterferencias() {
        Jugador j = jugador(); resolverAlInicio(j);
        var objetos = new RepositorioObjetosMemoria();
        var azar = new Tiradas(0, 0, 0, 1, 0, 0, 0, 0);
        assertFalse(aventura(j, "3\n2\n3\n2\n0\n", enemigo(), objetos, azar,
                ninguno(), ninguno(), ninguno(), new int[]{0}, new int[]{0}));
        azar.agotadas(); assertEquals(2, j.getInventarioEquipo().size());
        Equipo primera = j.getInventarioEquipo().get(0), segunda = j.getInventarioEquipo().get(1);
        assertNotSame(primera, segunda); assertEquals(primera.getNombre(), segunda.getNombre());
        assertEquals(primera.getIdDefinicion(), segunda.getIdDefinicion());
        assertEquals(1, primera.getMejorasDisponibles()); assertEquals(0, segunda.getMejorasDisponibles());
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(j.usarPergamino(Atributo.FUERZA, primera));
        assertEquals(Map.of(Atributo.FUERZA, 2), primera.getBonificaciones());
        assertEquals(Map.of(Atributo.FUERZA, 1), segunda.getBonificaciones());
        assertEquals(0, primera.getMejorasDisponibles()); assertEquals(0, segunda.getMejorasDisponibles());
        assertEquals(0, segunda.getMejorasConsumidas()); assertEquals(6, j.getFuerza());
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0)); assertEquals(8, j.getFuerza());
        assertSame(segunda, j.getInventarioEquipo().getFirst());
        int oro = j.getOro(); assertEquals(25, oro);
        assertTrue(app.consultarTienda(j, new Scanner("2\n1\n1\n0\n0\n"), salida(), new RepositorioTiendaMemoria(objetos), 5));
        assertSame(primera, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertTrue(j.getInventarioEquipo().isEmpty()); assertEquals(oro + 6, j.getOro());
        assertEquals(8, j.getFuerza()); assertEquals(Map.of(Atributo.FUERZA, 2), primera.getBonificaciones());
        assertEquals(1, primera.getMejorasConsumidas()); assertEquals(0, primera.getMejorasDisponibles());
        assertFalse(j.venderEquipo(segunda, false, new RepositorioTiendaMemoria(objetos)));
        assertEquals(oro + 6, j.getOro());
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void pocionesConservanSusDosTiradasAunqueTambienSeObtengaEquipo(int caso) {
        Jugador j = jugador(); resolverAlInicio(j);
        var pociones = new Tiradas((caso & 1) != 0 ? 49 : 50, (caso & 2) != 0 ? 19 : 20);
        var equipo = new Tiradas(0, 0, 2, 1);
        assertFalse(aventura(j, "3\n2\n0\n", enemigo(), new RepositorioObjetosMemoria(), equipo,
                ninguno(), ninguno(), pociones, new int[]{0}, new int[]{0}));
        pociones.agotadas(); equipo.agotadas(); assertEquals(List.of(100, 100), pociones.limites);
        assertEquals((caso & 1) != 0 ? 1 : 0, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals((caso & 2) != 0 ? 1 : 0, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(1, j.getInventarioEquipo().size()); assertEquals(TipoEquipo.CASCO, j.getInventarioEquipo().getFirst().getTipoEquipo());
        assertEquals(2, j.getResistencia()); assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test void aceptarLaEspadaFijaNoEquipaNiSustituyeLaPiezaAleatoria() {
        Jugador j = jugador(); var equipo = new Tiradas(19, 0, 2, 1);
        assertFalse(aventura(j, "3\n1\n1\n0\n", enemigo(), new RepositorioObjetosMemoria(), equipo,
                ninguno(), ninguno(), ninguno(), new int[]{0}, new int[]{0}));
        equipo.agotadas(); assertEquals(1, j.getEquipoEquipado().size()); assertEquals(1, j.getInventarioEquipo().size());
        assertEquals("espada_desgastada", j.getEquipoEquipado().get(TipoEquipo.ARMA).getIdDefinicion());
        assertEquals("casco_cuero", j.getInventarioEquipo().getFirst().getIdDefinicion());
        assertEquals(7, j.getFuerza()); assertEquals(2, j.getResistencia());
    }

    @ParameterizedTest @ValueSource(strings = {"derrota", "simultanea", "huida", "interrumpido", "sin explorar"})
    void sinVictoriaNoSeConsultaElGeneradorNiSeConcedeEquipo(String caso) {
        Jugador j = jugador();
        if (caso.equals("derrota") || caso.equals("simultanea")) j.aprenderHabilidad(new Habilidad("Final controlado de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) { p.setVida(0); if (caso.equals("simultanea")) o.setVida(0); }
        }, 1);
        String entrada = switch (caso) { case "huida" -> "3\n3\n0\n"; case "sin explorar" -> "5\n0\n"; default -> "3\n"; };
        var azar = new Tiradas();
        aventura(j, entrada, enemigo(), new RepositorioObjetosMemoria(), azar, ninguno(), ninguno(), ninguno(), new int[]{0}, new int[]{0});
        azar.agotadas(); assertTrue(j.getInventarioEquipo().isEmpty());
        assertEquals(15, j.getOro()); assertEquals(0, j.getExperiencia()); assertFalse(texto().contains("BOTÍN DE EQUIPAMIENTO"));
    }

    @Test void guardianUsaUnaSolaTiradaDeVictoriaYNivelDelEnemigo() {
        Jugador j = jugador(); resolverAlInicio(j);
        RepositorioEnemigos repo = () -> List.of(new DefinicionEnemigo("normal", "Normal", "", 1, 0, 1, 0, 0, 1, 1, 1, 1, false),
                new DefinicionEnemigo("guardian", "Guardián externo", "", 1, 0, 1, 0, 0, 1, 1, 4, 2, true));
        var azar = new Tiradas(99, 99, 99, 19, 10, 0, 2);
        assertFalse(aventura(j, "3\n2\n3\n2\n3\n2\n2\n6\n2\n2\n0\n", repo, new RepositorioObjetosMemoria(),
                azar, ninguno(), ninguno(), ninguno(), new int[]{0}, new int[]{0}));
        azar.agotadas(); assertEquals(List.of(100, 100, 100, 100, 15, 6, 3), azar.limites);
        assertEquals(1, j.getNivel()); assertEquals(1, j.getInventarioEquipo().size());
        Equipo pieza = j.getInventarioEquipo().getFirst(); assertEquals("espada_hierro", pieza.getIdDefinicion());
        assertEquals(2, pieza.getMejorasDisponibles()); assertEquals(1, texto().split("BOTÍN DE EQUIPAMIENTO", -1).length - 1);
        assertTrue(texto().contains("Planta 3 - Las criptas"));
    }

    @Test void primeraMuerteResucitadaNoGeneraEquipoHastaLaVictoriaDefinitiva() {
        Jugador j = jugador(); int[] muertes = {0};
        j.aprenderHabilidad(new Habilidad("Preparación controlada de prueba") {
            public void alInicioCombate(Combatiente p, Combatiente o, EstadoHabilidad e) {
                o.aprenderHabilidad(new Habilidad("Resurrección controlada de prueba") {
                    public void alMorir(Combatiente p, Combatiente o, EstadoHabilidad e) { if (++muertes[0] == 1) p.setVida(1); }
                }, 1);
            }
        }, 1);
        var secuencia = new Tiradas(0, 0, 0, 1);
        Random azar = new Random(0) { public int nextInt(int limite) { assertEquals(2, muertes[0]); return secuencia.nextInt(limite); } };
        int[] dados = {0};
        assertFalse(aventura(j, "3\n1\n1\n2\n0\n", enemigo(), new RepositorioObjetosMemoria(), azar,
                ninguno(), ninguno(), ninguno(), dados, new int[]{0}));
        secuencia.agotadas(); assertEquals(2, muertes[0]); assertEquals(14, dados[0]); assertEquals(1, j.getInventarioEquipo().size());
    }

    @Test void mismoGeneradorDuranteLaSesionYNuevaAventuraNoHeredaEquipo() {
        RepositorioEnemigos repo = () -> List.of(new DefinicionEnemigo("normal", "Normal", "", 1, 0, 1, 0, 0, 1, 1),
                new DefinicionEnemigo("letal", "Letal", "", 100, 0, 100, 0, 0, 1, 1));
        var encuentros = new Tiradas(0, 1, 0); var azarEquipo = new Tiradas(0, 0, 0, 1, 99);
        String entrada = "1\nAna\n3\n1\n2\n3\n1\nLuis\n3\n1\n2\n1\n2\n0\n0\n0\n";
        int[] dados = {0};
        app.ejecutar(new Scanner(entrada), salida(), () -> { dados[0]++; return 4; }, ninguno(), repo, encuentros,
                new RepositorioObjetosMemoria(), null, new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA), ninguno(), ninguno(), azarEquipo);
        azarEquipo.agotadas(); encuentros.agotadas(); assertEquals(18, dados[0]);
        assertEquals(1, texto().split("BOTÍN DE EQUIPAMIENTO", -1).length - 1);
        assertTrue(texto().contains("Derrota."));
        String segunda = texto().substring(texto().lastIndexOf("ENCUENTRO 1"));
        assertTrue(segunda.contains("Luis")); assertTrue(segunda.contains("Sin equipamiento"));
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
    }

    @Test void equipoObtenidoSeEquipaMejoraDesequipaYVendeConValoracionGlobal() {
        Jugador j = jugador(); resolverAlInicio(j); var objetos = new RepositorioObjetosMemoria();
        var azar = new Tiradas(0, 0, 0, 1);
        assertFalse(aventura(j, "3\n2\n0\n", enemigo(), objetos, azar, ninguno(), ninguno(), ninguno(), new int[]{0}, new int[]{0}));
        Equipo pieza = j.getInventarioEquipo().getFirst(); assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertEquals(7, j.getFuerza());
        j.agregarPergamino(new Pergamino(Atributo.FUERZA)); assertTrue(j.usarPergamino(Atributo.FUERZA, pieza)); assertEquals(8, j.getFuerza());
        assertTrue(app.consultarInventario(j, new Scanner("3\n1\n1\n0\n"), salida()));
        assertEquals(6, j.getFuerza()); assertSame(pieza, j.getInventarioEquipo().getFirst());
        assertTrue(app.consultarTienda(j, new Scanner("2\n1\n1\n0\n0\n"), salida(), new RepositorioTiendaMemoria(objetos), 5));
        assertEquals(26, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty());
        assertTrue(texto().contains("Has vendido Espada desgastada por 6 monedas"));
        assertEquals(1, pieza.getMejorasConsumidas()); azar.agotadas();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void catalogoVacioOAjenoAPlantaAvisaSinBloquearLaVictoria(boolean vacio) {
        var objetos = new RepositorioObjetosMemoria() {
            public List<DefinicionBotinEquipo> obtenerBotinEquipamiento() {
                return vacio ? List.of() : super.obtenerBotinEquipamiento().stream().filter(d -> d.calidad() == CalidadEquipo.SUPERIOR).toList();
            }
        };
        Jugador j = jugador(); var azar = new Tiradas(0); int[] dados = {0};
        assertFalse(aventura(j, "3\n1\n2\n0\n", enemigo(), objetos, azar, ninguno(), ninguno(), ninguno(), dados, new int[]{0}));
        azar.agotadas(); assertTrue(j.getInventarioEquipo().isEmpty()); assertEquals(20, j.getOro()); assertEquals(6, dados[0]);
        assertTrue(texto().contains("Botín de equipamiento no disponible en plantas:")); assertFalse(texto().contains("BOTÍN DE EQUIPAMIENTO"));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void metadatosInvalidosSeDetectanAlIniciarSinConsumirEntradasNiAleatoriedad(int caso) {
        var objetos = new RepositorioObjetosMemoria() {
            public List<DefinicionBotinEquipo> obtenerBotinEquipamiento() {
                var entrada = super.obtenerBotinEquipamiento().getFirst();
                return switch (caso) {
                    case 0 -> null;
                    case 1 -> List.of(entrada, entrada);
                    case 2 -> Arrays.asList(entrada, null);
                    default -> List.of(new DefinicionBotinEquipo(new DefinicionEquipo("desconocido", "Desconocido", TipoEquipo.ARMA, Map.of()), CalidadEquipo.BASICO, 1, 1));
                };
            }
        };
        Jugador j = jugador(); var azar = new Tiradas(); var entrada = new Scanner("3\n");
        assertTrue(app.jugarAventura(j, entrada, salida(), () -> { fail("No hay combate"); return 1; }, ninguno(), enemigo(),
                new Random(0), objetos, null, new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA), ninguno(), ninguno(), new TiempoAventura(() -> 0), azar));
        assertEquals("3", entrada.nextLine()); azar.agotadas();
        assertEquals(15, j.getOro()); assertEquals(0, j.getExperiencia()); assertTrue(j.getInventarioEquipo().isEmpty());
        assertTrue(texto().contains("El catálogo de objetos no es válido")); assertFalse(texto().contains("EXPLORACIÓN"));
    }
}
