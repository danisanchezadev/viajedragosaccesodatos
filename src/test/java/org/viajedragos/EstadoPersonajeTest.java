package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.arma.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.*;
import org.viajedragos.consola.PanelConsola;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EstadoPersonajeTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)).replaceAll("\\s+", " "); }
    private Jugador jugador() { return new Jugador("María del Río", "Exploradora", 6, 2, 3, 2, 2, 20, 48); }
    private Equipo pieza(String nombre, TipoEquipo tipo, Map<Atributo, Integer> bonos, Map<Habilidad, Integer> habilidades) {
        return new Equipo(null, nombre, tipo, bonos, habilidades, 3);
    }
    private void mostrar(Jugador j) {
        bytes.reset(); assertTrue(app.consultarEstado(j, new Scanner("0\n"), salida()));
        bytes.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("│") || l.startsWith("┌") || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> assertEquals(50, PanelConsola.anchoVisible(l), l));
    }
    private List<String> filasAtributos() {
        return bytes.toString(StandardCharsets.UTF_8).lines()
                .dropWhile(l -> !l.contains("ATRIBUTOS")).skip(1)
                .takeWhile(l -> !l.startsWith("├")).toList();
    }
    private void progreso(String titulo, String valor, String barra) {
        var filas = bytes.toString(StandardCharsets.UTF_8).lines().toList();
        int inicio = titulo.equals("VIDA") ? 3 : 26;
        for (int i = 0; i < filas.size() - 1; i++) {
            String fila = filas.get(i);
            if (!fila.startsWith("│") || fila.length() < 50) continue;
            if (!fila.substring(inicio, inicio + 21).strip().equals(titulo + " " + valor)) continue;
            assertEquals(barra, filas.get(i + 1).substring(inicio, inicio + 21).strip());
            assertTrue(fila.contains("VIDA ") && fila.contains("EXPERIENCIA "));
            return;
        }
        fail("No aparece el progreso en su columna: " + titulo + " " + valor);
    }
    private void atributo(Jugador j, String nombre, int base, int equipo, int total) {
        Atributo atributo = nombre.equals("VIDA MÁXIMA") ? Atributo.VIDA : Atributo.valueOf(nombre);
        assertEquals(base, atributo == Atributo.VIDA ? j.getVidaMaximaBase() : j.getAtributoBase(atributo));
        assertEquals(equipo, j.getModificadoresEquipo().getOrDefault(atributo, 0));
        var filas = filasAtributos();
        assertEquals(6, filas.size());
        String etiqueta = nombre.equals("VIDA MÁXIMA") && (nombre + " " + total).length() > 20 ? "VIDA MÁX." : nombre;
        boolean encontrado = false;
        for (int fila = 0; fila < 6; fila += 2) {
            for (int columna = 0; columna < 2; columna++) {
                int inicio = columna == 0 ? 3 : 27, fin = columna == 0 ? 25 : 47;
                String celda = filas.get(fila).substring(inicio, fin).strip();
                if (!celda.equals(etiqueta + " " + total)) continue;
                assertEquals(app.barraAtributoEstado(total), filas.get(fila + 1).substring(inicio, fin).strip());
                encontrado = true;
            }
        }
        assertTrue(encontrado, nombre + " " + total);
        assertFalse(texto().contains("Base:"));
        assertFalse(texto().contains("MODIFICADORES DE EQUIPO"));
    }

    @Test void estadoBasicoMuestraTodosLosDatosYSeisAtributosSinConfundirVida() {
        Jugador j = jugador(); mostrar(j);
        assertTrue(texto().contains("Nombre: María del Río")); assertTrue(texto().contains("Descripción: Exploradora"));
        assertTrue(texto().contains("Nivel: 1")); assertTrue(texto().contains("EXPERIENCIA 0/20"));
        assertFalse(texto().contains("Puntos pendientes:")); assertEquals(0, j.getPuntosPendientes());
        assertTrue(texto().contains("VIDA 20/48 PS")); assertTrue(texto().contains("Oro: 15 monedas"));
        atributo(j, "FUERZA", 6, 0, 6); atributo(j, "RESISTENCIA", 2, 0, 2); atributo(j, "INICIATIVA", 3, 0, 3);
        atributo(j, "AGILIDAD", 2, 0, 2); atributo(j, "VELOCIDAD", 2, 0, 2); atributo(j, "VIDA MÁXIMA", 48, 0, 48);
        assertTrue(texto().contains("No tienes habilidades aprendidas."));
        assertTrue(texto().contains("No tienes habilidades exclusivas de equipo."));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("[0] Volver a exploración"));
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\u001b"));
    }

    @Test void experienciaRepartoBonosAcumuladosYPergaminosUsanLasFuentesDelModelo() {
        Jugador j = jugador(); j.agregarExperiencia(25);
        assertTrue(j.asignarPunto(Atributo.FUERZA)); assertTrue(j.asignarPunto(Atributo.FUERZA));
        var arma = pieza("Espada", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of());
        j.equipar(arma);
        j.equipar(pieza("Guantes", TipoEquipo.GUANTES, Map.of(Atributo.FUERZA, 1), Map.of()));
        j.equipar(pieza("Anillo", TipoEquipo.ANILLO, Map.of(Atributo.INICIATIVA, 1), Map.of()));
        j.equipar(pieza("Botas", TipoEquipo.BOTAS, Map.of(Atributo.AGILIDAD, -1), Map.of()));
        j.equipar(pieza("Casco", TipoEquipo.CASCO, Map.of(Atributo.RESISTENCIA, -5), Map.of()));
        j.equipar(pieza("Armadura", TipoEquipo.ARMADURA, Map.of(Atributo.VIDA, 10), Map.of()));
        j.equipar(pieza("Amuleto", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, -2), Map.of()));
        j.agregarPergamino(new Pergamino(Atributo.FUERZA)); assertTrue(j.usarPergamino(Atributo.FUERZA, arma));
        mostrar(j);
        atributo(j, "FUERZA", 9, 4, 13); atributo(j, "RESISTENCIA", 3, -5, 0); atributo(j, "INICIATIVA", 4, 1, 5);
        atributo(j, "AGILIDAD", 3, -1, 2); atributo(j, "VELOCIDAD", 3, 0, 3); atributo(j, "VIDA MÁXIMA", 54, 8, 62);
        assertTrue(texto().contains("Nivel: 2")); assertTrue(texto().contains("EXPERIENCIA 5/30"));
        assertTrue(texto().contains("VIDA 26/62 PS"));
        assertEquals(0, j.getResistencia());
    }

    @Test void muestraLosPuntosPendientesSinAsignarlos() {
        Jugador j = jugador(); j.agregarExperiencia(20); mostrar(j);
        assertTrue(texto().contains("Puntos pendientes: 2")); assertEquals(2, j.getPuntosPendientes());
        atributo(j, "FUERZA", 7, 0, 7);
    }

    @Test void actualizaDespuesDeEquiparSustituirYDesequiparSinPerderLaBase() {
        Jugador j = jugador(); j.aprenderHabilidad(new Caparazon(), 2);
        var anterior = pieza("Anterior", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(new Caparazon(), 2));
        var nueva = pieza("Nueva", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 4), Map.of(new Caparazon(), 3));
        j.agregarEquipo(anterior); j.agregarEquipo(nueva);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0)); mostrar(j);
        atributo(j, "FUERZA", 6, 1, 7); assertTrue(texto().contains("Permanente: 2 | Equipo: +2 | Total: 4"));
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0)); mostrar(j);
        atributo(j, "FUERZA", 6, 4, 10); assertTrue(texto().contains("Permanente: 2 | Equipo: +3 | Total: 5"));
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); mostrar(j);
        atributo(j, "FUERZA", 6, 0, 6); assertTrue(texto().contains("Permanente: 2 | Equipo: +0 | Total: 2"));
        var e = j.getHabilidades().get(new Caparazon());
        assertTrue(e.isAprendida()); assertEquals(2, e.getNivelPermanente()); assertEquals(0, e.getNivelEquipo());
        assertEquals(2, j.getInventarioEquipo().size());
    }

    @Test void separaAprendidasDeEquipoYNoDuplicaHabilidadConVariasFuentes() {
        Jugador j = jugador(); var caparazon = new Caparazon(); var instinto = new InstintoSupervivencia();
        j.aprenderHabilidad(caparazon, 2);
        j.equipar(pieza("Espada", TipoEquipo.ARMA, Map.of(), Map.of(caparazon, 2, instinto, 1)));
        j.equipar(pieza("Anillo", TipoEquipo.ANILLO, Map.of(), Map.of(caparazon, 2, instinto, 3)));
        mostrar(j);
        assertEquals(1, texto().split("Caparazón", -1).length - 1);
        assertTrue(texto().contains("Caparazón Permanente: 2 | Equipo: +4 | Total: 6"));
        assertEquals(1, texto().split("Instinto de supervivencia", -1).length - 1);
        assertTrue(texto().contains("Nivel de equipo: 4"));
        assertTrue(texto().contains("Fuente: Espada (+1)")); assertTrue(texto().contains("Fuente: Anillo (+3)"));
        assertTrue(texto().indexOf("Caparazón") < texto().indexOf("HABILIDADES OTORGADAS POR EQUIPO"));
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); assertTrue(j.desequiparAlInventario(TipoEquipo.ANILLO));
        mostrar(j); assertFalse(texto().contains("Instinto de supervivencia"));
        assertTrue(texto().contains("Permanente: 2 | Equipo: +0 | Total: 2"));
    }

    @Test void habilidadAprendidaPorPergaminoSeMuestraConElNivelPermanenteReal() {
        Jugador j = jugador(); j.equipar(pieza("Espada", TipoEquipo.ARMA, Map.of(), Map.of(new Caparazon(), 2)));
        j.agregarPergaminoMaestria(); assertTrue(j.usarPergaminoMaestria(HabilidadDisponible.CAPARAZON));
        mostrar(j); assertTrue(texto().contains("Permanente: 1 | Equipo: +2 | Total: 3"));
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); mostrar(j);
        assertTrue(texto().contains("Permanente: 1 | Equipo: +0 | Total: 1"));
    }

    @Test void consultarNoDisparaCuracionNiLimpiezaNiCambiaInstanciasORecursos() {
        Jugador j = jugador(); j.aprenderHabilidad(new InstintoSupervivencia(), 2);
        j.setVida(1); j.curarse(8); j.agregarExperiencia(1); j.agregarPocion(TipoPocion.PEQUENA);
        j.agregarPergamino(new Pergamino(Atributo.VIDA)); j.agregarPergaminoMaestria();
        var arma = pieza("Espada", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of(new InstintoSupervivencia(), 3));
        j.equipar(arma); j.agregarEquipo(pieza("Guardada", TipoEquipo.CASCO, Map.of(), Map.of()));
        var atributos = new EnumMap<Atributo, Integer>(Atributo.class);
        for (Atributo a : Atributo.values()) if (a != Atributo.VIDA) atributos.put(a, j.getAtributoBase(a));
        var mods = Map.copyOf(j.getModificadoresEquipo()); var ranuras = Map.copyOf(j.getEquipoEquipado());
        var guardadas = List.copyOf(j.getInventarioEquipo()); var estado = j.getHabilidades().get(new InstintoSupervivencia());
        for (int i = 0; i < 3; i++) mostrar(j);
        atributos.forEach((a, valor) -> assertEquals(valor, j.getAtributoBase(a)));
        assertEquals(mods, j.getModificadoresEquipo()); assertEquals(ranuras, j.getEquipoEquipado());
        assertEquals(guardadas, j.getInventarioEquipo()); assertSame(estado, j.getHabilidades().get(new InstintoSupervivencia()));
        assertTrue(estado.isEfectoTemporalActivo()); assertEquals(2, estado.getNivelPermanente());
        assertEquals(3, estado.getNivelEquipo()); assertEquals(5, estado.getNivel());
        assertEquals(9, j.getVida()); assertEquals(48, j.getVidaMaxima()); assertEquals(15, j.getOro());
        assertEquals(1, j.getExperiencia()); assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, j.getCantidadPergaminos(Atributo.VIDA)); assertEquals(1, j.getCantidadPergaminosMaestria());
        assertEquals(3, arma.getMejorasDisponibles()); assertFalse(texto().contains("Efectos temporales activos"));
    }

    @ParameterizedTest @ValueSource(strings = {"", "x\n", "1\n"})
    void eofSaleSinModificarAunqueHayaEntradasInvalidas(String entrada) {
        Jugador j = jugador(); assertFalse(app.consultarEstado(j, new Scanner(entrada), salida()));
        assertEquals(20, j.getVida()); assertEquals(15, j.getOro()); assertEquals(6, j.getFuerza());
        assertTrue(texto().contains("Entrada finalizada"));
    }

    @Test void entradasInvalidasPermitenVolverSinConsumirLaSiguienteOpcion() {
        var entrada = new Scanner("x\n1\n-1\n0\npendiente\n");
        assertTrue(app.consultarEstado(jugador(), entrada, salida())); assertEquals("pendiente", entrada.nextLine());
        assertEquals(3, texto().split("Entrada inválida", -1).length - 1);
    }

    @Test void nombresLargosDescripcionAusenteYNivelesGrandesNoDesbordanElPanel() {
        Jugador j = jugador(); j.setNombre("a".repeat(30)); j.setDescripcion("");
        j.aprenderHabilidad(new Caparazon(), Integer.MAX_VALUE);
        mostrar(j); assertFalse(texto().contains("Descripción:"));
        assertTrue(texto().contains("Permanente: 2147483647"));
    }

    @ParameterizedTest
    @CsvSource({"0,100,0", "50,100,5", "100,100,10", "150,100,10", "-1,100,0",
            "10,0,0", "10,-1,0", "1,3,3", "2,3,7", "4611686018427387903,9223372036854775807,5",
            "9223372036854775807,9223372036854775807,10", "-9223372036854775808,9223372036854775807,0"})
    void barrasProporcionalesAcotadasSinDesbordamientos(long actual, long maximo, int llenos) {
        String barra = app.barraProgresoEstado(actual, maximo);
        assertEquals("[" + "█".repeat(llenos) + "░".repeat(10 - llenos) + "]", barra);
        assertEquals(12, barra.length());
    }

    @ParameterizedTest
    @CsvSource({"-2147483648,0", "-5,0", "0,0", "1,1", "8,8", "16,16", "17,16", "2147483647,16"})
    void escalaDeAtributosEsVisualYSeSaturaEnDieciseis(int valor, int llenos) {
        assertEquals("[" + "█".repeat(llenos) + "░".repeat(16 - llenos) + "]", app.barraAtributoEstado(valor));
    }

    @Test void atributosGrandesConservanSuNumeroRealSinDesbordarColumnas() {
        Jugador j = jugador(); j.setFuerza(Integer.MAX_VALUE); j.setResistencia(-3); j.setVelocidad(0);
        mostrar(j); atributo(j, "FUERZA", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        atributo(j, "RESISTENCIA", -3, 0, 0); atributo(j, "VELOCIDAD", 0, 0, 0);
        assertTrue(filasAtributos().get(1).contains("[████████████████]"));
        assertTrue(filasAtributos().get(3).contains("[░░░░░░░░░░░░░░░░]"));
        assertEquals(Integer.MAX_VALUE, j.getFuerza()); assertEquals(-3, j.getAtributoBase(Atributo.RESISTENCIA));
    }

    @Test void vidaUsaMaximoEfectivoYExperienciaElUmbralActualIncluyendoLaSubida() {
        Jugador j = jugador(); j.setVida(24); j.agregarExperiencia(10); mostrar(j);
        progreso("VIDA", "24/48 PS", "[█████░░░░░]");
        progreso("EXPERIENCIA", "10/20", "[█████░░░░░]");
        j.equipar(pieza("Amuleto", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 48), Map.of()));
        mostrar(j);
        progreso("VIDA", "24/96 PS", "[███░░░░░░░]"); atributo(j, "VIDA MÁXIMA", 48, 48, 96);
        j.agregarExperiencia(10); mostrar(j);
        progreso("EXPERIENCIA", "0/30", "[░░░░░░░░░░]");
        assertEquals(2, j.getNivel()); assertEquals(0, j.getExperiencia()); assertEquals(30, j.getVida());
    }

    @Test void valoresEfectivosIncluyenBonosNetosYMejorasSinMostrarDesglosesNiResumen() {
        Jugador j = jugador();
        var arma = pieza("Arma", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2, Atributo.RESISTENCIA, -1), Map.of());
        j.equipar(arma);
        j.equipar(pieza("Casco", TipoEquipo.CASCO, Map.of(Atributo.RESISTENCIA, 1, Atributo.VIDA, 5), Map.of()));
        j.agregarPergamino(new Pergamino(Atributo.FUERZA)); assertTrue(j.usarPergamino(Atributo.FUERZA, arma));
        mostrar(j);
        assertFalse(texto().contains("MODIFICADORES DE EQUIPO"));
        assertFalse(texto().contains("Base:"));
        atributo(j, "FUERZA", 6, 3, 9); atributo(j, "RESISTENCIA", 2, 0, 2);
        atributo(j, "VIDA MÁXIMA", 48, 5, 53);
        j.desequipar(TipoEquipo.ARMA); j.desequipar(TipoEquipo.CASCO); mostrar(j);
        atributo(j, "FUERZA", 6, 0, 6); atributo(j, "VIDA MÁXIMA", 48, 0, 48);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void ansiNoCambiaContenidoNiAnchuraYSeRestauraAntesDelBorde(boolean colores) {
        Jugador j = jugador(); j.agregarExperiencia(10); j.setVida(24);
        j.aprenderHabilidad(new Caparazon(), 1);
        j.equipar(pieza("Arma", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2, Atributo.AGILIDAD, -1), Map.of(new Caparazon(), 2)));
        mostrar(j); String sinColor = bytes.toString(StandardCharsets.UTF_8);
        bytes.reset(); assertTrue(app.consultarEstado(j, new Scanner("0\n"), salida(), colores));
        String conColor = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(sinColor, conColor.replaceAll("\u001b\\[[0-9;]*m", ""));
        assertEquals(colores, conColor.contains("\u001b["));
        if (colores) {
            assertTrue(conColor.contains("\u001b[32m[█████░░░░░]\u001b[0m"));
            assertTrue(conColor.contains("\u001b[36m[█████░░░░░]\u001b[0m"));
            assertTrue(conColor.contains("\u001b[32m+2\u001b[0m"));
            assertFalse(conColor.contains("\u001b[31m-1\u001b[0m"), "El bono de atributo ya no se muestra");
            assertFalse(conColor.contains("\u001b[32m+0"));
        }
        comprobarColoresYBordes(conColor);
        assertEquals(8, j.getFuerza()); assertEquals(1, j.getAgilidad()); assertEquals(10, j.getExperiencia());
    }

    @Test void atributosOcupanExactamenteTresParejasDeDosLineasEnElOrdenSolicitado() {
        Jugador j = jugador(); mostrar(j);
        var filas = filasAtributos();
        assertEquals(6, filas.size());
        assertEquals("FUERZA 6", filas.get(0).substring(3, 25).strip());
        assertEquals("AGILIDAD 2", filas.get(0).substring(27, 47).strip());
        assertEquals("RESISTENCIA 2", filas.get(2).substring(3, 25).strip());
        assertEquals("VELOCIDAD 2", filas.get(2).substring(27, 47).strip());
        assertEquals("INICIATIVA 3", filas.get(4).substring(3, 25).strip());
        assertEquals("VIDA MÁXIMA 48", filas.get(4).substring(27, 47).strip());
        assertEquals("[██████░░░░░░░░░░]", filas.get(1).substring(3, 25).strip());
        assertEquals("[██░░░░░░░░░░░░░░]", filas.get(1).substring(27, 47).strip());
        assertEquals("[████████████████]", filas.get(5).substring(27, 47).strip());
        assertFalse(texto().contains("Base:"));
        assertFalse(texto().contains("MODIFICADORES DE EQUIPO"));
    }

    @Test void seisValoresMaximosCabenEnDosLineasPorParejaSinRecortarCifras() {
        Jugador j = jugador();
        for (Atributo a : Atributo.values()) if (a != Atributo.VIDA) j.aumentarAtributo(a, Integer.MAX_VALUE - j.getAtributoBase(a));
        j.setVidaMaxima(Integer.MAX_VALUE); mostrar(j);
        for (String nombre : new String[]{"FUERZA", "AGILIDAD", "RESISTENCIA", "VELOCIDAD", "INICIATIVA", "VIDA MÁXIMA"}) {
            atributo(j, nombre, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        }
        for (int fila : new int[]{1, 3, 5}) assertEquals(2, filasAtributos().get(fila).split("████████████████", -1).length - 1);
        assertEquals(Integer.MAX_VALUE, j.getVidaMaxima());
    }

    private void comprobarColoresYBordes(String original) {
        original.lines().filter(l -> l.startsWith("│") || l.startsWith("┌") || l.startsWith("├") || l.startsWith("└"))
                .forEach(l -> {
                    assertEquals(50, PanelConsola.anchoVisible(l), l);
                    var colores = java.util.regex.Pattern.compile("\u001b\\[[0-9;]*m").matcher(l);
                    String ultimo = "";
                    while (colores.find()) ultimo = colores.group();
                    assertTrue(ultimo.isEmpty() || ultimo.equals("\u001b[0m"), "Color activo al llegar al borde: " + l);
                });
    }

    @Test void muchasHabilidadesConNombresLargosYFuentesLargasSeAjustanSinMutaciones() {
        Jugador j = jugador(); j.setNombre("María".repeat(6));
        String nombreLargo = "Habilidad de prueba con un nombre muy largo y sin efectos";
        Habilidad larga = new Habilidad(nombreLargo) {};
        for (var tipo : HabilidadDisponible.values()) j.aprenderHabilidad(tipo.crearHabilidad(), 2);
        j.aprenderHabilidad(larga, 1);
        String piezaLarga = "Espada de prueba con un nombre extraordinariamente largo";
        j.equipar(pieza(piezaLarga, TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of(larga, 1)));
        var mapa = Map.copyOf(j.getHabilidades());
        assertTrue(app.consultarEstado(j, new Scanner("0\n"), salida(), true));
        comprobarColoresYBordes(bytes.toString(StandardCharsets.UTF_8));
        assertTrue(texto().contains(nombreLargo)); assertEquals(14, j.getHabilidades().size());
        assertEquals(mapa, j.getHabilidades()); assertEquals(8, j.getFuerza());
        bytes.reset(); j.desequipar(TipoEquipo.ARMA); j.eliminarHabilidad(larga);
        j.equipar(pieza(piezaLarga, TipoEquipo.ARMA, Map.of(), Map.of(larga, 1)));
        assertTrue(app.consultarEstado(j, new Scanner("0\n"), salida(), true));
        comprobarColoresYBordes(bytes.toString(StandardCharsets.UTF_8));
        assertTrue(texto().contains("Fuente: " + piezaLarga));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void propiedadAnsiSeAplicaSoloALaConsolaYLaPruebaControlaSuValor(boolean habilitar) {
        String anterior = System.getProperty("viajedragos.ansi"); PrintStream salidaAnterior = System.out;
        try {
            System.setProperty("viajedragos.ansi", String.valueOf(habilitar));
            System.setOut(salida());
            assertTrue(app.consultarEstado(jugador(), new Scanner("0\n"), System.out));
            assertEquals(habilitar, bytes.toString(StandardCharsets.UTF_8).contains("\u001b["));
            bytes.reset();
            assertTrue(app.consultarEstado(jugador(), new Scanner("0\n"), salida()));
            assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("\u001b["));
        } finally {
            System.setOut(salidaAnterior);
            if (anterior == null) System.clearProperty("viajedragos.ansi"); else System.setProperty("viajedragos.ansi", anterior);
        }
    }
}
