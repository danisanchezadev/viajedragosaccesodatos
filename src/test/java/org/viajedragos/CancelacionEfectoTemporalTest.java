package org.viajedragos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import org.viajedragos.modelo.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CancelacionEfectoTemporalTest {
    @ParameterizedTest
    @ValueSource(strings = {"2\n0\n0\n", "2\n1\n0\n0\n0\n", "2\n1\n1\n0\n0\n0\n"})
    void cancelarConInstintoActivoConservaElEfectoYPermiteRetirarloDespues(String entrada) {
        Jugador j = new Jugador("Prueba", "",6,2,3,2,2,1,100);
        var h = new InstintoSupervivencia();
        Equipo actual = new Arma("Actual",TipoEquipo.ARMA,Map.of(Atributo.FUERZA,1),Map.of(h,1));
        Equipo nueva = new Arma("Nueva",TipoEquipo.ARMA,Map.of(Atributo.FUERZA,2),Map.of());
        j.equipar(actual);
        j.agregarEquipo(nueva);
        j.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(8,j.usarPocion(TipoPocion.PEQUENA));
        var estado = j.getHabilidades().get(h);
        assertTrue(app.consultarInventario(j,new Scanner(entrada),new PrintStream(new ByteArrayOutputStream())));
        assertSame(actual,j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(nueva),j.getInventarioEquipo());
        assertSame(estado,j.getHabilidades().get(h));
        assertEquals(1,estado.getNivel());
        assertEquals(1,estado.getVecesEquipada());
        assertEquals(9,j.getVida());
        assertEquals(10,j.getFuerza());
        j.curarse(30);
        assertEquals(6,j.getAtributoBase(Atributo.FUERZA));
        assertEquals(7,j.getFuerza());
    }
}
