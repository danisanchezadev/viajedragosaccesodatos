package org.viajedragos.habilidad;

import java.util.function.Supplier;
import org.viajedragos.habilidad.implementacion.*;

/** Enumeración estable de habilidades jugables; no contiene estados de combatientes. */
public enum HabilidadDisponible {
    CAPARAZON(Caparazon::new),
    CASTIGO_AL_TORPE(CastigoAlTorpe::new),
    DEBILIDAD_ENEMIGA(DebilidadEnemiga::new),
    DEFENSA_PERFECTA(DefensaPerfecta::new),
    FURIA_CONTENIDA(FuriaContenida::new),
    GOLPE_CERTERO(GolpeCertero::new),
    GUARDIA_FERREA(GuardiaFerrea::new),
    IMPETU(Impetu::new),
    INSTINTO_SUPERVIVENCIA(InstintoSupervivencia::new),
    PRIMER_IMPULSO(() -> new PrimerImpulso(3)),
    RABIA(Rabia::new),
    SED_DE_BATALLA(SedDeBatalla::new),
    SEGUNDO_ALIENTO(() -> new SegundoAliento(2));

    private final Supplier<Habilidad> constructor;
    private final String nombre;

    HabilidadDisponible(Supplier<Habilidad> constructor) {
        this.constructor = constructor;
        nombre = constructor.get().getNombre();
    }

    public String getNombre() { return nombre; }
    public Habilidad crearHabilidad() { return constructor.get(); }
}
