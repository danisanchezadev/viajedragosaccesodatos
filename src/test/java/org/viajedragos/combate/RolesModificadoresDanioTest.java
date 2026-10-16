package org.viajedragos.combate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.habilidad.EstadoHabilidad;

import static org.junit.jupiter.api.Assertions.*;

/** Regresiones de los roles confirmados, conservando condiciones y magnitudes. */
class RolesModificadoresDanioTest {
    @ParameterizedTest(name = "{0}, propiedad del atacante: {1}, daño esperado: {4}")
    @CsvSource({
            "Caparazon,true,4,2,9", "Caparazon,false,4,2,6",
            "CastigoAlTorpe,true,4,1,21", "CastigoAlTorpe,false,4,1,17",
            "DefensaPerfecta,true,4,6,5", "DefensaPerfecta,false,4,6,2",
            "GolpeCertero,true,4,2,11", "GolpeCertero,false,4,2,9",
            "GuardiaFerrea,true,4,2,9", "GuardiaFerrea,false,4,2,7"
    })
    void aplicaExclusivamenteEnElRolConfirmado(
            String nombre, boolean propietarioAtacante, int ataque, int defensa, int danioEsperado) {
        Jugador j = new Jugador("Atacante", "", 10, 0, 3, 3, 0, 100, 100);
        Enemigo e = new Enemigo("Defensor", "", 5, 4, 1, 2, 0, 100, 100);
        Habilidad habilidad = habilidad(nombre);
        Combatiente propietario = propietarioAtacante ? j : e;
        // Solo Caparazón usa 25%; los otros conservan su magnitud interna.
        propietario.aprenderHabilidad(habilidad, 25);

        atacar(j, e, ataque, defensa);

        assertEquals(100 - danioEsperado, e.getVida());
        assertEquals(100, j.getVida());
        assertEquals(25, propietario.getHabilidades().get(habilidad).getNivel());
    }

    @ParameterizedTest
    @CsvSource({
            "Caparazon,false,4,2,100,0,100",
            "Caparazon,false,1,2,4,0,100",
            "Caparazon,false,1,6,4,0,95",
            "CastigoAlTorpe,true,4,2,4,9,100",
            "CastigoAlTorpe,true,1,1,4,0,100",
            "CastigoAlTorpe,true,1,6,4,0,95",
            "DefensaPerfecta,false,4,2,4,9,100",
            "DefensaPerfecta,false,4,1,4,17,100",
            "DefensaPerfecta,false,1,6,4,0,95",
            "GolpeCertero,true,1,1,4,0,100",
            "GolpeCertero,true,1,6,4,0,95",
            "GolpeCertero,true,4,2,100,2,100",
            "GuardiaFerrea,false,4,2,100,0,100",
            "GuardiaFerrea,false,1,2,4,0,100",
            "GuardiaFerrea,false,1,6,4,0,95",
            "GuardiaFerrea,false,2,6,5,0,100",
            "GuardiaFerrea,false,2,6,6,0,100"
    })
    void mantieneCondicionesFallosContraataquesYLimites(
            String nombre, boolean propietarioAtacante, int ataque, int defensa,
            int resistenciaDefensor, int danioEsperado, int vidaAtacante) {
        Jugador j = new Jugador("Atacante", "", 10, 0, 3, 3, 0, 100, 100);
        Enemigo e = new Enemigo("Defensor", "", 5, resistenciaDefensor, 1, 2, 0, 100, 100);
        (propietarioAtacante ? j : e).aprenderHabilidad(habilidad(nombre), 25);

        atacar(j, e, ataque, defensa);

        assertEquals(100 - danioEsperado, e.getVida());
        assertEquals(vidaAtacante, j.getVida());
    }

    @ParameterizedTest
    @CsvSource({
            "Caparazon,false,4,2,4", "Caparazon,true,4,2,4",
            "CastigoAlTorpe,false,4,1,21", "CastigoAlTorpe,true,4,1,21",
            "DefensaPerfecta,false,4,6,2", "DefensaPerfecta,true,4,6,2",
            "GolpeCertero,false,4,2,11", "GolpeCertero,true,4,2,11",
            "GuardiaFerrea,false,4,2,7", "GuardiaFerrea,true,4,2,7"
    })
    void ambosPuedenPoseerElMismoTipoOSuMismaInstanciaSinAplicarDosVeces(
            String nombre, boolean mismaInstancia, int ataque, int defensa, int danioEsperado) {
        Jugador j = new Jugador("Atacante", "", 10, 0, 3, 3, 0, 100, 100);
        Enemigo e = new Enemigo("Defensor", "", 5, 4, 1, 2, 0, 100, 100);
        Habilidad habilidadJ = habilidad(nombre);
        Habilidad habilidadE = mismaInstancia ? habilidadJ : habilidad(nombre);
        j.aprenderHabilidad(habilidadJ, 25);
        e.aprenderHabilidad(habilidadE, 50);
        EstadoHabilidad estadoJ = j.getHabilidades().get(habilidadJ);
        EstadoHabilidad estadoE = e.getHabilidades().get(habilidadE);
        assertNotSame(estadoJ, estadoE);

        atacar(j, e, ataque, defensa);

        assertEquals(100 - danioEsperado, e.getVida());
        assertEquals(100, j.getVida());
        assertSame(estadoJ, j.getHabilidades().get(habilidadJ));
        assertSame(estadoE, e.getHabilidades().get(habilidadE));
        assertEquals(25, estadoJ.getNivel());
        assertEquals(50, estadoE.getNivel());
    }

    @ParameterizedTest
    @CsvSource({"4,2,9,100", "6,1,23,100", "1,1,0,100", "1,6,0,95"})
    void conservaLaCombinacionOfensivaDefensivaYLasReglasEspeciales(
            int ataque, int defensa, int danioEsperado, int vidaAtacante) {
        Jugador j = new Jugador("Atacante", "", 10, 0, 3, 3, 0, 100, 100);
        Enemigo e = new Enemigo("Defensor", "", 5, 4, 1, 2, 0, 100, 100);
        j.aprenderHabilidad(new GolpeCertero(), 25);
        j.aprenderHabilidad(new GuardiaFerrea(), 25);
        e.aprenderHabilidad(new GuardiaFerrea(), 50);
        e.aprenderHabilidad(new GolpeCertero(), 50);

        if (ataque == 6 && defensa == 1) atacar(j, e, ataque, defensa, 4);
        else atacar(j, e, ataque, defensa);

        assertEquals(100 - danioEsperado, e.getVida());
        assertEquals(vidaAtacante, j.getVida());
    }

    private Habilidad habilidad(String nombre) {
        return switch (nombre) {
            case "Caparazon" -> new Caparazon();
            case "CastigoAlTorpe" -> new CastigoAlTorpe();
            case "DefensaPerfecta" -> new DefensaPerfecta();
            case "GolpeCertero" -> new GolpeCertero();
            case "GuardiaFerrea" -> new GuardiaFerrea();
            default -> throw new IllegalArgumentException(nombre);
        };
    }

    private void atacar(Combatiente atacante, Combatiente defensor, int... tiradas) {
        int[] usadas = {0};
        TiradorDado dados = () -> {
            assertTrue(usadas[0] < tiradas.length, "No debe aparecer otra secuencia de ataque");
            return tiradas[usadas[0]++];
        };

        new Combate(atacante, defensor, dados).ataque(atacante, defensor);
        assertEquals(tiradas.length, usadas[0]);
    }
}
