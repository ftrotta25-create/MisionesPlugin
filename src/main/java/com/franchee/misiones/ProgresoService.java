package com.franchee.misiones;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/** Calcula el progreso de una mision y se encarga del reclamo de la recompensa. */
public class ProgresoService {

    public enum Resultado { OK, NO_ASIGNADA, YA_RECLAMADA, INCOMPLETA, RECOMPENSA_NO_DISPONIBLE }

    private final MisionesPlugin plugin;

    public ProgresoService(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    public int progreso(Player jugador, Mision m) {
        if (m.tipo() == Mision.TipoMision.DELIVER) {
            return Math.min(contarItems(jugador, m), m.cantidad());
        }
        return Math.min(plugin.getDatos().getProgreso(jugador.getUniqueId(), m.id()), m.cantidad());
    }

    public boolean completa(Player jugador, Mision m) {
        return progreso(jugador, m) >= m.cantidad();
    }

    private int contarItems(Player jugador, Mision m) {
        int total = 0;
        for (ItemStack item : jugador.getInventory().getStorageContents()) {
            if (item != null && m.objetivos().contains(item.getType().name())) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private boolean quitarItems(Player jugador, Mision m) {
        int restante = m.cantidad();
        ItemStack[] contenido = jugador.getInventory().getStorageContents();
        for (int i = 0; i < contenido.length && restante > 0; i++) {
            ItemStack item = contenido[i];
            if (item == null || !m.objetivos().contains(item.getType().name())) continue;

            int quitar = Math.min(restante, item.getAmount());
            restante -= quitar;
            if (quitar >= item.getAmount()) {
                contenido[i] = null;
            } else {
                item.setAmount(item.getAmount() - quitar);
            }
        }
        if (restante > 0) return false;
        jugador.getInventory().setStorageContents(contenido);
        return true;
    }

    public Resultado reclamar(Player jugador, Mision m) {
        UUID u = jugador.getUniqueId();
        DatosJugadores datos = plugin.getDatos();

        if (!datos.tieneAsignada(u, m.id())) return Resultado.NO_ASIGNADA;
        if (datos.estaReclamada(u, m.id())) return Resultado.YA_RECLAMADA;
        if (!completa(jugador, m)) return Resultado.INCOMPLETA;

        // Antes de cobrar nada, nos aseguramos de que la recompensa se pueda entregar.
        List<String> faltan = plugin.getRecompensas().noDisponibles(m.recompensas());
        if (!faltan.isEmpty()) {
            plugin.getLogger().warning("No se pudo entregar la misión '" + m.id() + "' a " + jugador.getName()
                    + ": recompensa(s) no disponible(s): " + faltan);
            return Resultado.RECOMPENSA_NO_DISPONIBLE;
        }

        if (m.tipo() == Mision.TipoMision.DELIVER && !quitarItems(jugador, m)) {
            return Resultado.INCOMPLETA;
        }

        datos.marcarReclamada(u, m.id());
        plugin.getRecompensas().entregar(jugador, m.recompensas());
        return Resultado.OK;
    }
}
