package com.franchee.misiones.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Identifica los menus del editor y guarda su estado (que mision se edita, pagina). */
public class EditorHolder implements InventoryHolder {

    public enum Tipo { LISTA, MISION, RECOMPENSAS, ESPECIALES, CONFIRMAR_BORRADO }

    private final Tipo tipo;
    private final String misionId;
    private int pagina = 0;
    private Inventory inventario;

    public EditorHolder(Tipo tipo, String misionId) {
        this.tipo = tipo;
        this.misionId = misionId;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** null en el menu LISTA. */
    public String getMisionId() {
        return misionId;
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
