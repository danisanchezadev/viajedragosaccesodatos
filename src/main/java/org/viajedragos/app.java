package org.viajedragos;

import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class app {

    public static void main(String[] args) {
        Map<Atributo, Integer> atributosEspada1 = new EnumMap<>(Atributo.class);
        atributosEspada1.put(Atributo.FUERZA, 5);
        atributosEspada1.put(Atributo.RESISTENCIA, 3);
        atributosEspada1.put(Atributo.INICIATIVA, 2);

        Map<Atributo, Integer> atributosEspada2 = new EnumMap<>(Atributo.class);
        atributosEspada2.put(Atributo.FUERZA, 8);
        atributosEspada2.put(Atributo.RESISTENCIA, -4);
        atributosEspada2.put(Atributo.AGILIDAD, 3);

        Arma espadaHierro = new Arma(
                "Espada de hierro",
                TipoEquipo.ARMA,
                atributosEspada1,
                new LinkedHashMap<>()
        );

        Arma espadaMaldita = new Arma(
                "Espada maldita",
                TipoEquipo.ARMA,
                atributosEspada2,
                new LinkedHashMap<>()
        );

// Comparamos la espada equipada con la nueva
        espadaHierro.compararCon(espadaMaldita);

    }


}
