package org.viajedragos.modelo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.HashSet;
import java.util.ArrayList;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.arma.Atributo;
import org.viajedragos.catalogo.RepositorioTienda;

public class Jugador extends Combatiente{

    private final Map<TipoPocion, Integer> inventarioPociones = new EnumMap<>(TipoPocion.class);
    private Map<TipoPocion, DefinicionPocion> definicionesPociones;
    private final List<Equipo> inventarioEquipo = new ArrayList<>();
    private int oro = 15;
    private int nivel = 1;
    private long experiencia;
    private int puntosPendientes;

    public int getNivel() { return nivel; }
    public long getExperiencia() { return experiencia; }
    public long getExperienciaNecesaria() { return (nivel + 1L) * 10; }
    public int getPuntosPendientes() { return puntosPendientes; }

    // Devuelve los niveles alcanzados; no contiene entrada/salida ni reglas de recompensas.
    public int agregarExperiencia(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La experiencia debe ser positiva");
        long restante = Math.addExact(experiencia, cantidad);
        int nuevoNivel = nivel;
        while (restante >= (nuevoNivel + 1L) * 10) {
            restante -= (nuevoNivel + 1L) * 10;
            nuevoNivel = Math.incrementExact(nuevoNivel);
        }
        int subidas = nuevoNivel - nivel;
        int puntos = Math.addExact(puntosPendientes, Math.multiplyExact(subidas, 2));
        int vidaExtra = Math.multiplyExact(subidas, 6);
        int maxima = Math.addExact(getVidaMaxima(), vidaExtra);
        int actual = (int) Math.min(maxima, (long) getVida() + vidaExtra);
        // Validar todos los incrementos antes de modificar el estado.
        for (Atributo atributo : Atributo.values()) {
            if (atributo != Atributo.VIDA) Math.addExact(getAtributoBase(atributo), subidas);
        }
        if (subidas > 0) {
            for (Atributo atributo : Atributo.values()) {
                if (atributo != Atributo.VIDA) aumentarAtributo(atributo, subidas);
            }
            setVidaMaxima(maxima);
            setVida(actual);
        }
        nivel = nuevoNivel;
        experiencia = restante;
        puntosPendientes = puntos;
        return subidas;
    }

    // Cada llamada asigna exactamente un punto permanente.
    public boolean asignarPunto(Atributo atributo) {
        if (atributo == null || atributo == Atributo.VIDA || puntosPendientes == 0) return false;
        if (getAtributoBase(atributo) == Integer.MAX_VALUE) return false;
        aumentarAtributo(atributo, 1);
        puntosPendientes--;
        return true;
    }

    public int getOro() { return oro; }

    public void agregarOro(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad de oro debe ser positiva");
        oro = Math.addExact(oro, cantidad);
    }

    public boolean gastarOro(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad de oro debe ser positiva");
        if (cantidad > oro) return false;
        oro -= cantidad;
        return true;
    }

    public ResultadoCompra comprar(String id, RepositorioTienda repositorio) {
        ProductoTienda producto;
        try {
            producto = Objects.requireNonNull(repositorio).buscarPorId(id).orElse(null);
            if (producto == null) return ResultadoCompra.PRODUCTO_INEXISTENTE;
            if (!producto.id().equals(id)) return ResultadoCompra.PRODUCTO_INVALIDO;
            // El inventario cuenta por tipo: no puede almacenar otra definición del mismo tipo.
            if (producto.pocion() != null && !producto.pocion().equals(
                    getDefinicionPocion(producto.pocion().tipo()))) return ResultadoCompra.PRODUCTO_INVALIDO;
        } catch (RuntimeException datosInvalidos) {
            return ResultadoCompra.PRODUCTO_INVALIDO;
        }
        if (producto.precio() > oro) return ResultadoCompra.SALDO_INSUFICIENTE;

        int oroAntes = oro;
        var pocionesAntes = new EnumMap<>(inventarioPociones);
        var equipoAntes = new ArrayList<>(inventarioEquipo);
        try {
            if (producto.pocion() != null) {
                TipoPocion tipo = producto.pocion().tipo();
                var esperadas = new EnumMap<>(pocionesAntes);
                esperadas.put(tipo, Math.incrementExact(getCantidadPociones(tipo)));
                agregarPocion(tipo);
                if (!inventarioPociones.equals(esperadas) || !inventarioEquipo.equals(equipoAntes)) {
                    throw new IllegalStateException("No se incorporó la poción correctamente");
                }
            } else {
                Equipo arma = producto.arma().crearArma();
                var esperado = new ArrayList<>(equipoAntes);
                esperado.add(arma);
                agregarEquipo(arma);
                if (!inventarioEquipo.equals(esperado) || !inventarioPociones.equals(pocionesAntes)) {
                    throw new IllegalStateException("No se incorporó el arma correctamente");
                }
            }
            if (!gastarOro(producto.precio())) throw new IllegalStateException("No se pudo descontar el oro");
            return ResultadoCompra.COMPRADA;
        } catch (RuntimeException falloIncorporacion) {
            oro = oroAntes;
            inventarioPociones.clear();
            inventarioPociones.putAll(pocionesAntes);
            inventarioEquipo.clear();
            inventarioEquipo.addAll(equipoAntes);
            return ResultadoCompra.INCORPORACION_FALLIDA;
        }
    }

    public void agregarEquipo(Equipo equipo) {
        inventarioEquipo.add(Objects.requireNonNull(equipo, "El equipo no puede ser null"));
    }

    // No se asigna un precio a objetos desconocidos ni a correspondencias ambiguas.
    private java.util.Optional<ProductoTienda> productoVenta(RepositorioTienda repositorio,
            java.util.function.Predicate<ProductoTienda> corresponde) {
        try {
            var productos = List.copyOf(repositorio.obtenerTodos());
            var ids = new HashSet<String>();
            for (var producto : productos) {
                if (!ids.add(producto.id())) return java.util.Optional.empty();
            }
            var candidatos = productos.stream().filter(corresponde).toList();
            if (candidatos.size() != 1) return java.util.Optional.empty();
            var producto = candidatos.getFirst();
            return repositorio.buscarPorId(producto.id()).filter(producto::equals);
        } catch (RuntimeException catalogoInvalido) {
            return java.util.Optional.empty();
        }
    }

    public java.util.Optional<ProductoTienda> productoVenta(TipoPocion tipo, RepositorioTienda repositorio) {
        return productoVenta(repositorio, p -> tipo != null && p.pocion() != null
                && p.pocion().equals(getDefinicionPocion(tipo)));
    }

    public java.util.Optional<ProductoTienda> productoVenta(Equipo equipo, RepositorioTienda repositorio) {
        return productoVenta(repositorio, p -> p.corresponde(equipo));
    }

    public boolean venderPocion(TipoPocion tipo, RepositorioTienda repositorio) {
        var producto = productoVenta(tipo, repositorio);
        if (producto.isEmpty() || getCantidadPociones(tipo) <= 0) return false;
        final int saldo;
        try { saldo = Math.addExact(oro, producto.get().precioVenta()); }
        catch (ArithmeticException exceso) { return false; }
        int cantidad = getCantidadPociones(tipo);
        if (cantidad == 1) inventarioPociones.remove(tipo);
        else inventarioPociones.put(tipo, cantidad - 1);
        oro = saldo;
        return true;
    }

    public boolean venderEquipo(Equipo equipo, boolean equipado, RepositorioTienda repositorio) {
        var producto = productoVenta(equipo, repositorio);
        if (producto.isEmpty()) return false;
        final int saldo;
        try { saldo = Math.addExact(oro, producto.get().precioVenta()); }
        catch (ArithmeticException exceso) { return false; }
        if (equipado) {
            if (getEquipoEquipado().get(equipo.getTipoEquipo()) != equipo) return false;
            // Una referencia duplicada manualmente no representa dos objetos físicos.
            if (inventarioEquipo.stream().anyMatch(pieza -> pieza == equipo)) return false;
            if (!desequiparSeguro(equipo.getTipoEquipo())) return false;
        } else {
            if (getEquipoEquipado().containsValue(equipo)) return false;
            if (inventarioEquipo.stream().filter(pieza -> pieza == equipo).count() != 1) return false;
            int indice = -1;
            for (int i = 0; i < inventarioEquipo.size(); i++) {
                if (inventarioEquipo.get(i) == equipo) { indice = i; break; }
            }
            if (indice < 0) return false;
            inventarioEquipo.remove(indice);
        }
        oro = saldo;
        return true;
    }

    public List<Equipo> getInventarioEquipo() {
        return Collections.unmodifiableList(inventarioEquipo);
    }

    // Índice de la lista, desde cero: solo se retira esa entrada, incluso si está repetida.
    public boolean equiparDesdeInventario(TipoEquipo tipo, int indice) {
        Objects.requireNonNull(tipo, "La ranura no puede ser null");
        if (indice < 0 || indice >= inventarioEquipo.size()) return false;
        Equipo nuevo = inventarioEquipo.get(indice);
        if (nuevo.getTipoEquipo() != tipo) return false;
        Equipo anterior = getEquipoEquipado().get(tipo);
        if (!cambiarEquipoSeguro(nuevo)) return false;
        inventarioEquipo.remove(indice);
        if (anterior != null) inventarioEquipo.add(anterior);
        return true;
    }

    public Jugador(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa, int agilidad, int velocidad, int vida, int vidaMaxima) {
        this(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima,
                pocionesPredeterminadas());
    }

    public Jugador(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa,
                   int agilidad, int velocidad, int vida, int vidaMaxima, List<DefinicionPocion> pociones) {
        super(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima);
        configurarPociones(pociones);
    }

    public Jugador(){
        configurarPociones(pocionesPredeterminadas());
    }

    private static List<DefinicionPocion> pocionesPredeterminadas() {
        return Arrays.stream(TipoPocion.values()).map(tipo -> new DefinicionPocion(
                "pocion_" + tipo.name().toLowerCase(Locale.ROOT), tipo,
                tipo.getNombre(), tipo.getRecuperacionPS())).toList();
    }

    // Se valida la copia completa antes de sustituir la configuración; el inventario no cambia.
    public void configurarPociones(List<DefinicionPocion> pociones) {
        var copia = new EnumMap<TipoPocion, DefinicionPocion>(TipoPocion.class);
        var ids = new HashSet<String>();
        for (DefinicionPocion definicion : List.copyOf(pociones)) {
            if (!ids.add(definicion.id()) || copia.putIfAbsent(definicion.tipo(), definicion) != null) {
                throw new IllegalArgumentException("Hay pociones con identificador o tipo repetido");
            }
        }
        if (copia.size() != TipoPocion.values().length) {
            throw new IllegalArgumentException("Falta la definición de alguna poción");
        }
        definicionesPociones = Collections.unmodifiableMap(copia);
    }

    public DefinicionPocion getDefinicionPocion(TipoPocion tipo) {
        return definicionesPociones.get(Objects.requireNonNull(tipo));
    }

    public void agregarPocion(TipoPocion tipo) {
        inventarioPociones.merge(Objects.requireNonNull(tipo), 1, Integer::sum);
    }

    public int getCantidadPociones(TipoPocion tipo) {
        return inventarioPociones.getOrDefault(Objects.requireNonNull(tipo), 0);
    }

    public Map<TipoPocion, Integer> getInventarioPociones() {
        return Collections.unmodifiableMap(inventarioPociones);
    }

    // Devuelve los PS recuperados; cero indica que no se ha consumido la poción.
    public int usarPocion(TipoPocion tipo) {
        int cantidad = getCantidadPociones(tipo);
        if (cantidad == 0 || getVida() >= getVidaMaxima()) return 0;

        int vidaAnterior = getVida();
        curarse(getDefinicionPocion(tipo).recuperacionPS());
        int recuperacion = getVida() - vidaAnterior;
        if (recuperacion > 0) {
            if (cantidad == 1) inventarioPociones.remove(tipo);
            else inventarioPociones.put(tipo, cantidad - 1);
        }
        return recuperacion;
    }

}
