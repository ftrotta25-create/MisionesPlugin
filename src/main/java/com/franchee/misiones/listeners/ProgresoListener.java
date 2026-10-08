package com.franchee.misiones.listeners;

import com.franchee.misiones.DatosJugadores;
import com.franchee.misiones.Mision;
import com.franchee.misiones.MisionesPlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Locale;
import java.util.UUID;

/** Suma progreso a las misiones de tipo BREAK y KILL. (DELIVER se calcula del inventario.) */
public class ProgresoListener implements Listener {

    private final MisionesPlugin plugin;

    public ProgresoListener(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean ignorar(Player jugador) {
        return jugador.getGameMode() == GameMode.CREATIVE
                && plugin.getConfig().getBoolean("ignorarCreativo", true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player jugador = e.getPlayer();
        if (ignorar(jugador)) return;
        sumar(jugador, Mision.TipoMision.BREAK, e.getBlock().getType().name(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        Player jugador = e.getEntity().getKiller();
        if (jugador == null || ignorar(jugador)) return;

        @SuppressWarnings("deprecation")
        String nombre = e.getEntity().getCustomName();
        if (nombre != null) nombre = ChatColor.stripColor(nombre);

        sumar(jugador, Mision.TipoMision.KILL, e.getEntityType().name(), nombre);
    }

    private void sumar(Player jugador, Mision.TipoMision tipo, String objetivo, String nombreEntidad) {
        UUID u = jugador.getUniqueId();
        DatosJugadores datos = plugin.getDatos();

        for (Mision m : plugin.getMisionManager().todas()) {
            if (m.tipo() != tipo || !m.objetivos().contains(objetivo)) continue;

            if (m.filtroNombre() != null) {
                if (nombreEntidad == null
                        || !nombreEntidad.toLowerCase(Locale.ROOT).contains(m.filtroNombre().toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }

            if (!datos.tieneAsignada(u, m.id()) || datos.estaReclamada(u, m.id())) continue;

            int actual = datos.getProgreso(u, m.id());
            if (actual >= m.cantidad()) continue;

            int nuevo = actual + 1;
            datos.setProgreso(u, m.id(), nuevo);

            if (nuevo >= m.cantidad()) {
                plugin.enviar(jugador, "misionCompletada", "%mision%", m.nombre(), "%jugador%", jugador.getName());
                jugador.playSound(jugador.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
    }
}
