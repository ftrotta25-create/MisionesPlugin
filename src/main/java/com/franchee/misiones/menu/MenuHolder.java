package com.franchee.misiones.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Identifica nuestros menus y guarda su estado (tipo, jugador objetivo, pagina). */
public class MenuHolder implements InventoryHolder {

    public enum Tipo { ASIGNAR_GLOBAL, ASIGNAR_JUGADOR, JUGADOR }

    private final Tipo tipo;
    private final UUID objetivo;
    private final String nombreObjetivo;
    private int pagina = 0;
    private Inventory inventario;

    public MenuHolder(Tipo tipo, UUID objetivo, String nombreObjetivo) {
        this.tipo = tipo;
        this.objetivo = objetivo;
        this.nombreObjetivo = nombreObjetivo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** ASIGNAR_JUGADOR: a quien se le asigna. JUGADOR: el dueño del menu. GLOBAL: null. */
    public UUID getObjetivo() {
        return objetivo;
    }

    public String getNombreObjetivo() {
        return nombreObjetivo;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = pagina;
    }

    public void setInventario(Inventory inventario) {
        this.inventario = inventario;
    }

    public Inventory getInventario() {
        return inventario;
    }

    @Override
    public Inventory getInventory() {
        return inventario;
    }
}
